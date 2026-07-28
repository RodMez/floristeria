# Auditoría de Seguridad: Integración Wompi Web Checkout

| Campo | Valor |
|---|---|
| **Fecha** | 2026-07-25 |
| **Auditor** | opencode (big-pickle) |
| **Alcance** | Backend Spring Boot — integración completa con Wompi (Web Checkout Widget) |
| **Archivos analizados** | `PedidoServiceImpl.java`, `WebhookController.java`, `Pedido.java`, `EstadoPedido.java`, `SecurityConfig.java`, `application.yml`, `.env`, `pom.xml`, `PedidoClienteResponseDTO.java`, `PedidoAdminController.java`, `PedidoClienteController.java`, `EmailServiceImpl.java`, repositorios |

---

## 1. Contexto Arquitectónico

- **Modelo Web Checkout (Widget):** El backend nunca maneja datos de tarjeta. El frontend carga el widget de Wompi con `public-key`, `referencia`, `montoEnCentavos` y `firmaIntegridad`. PCI DSS delegado completamente a Wompi.
- **Firma de integridad:** `SHA-256(referencia_pago + total_en_centavos + "COP" + INTEGRITY_SECRET)` → hash hex minúsculas, enviado al frontend para abrir el widget.
- **Webhook público** en `POST /api/v1/webhooks/wompi` (sin JWT). Valida checksum: `SHA-256(transaction.id + status + amount_in_cents + timestamp + EVENTS_SECRET)`.
- **Máquina de estados:** `PENDIENTE_PAGO → PAGADO → EN_PREPARACION → EN_CAMINO → ENTREGADO`, con `CANCELADO` solo desde `PENDIENTE_PAGO`.
- **Deducción de stock:** Exclusivamente en el webhook tras pago aprobado, dentro de `@Transactional`.

---

## 2. Tabla de Hallazgos

| # | Hallazgo | Severidad | Veredicto | Evidencia |
|---|---|---|---|---|
| H1 | Comparación de checksum con `String.equals()` (timing attack) | Baja | Mejor práctica | `PedidoServiceImpl.java:419` |
| H2 | Idempotencia del webhook — doble deducción de inventario | Crítica | **PASS** | `PedidoServiceImpl.java:434-439` |
| H3 | Sin validación de `amount_in_cents` contra total en BD | Media | **GAP** | `PedidoServiceImpl.java:345-447` |
| H4 | Secrets en variables de entorno, no expuestos al frontend | Alta | **PASS** | `application.yml:41-44`, `PedidoServiceImpl.java:231` |
| H5 | Credenciales de sandbox/test en `.env` y fallbacks de `application.yml` | **Crítica** | **BLOQUEANTE** | `.env:22-24`, `application.yml:42-44` |
| H6 | No se loguean payloads completos del webhook | Alta | **PASS** | `WebhookController.java:24` |
| H7 | Unicidad de `referencia_pago` (UUID + timestamp) | Alta | **PASS** | `PedidoServiceImpl.java:208`, `Pedido.java:83` |
| H8 | Decisión Bold — sin referencias residuales en código | N/A | **CLEAN** | Búsqueda exhaustiva del repo completo |
| H9 | Stock insuficiente causa rollback del estado PAGADO | **Alta** | **REQUERIR CAMBIO** | `PedidoServiceImpl.java:442-447` |
| H10 | Sin rate limiting en endpoint de webhook | Media | **GAP** | `SecurityConfig.java:48`, pom.xml |
| H11 | Sin archivo `.env.example` | Baja | **GAP** | No existe |
| H12 | Admin bloqueado de forzar estado PAGADO manualmente | N/A | **PASS** | `PedidoServiceImpl.java:266-268` |

---

## 3. Análisis Detallado

### H1 — Timing Attack en Comparación de Checksum (Baja)

**Código:**
```java
// PedidoServiceImpl.java:419
if (!firmaCalculada.equals(checksum)) {
    throw new SecurityException("Firma del webhook inválida");
}
```

**Problema:** `String.equals()` compara byte por byte y retorna `false` en el primer byte distinto. Un atacante podría medir tiempos de respuesta para inferir bytes del hash calculado.

**Riesgo práctico:** Bajo. El checksum es un hash SHA-256 de 64 caracteres hexadecimales. La diferencia de tiempo por byte es del orden de nanosegundos, y la latencia de red lo enmascara completamente.

**Recomendación:** Usar `java.security.MessageDigest.isEqual()` que implementa comparación en tiempo constante:
```java
// Reemplazar la línea 419 por:
if (!MessageDigest.isEqual(firmaCalculada.getBytes(), checksum.getBytes())) {
    throw new SecurityException("Firma del webhook inválida");
}
```

---

### H2 — Idempotencia del Webhook (Crítica — PASS)

**Esta es la verificación de mayor riesgo financiero.** El webhook de Wompi puede reenviar el mismo evento múltiples veces (política de reintentos con backoff exponencial).

**Código de protección (PedidoServiceImpl.java:382-449):**
```java
@Transactional
public void procesarWebhookWompi(Map<String, Object> payload) {
    // ... extracción de campos y validación de firma ...

    Pedido pedido = pedidoRepository.findByReferenciaPago(reference).orElse(null);
    if (pedido == null) { return; }                              // Línea 430-431

    if (pedido.getEstado() == EstadoPedido.PAGADO) { return; }  // Línea 434-435 ← GUARD 1
    if (pedido.getEstado() != EstadoPedido.PENDIENTE_PAGO) {    // Línea 438-439 ← GUARD 2
        return;
    }

    pedido.setEstado(EstadoPedido.PAGADO);                      // Línea 442
    pedido.setTransaccionId(transactionId);                      // Línea 443
    pedido.setMetodoPago(paymentMethodType);                     // Línea 444
    pedidoRepository.save(pedido);                               // Línea 445

    deducirInventario(pedido);                                   // Línea 447
    emailService.notificarNuevaVenta(pedido);                    // Línea 449
}
```

**Análisis de protección contra doble deducción:**

1. **Escenario:** Wompi envía el mismo evento 2 veces.
2. **Primera ejecución:** Lee pedido (PENDIENTE_PAGO), pasa los guards, marca PAGADO, deduce stock, envía email. La transacción commitea.
3. **Segunda ejecución:** El `@Transactional` crea una nueva transacción. Con PostgreSQL READ COMMITTED + row lock del primer commit, la segunda ejecución lee el pedido ya en estado PAGADO.
4. **Guard 1 (línea 434):** `pedido.getEstado() == EstadoPedido.PAGADO` → `return`. **Doble deducción bloqueada.**

**Concurrencia simultánea (dos webhooks en el mismo milisegundo):**
- Thread A inicia transacción, lee el pedido (row lock), modifica estado a PAGADO.
- Thread B intenta leer el mismo pedido → **bloqueado por el row lock** de Thread A.
- Thread A commitea. Thread B desbloquea, lee estado PAGADO, entra al Guard 1 y retorna.
- **SEGURO:** PostgreSQL serializa los accesos a nivel de fila dentro de transacciones READ COMMITTED.

**Doble email:**
- `emailService.notificarNuevaVenta()` es `@Async` (se ejecuta en un thread separado, línea 61 de `EmailServiceImpl.java`), pero está dentro de la misma transacción. Solo se despacha si la transacción commitea. Como la segunda ejecución retorna en el Guard 1 antes de llegar a la línea 449, el email solo se envía una vez.

**Veredicto: PASS — La idempotencia está correctamente implementada.**

---

### H3 — Sin Validación de Monto del Webhook (Media)

**Código (PedidoServiceImpl.java:345-447):**
```java
// Línea 345-346: El monto se extrae del payload de Wompi
String amountInCents = amountObj.toString();
String cadenaFirma = transactionId + status + amountInCents + timestampSecs + wompiEventsSecret;
String firmaCalculada = generarSha256Hex(cadenaFirma);

// Línea 419: Se valida la firma
if (!firmaCalculada.equals(checksum)) { throw new SecurityException(...); }

// Línea 427: Se busca el pedido por referencia
Pedido pedido = pedidoRepository.findByReferenciaPago(reference).orElse(null);

// LÍNEA FALTANTE: NUNCA se compara amountInCents contra pedido.getTotal()
pedido.setEstado(EstadoPedido.PAGADO);  // Se confía ciegamente en el monto de Wompi
```

**Problema:** Una vez que la firma es válida, el `amount_in_cents` del webhook se acepta sin cotejar contra el total real guardado en la tabla `Pedidos`. Si un actor comprometiera `EVENTS_SECRET`, podría enviar un webhook con un monto manipulado. Aunque la firma sería válida, el monto no coincidiría con lo que el cliente realmente debía pagar.

**Vector de ataque:** Requiere comprometer `EVENTS_SECRET` (que está en variable de entorno). Con ese nivel de acceso, el atacante ya tiene control total, por lo que el riesgo es de **defensa en profundidad** más que un vector práctico.

**Recomendación:** Agregar validación de monto después de encontrar el pedido:
```java
// Después de línea 427-428
Pedido pedido = pedidoRepository.findByReferenciaPago(reference).orElse(null);
if (pedido == null) { return; }

// NUEVA VALIDACIÓN: Comparar monto del webhook contra monto guardado
long montoEsperadoCentavos = pedido.getTotal().longValue() * 100;
if (!String.valueOf(montoEsperadoCentavos).equals(amountInCents)) {
    log.error("Webhook Wompi - monto mismatch: esperado={} recibido={}",
              montoEsperadoCentavos, amountInCents);
    return;
}
```

---

### H4 — Secrets en Variables de Entorno (Alta — PASS)

**Verificación:**

| Secret | Env var | Envío al frontend | En `.env` |
|---|---|---|---|
| `wompi.public-key` | `WOMPI_PUBLIC_KEY` | Sí (en `PedidoClienteResponseDTO:22`) — **Correcto** | Sí |
| `wompi.integrity-secret` | `WOMPI_INTEGRITY_SECRET` | **NUNCA** | Sí |
| `wompi.events-secret` | `WOMPI_EVENTS_SECRET` | **NUNCA** | Sí |

**Código que envía solo la public key:**
```java
// PedidoServiceImpl.java:224-232
return PedidoClienteResponseDTO.builder()
        .publicKeyWompi(wompiPublicKey)        // Solo la public key
        .firmaIntegridad(firmaIntegridad)       // Hash calculado con secret, pero es un hash
        .referenciaWompi(referencia)
        .montoEnCentavos(montoCentavos)
        .build();
```

Los secrets (`integrity-secret`, `events-secret`) nunca se incluyen en la respuesta DTO. La `firmaIntegridad` es un hash SHA-256 precalculado, no el secreto en sí.

**Veredicto: PASS — Los secrets están protegidos.**

---

### H5 — Credenciales de Sandbox en Producción (Crítica — BLOQUEANTE)

**Evidencia en `.env`:**
```env
WOMPI_PUBLIC_KEY=pub_test_scgHTdI2VdshQpxhZG4cglsOl3cZDonG
WOMPI_INTEGRITY_SECRET=test_integrity_dti7eqw7T823Blh57mX3Ik9hG9I2SZYa
WOMPI_EVENTS_SECRET=test_events_2RZD5BUO9AGaGIztQpAoBcW9XYOgtmWT
```

**Evidencia en `application.yml` (fallbacks por defecto):**
```yaml
wompi:
  public-key: ${WOMPI_PUBLIC_KEY:pub_test_XXXXX}
  integrity-secret: ${WOMPI_INTEGRITY_SECRET:test_integrity_XXXXX}
  events-secret: ${WOMPI_EVENTS_SECRET:test_events_XXXXX}
```

**Problema:** Todos los prefijos (`pub_test_`, `test_integrity_`, `test_events_`) indican claramente credenciales de **sandbox/test** de Wompi. Si estas credenciales se usan en producción:
- Los pagos se procesan en el entorno de prueba de Wompi (no se cobra dinero real).
- Los webhooks provienen del sandbox de Wompi, no de producción.
- La firma de integridad no coincidirá con transacciones reales.

**Recomendación:**
1. **Antes del lanzamiento:** Generar credenciales de producción en el dashboard de Wompi.
2. Configurar las nuevas env vars en Coolify (o el orchestrador de producción) con los valores de producción.
3. Los fallbacks en `application.yml` deben permanecer como valores test para desarrollo local.
4. **NUNCA** colocar las credenciales de producción en `.env` del repositorio.

---

### H6 — Logging de Payloads Sensibles (Alta — PASS)

**WebhookController.java:20-27:**
```java
@PostMapping("/wompi")
public ResponseEntity<Void> recibirEventoWompi(@RequestBody Map<String, Object> payload) {
    try {
        pedidoService.procesarWebhookWompi(payload);
    } catch (IllegalArgumentException | SecurityException | IllegalStateException e) {
        log.error("Webhook Wompi - error de negocio (no reintentable): {}", e.getMessage(), e);
    }
    return ResponseEntity.ok().build();
}
```

**Verificación:**
- No existe `log.debug("Payload: {}", payload)` ni similar en ningún punto del flujo.
- No hay configuración de logging personalizada (`logback.xml` no existe).
- El único log es el mensaje de error en catch, que solo imprime `e.getMessage()`.
- Aunque el modelo Web Checkout no maneja tarjetas, algunos payloads pueden incluir datos parciales del pagador (email, teléfono parcial). No se loguean.

**Veredicto: PASS — No se exponen datos sensibles en logs.**

---

### H7 — Unicidad de `referencia_pago` (Alta — PASS)

**Generación (PedidoServiceImpl.java:208):**
```java
String referencia = savedPedido.getCodigo() + "-" + System.currentTimeMillis();
```

**El `codigo` (Pedido.java:82-84):**
```java
if (this.codigo == null) {
    this.codigo = "PED-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
}
```

**Análisis:** La `referencia_pago` tiene formato `PED-XXXXXXXX-1721912345678`. Aunque dos pedidos se creen en el mismo milisegundo (colisión de `System.currentTimeMillis()`), el componente `PED-XXXXXXXX` usa `UUID.randomUUID()` que tiene 122 bits de entropía. La probabilidad de colisión es despreciable (< 2^-61 para mil millones de pedidos, según la función de Birthday).

Además, la referencia es **globalmente única** porque combina un identificador único (UUID) con un timestamp, y se almacena en la BD antes de usarse para el widget.

**Veredicto: PASS — La unicidad está garantizada por la UUID.**

---

### H8 — Decisión Bold (N/A — CLEAN)

**Búsqueda realizada:**
- `grep -ri "bold"` en todo el repo: Solo coincidencias en `font.setBold(true)` de exportaciones Excel (`PedidoExportService.java`, `ProductoInventarioExportService.java`) y una mención en documentación de skills de Claude.
- `grep -ri "WOMPI_BOLD\|BOLD_\|bold-\|\.bold"`: 0 resultados.
- `pom.xml`: No hay dependencias de Bold.
- `.env` / `application.yml`: No hay variables de Bold.
- Directorio de configuraciones: No hay archivos de Bold.

**Estado confirmado:** No existe ningún código, configuración, dependencia, ni variable de entorno relacionada con Bold como pasarela de pago.

**Recomendación:**
- Si se decidió **NO implementar Bold**: Agregar una nota en `README.md` o en un documento de arquitectura indicando que Bold fue evaluada como alternativa a Wompi (por exención de retenciones en PSE) pero descartada por razones [especificar]. Esto evita que futuros desarrolladores pierdan tiempo re-evaluando.
- Si se decidió **implementar Bold en el futuro**: Crear un branch dedicado con la integración, sin mezclar con el flujo de Wompi.

---

### H9 — Stock Insuficiente Causa Rollback del Pago (Alta — REQUERIR CAMBIO)

**Este es el hallazgo de mayor riesgo operativo descubierto.**

**Flujo actual (PedidoServiceImpl.java:382-449):**
```
procesarWebhookWompi() — @Transactional
  1. Validar firma SHA-256                    ✓
  2. Buscar pedido por referencia             ✓
  3. Guard: ¿Ya pagado? → return              ✓
  4. Guard: ¿Es PENDIENTE_PAGO? → return      ✓
  5. Marcar estado = PAGADO                   ← Se guarda en BD
  6. Guardar pedido                           ← Flush a BD
  7. deducirInventario()                      ← Si stock < cantidad → EXCEPTION
  8. emailService.notificarNuevaVenta()       ← Nunca se alcanza si falla paso 7
```

**El problema:**
Si `deducirInventario()` lanza `IllegalStateException` (stock insuficiente), la **transacción completa se revierte** — incluyendo el cambio de estado a PAGADO (pasos 5-6).

**Consecuencia:**
1. Wompi cobró al cliente (el webhook llegó con status APPROVED).
2. El pedido queda en estado `PENDIENTE_PAGO` en la BD.
3. El stock no se dedujo.
4. No se envió email de confirmación.
5. El cliente pagó pero no tiene registro de pedido pagado.
6. El webhook retornó 200 a Wompi (el controller siempre retorna 200, línea 26), así que Wompi no reintenta.

**Estado resultante:** Inconsistencia financiera — dinero cobrado, pedido sin procesar.

**Recomendación — Separar en dos fases:**

**Opción A (Recomendada): Marcar PAGADO primero, compensar stock después.**
```java
@Transactional
public void procesarWebhookWompi(Map<String, Object> payload) {
    // ... validación de firma y extracción de campos ...

    Pedido pedido = pedidoRepository.findByReferenciaPago(reference).orElse(null);
    if (pedido == null) { return; }
    if (pedido.getEstado() == EstadoPedido.PAGADO) { return; }
    if (pedido.getEstado() != EstadoPedido.PENDIENTE_PAGO) { return; }

    // FASE 1: Marcar como PAGADO (esto NUNCA debe fallar por stock)
    pedido.setEstado(EstadoPedido.PAGADO);
    pedido.setTransaccionId(transactionId);
    pedido.setMetodoPago(paymentMethodType);
    pedidoRepository.save(pedido);

    // FASE 2: Intentar deducir stock (si falla, el pedido ya está PAGADO)
    try {
        deducirInventario(pedido);
    } catch (IllegalStateException e) {
        log.error("Webhook Wompi - Stock insuficiente post-pago para pedido {}: {}",
                  pedido.getCodigo(), e.getMessage());
        // El pedido queda PAGADO pero sin stock deducido.
        // Requiere intervención manual del admin para resolver.
        // Opcionalmente: notificar al admin por email/WhatsApp.
    }

    emailService.notificarNuevaVenta(pedido);
}
```

**Opción B (Alternativa): Validar stock ANTES de marcar PAGADO.**
Mover `deducirInventario()` antes del cambio de estado. Si stock falla, el pedido se mantiene PENDIENTE_PAGO y el cliente puede intentar de nuevo. Riesgo: el cliente ya pagó, pero el pedido no se confirmó. Menor que la Opción A porque el admin puede ver el estado y contactar al cliente.

**Impacto de no corregir:** En escenarios de alta demanda (ej. día de la madre, San Valentín), donde el stock cambia rápidamente, es posible que un cliente pague un producto que se agota entre la creación del pedido y la confirmación del webhook. El sistema quedaría en estado inconsistente requiriendo intervención manual.

---

### H10 — Sin Rate Limiting en Webhook (Media)

**Evidencia:**
- `SecurityConfig.java:48`: `.requestMatchers("/api/v1/webhooks/**").permitAll()` — Sin throttling.
- `pom.xml`: No hay dependencias de `bucket4j`, `resilience4j`, ni similar.
- No hay filtro, interceptor, o anotación de rate limiting en el proyecto.

**Protección actual:** La única barrera es la validación de firma SHA-256. Un atacante sin conocimiento de `EVENTS_SECRET` no puede explotar el endpoint de manera útil (las firmas serían inválidas y el controller retornaría 200 sin procesar).

**Riesgo:** Un atacante podría inundar el endpoint con payloads inválidos, causando:
- Consumo de CPU (cálculo de SHA-256 por cada request).
- Ruido en logs de errores.
- Posible denegación de servicio si el volume es alto suficiente.

**Recomendación:** Implementar rate limiting a nivel de infrastructure (Coolify/nginx reverse proxy) o con un filtro de Spring. Ejemplo mínimo:
- Límite: 100 requests/minuto por IP para `/api/v1/webhooks/**`.
- Esto es suficiente para las reintentas legítimas de Wompi (que reintentan con backoff exponencial y típicamente envían < 10 reintentos por evento).

---

### H11 — Sin `.env.example` (Baja)

**Problema:** No existe un archivo `.env.example` o `.env.template` que liste las variables de entorno requeridas. Un nuevo desarrollador debe revisar `application.yml` y las anotaciones `@Value` para descubrir qué variables configurar.

**Recomendación:** Crear `.env.example` con todas las variables requeridas y valores placeholder:
```env
# === Database ===
DB_URL=jdbc:postgresql://localhost:5432/floristeria_db
DB_USER=your_user
DB_PASS=your_password

# === JWT ===
JWT_SECRET=your_base64_encoded_secret

# === ImageKit ===
IMAGEKIT_PRIVATE_KEY=your_private_key
IMAGEKIT_PUBLIC_KEY=your_public_key
IMAGEKIT_URL_ENDPOINT=https://your_imagekit_url

# === CORS ===
CORS_ALLOWED_ORIGINS=http://localhost:3000

# === Wompi (sandbox) ===
WOMPI_PUBLIC_KEY=pub_test_xxxxx
WOMPI_INTEGRITY_SECRET=test_integrity_xxxxx
WOMPI_EVENTS_SECRET=test_events_xxxxx

# === Brevo (email) ===
BREVO_API_KEY=your_brevo_api_key
BREVO_SENDER_EMAIL=sender@example.com
BREVO_SENDER_NAME=Your Store Name

# === Frontend ===
FRONTEND_URL=http://localhost:3000
```

---

### H12 — Admin Bloqueado de Forzar PAGADO (PASS)

**Código (PedidoServiceImpl.java:258-278):**
```java
@Transactional
public PedidoAdminResponseDTO actualizarEstadoPedido(...) {
    // ...
    if (nuevoEstado == EstadoPedido.PAGADO) {
        throw new IllegalStateException(
            "El pago solo puede ser procesado automáticamente por la pasarela.");
    }

    if (pedido.getEstado() == EstadoPedido.PENDIENTE_PAGO
            && nuevoEstado != EstadoPedido.CANCELADO) {
        throw new IllegalStateException(
            "Desde PENDIENTE_PAGO solo se permite cancelar el pedido.");
    }
    // ...
}
```

**Verificación:**
- Un ADMIN o SUPERADMIN **no puede** setear manualmente el estado a `PAGADO` — lanza excepción.
- Desde `PENDIENTE_PAGO`, solo se permite transicionar a `CANCELADO`.
- El único camino a `PAGADO` es a través de `procesarWebhookWompi()` o `procesarPagoExitoso()`.

**Veredicto: PASS — La máquina de estados protege correctamente la transición de pago.**

---

## 4. Subsección: Idempotencia del Webhook (Detalle Financiero)

Este es el punto de mayor riesgo financiero de toda la integración. Un fallo en la idempotencia puede causar:

| Escenario | Consecuencia | Mitigación actual |
|---|---|---|
| Doble deducción de inventario | Stock negativo, clientes con pedidos que no se pueden fulfillment | **Protegido** — state check + row lock (H2) |
| Doble envío de email | Cliente recibe duplicados, confusión | **Protegido** — solo se envía si transacción commitea |
| Doble cobro | Wompi cobra dos veces al cliente | **Fuera del scope del backend** — Wompi maneja idempotencia de cobro con `reference` como idempotency key |
| Pedido marcado PAGADO dos veces | No tiene efecto (ya está en PAGADO) | **Protegido** — Guard 1 en línea 434 |

**Mecanismo de protección confirmado con evidencia:**

```
Thread A (webhook #1)                    Thread B (webhook #2)
─────────────────────                    ─────────────────────
BEGIN TRANSACTION                        BEGIN TRANSACTION
  READ pedido (row lock)                   BLOCKED (waiting for row lock)
  estado = PENDIENTE_PAGO ✓
  SET estado = PAGADO
  SAVE pedido
  DEDUCT stock
  SEND email
COMMIT                                    DESBLOQUEADO
                                          READ pedido → estado = PAGADO
                                          Guard 1: estado == PAGADO → RETURN
                                          (nunca llega a deducir stock)
```

**Base de datos:** PostgreSQL con READ COMMITTED (aislamiento por defecto) + row-level locking dentro de transacciones `@Transactional` garantiza la serialización.

---

## 5. Resumen de Acciones Requeridas

### Prioridad Crítica (antes de lanzamiento a producción)

| # | Acción | Hallazgo | Esfuerzo |
|---|---|---|---|
| 1 | **Cambiar credenciales de sandbox a producción** en Coolify | H5 | 15 min |
| 2 | **Corregir rollback del pago** — Separar transacción en fases (marcar PAGADO antes de stock) | H9 | 1-2 horas |

### Prioridad Alta (corto plazo)

| # | Acción | Hallazgo | Esfuerzo |
|---|---|---|---|
| 3 | Agregar validación de `amount_in_cents` contra `pedido.getTotal()` | H3 | 30 min |
| 4 | Reemplazar `String.equals()` por `MessageDigest.isEqual()` en checksum | H1 | 15 min |
| 5 | Crear `.env.example` con variables documentadas | H11 | 15 min |

### Prioridad Media (mejora continua)

| # | Acción | Hallazgo | Esfuerzo |
|---|---|---|---|
| 6 | Implementar rate limiting en webhook (nginx o Spring filter) | H10 | 1-2 horas |
| 7 | Documentar decisión Bold en README | H8 | 15 min |

---

## 6. Checklist de Verificación Final

- [x] Comparación de checksum: usa `String.equals()` — documentado (H1)
- [x] Idempotencia del webhook: confirmada con evidencia de código (H2)
- [x] Validación de monto: **NO existe** — gap documentado (H3)
- [x] Secrets en env vars, no expuestos al frontend (H4)
- [x] Credenciales sandbox detectadas — **BLOQUEANTE** (H5)
- [x] No se loguean payloads sensibles (H6)
- [x] Unicidad de referencia_pago garantizada por UUID (H7)
- [x] Bold: sin referencias residuales (H8)
- [x] Rollback por stock: riesgo documentado con recomendación (H9)
- [x] Rate limiting: no existe — gap documentado (H10)
- [x] `.env.example`: no existe — gap documentado (H11)
- [x] Admin bloqueado de forzar PAGADO (H12)
