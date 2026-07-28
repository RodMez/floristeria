# Cambios Realizados — Pagos Wompi

**Fecha:** 2026-07-27
**Referencia:** Hallazgos documentados en `04-pagos-wompi.md`

---

A continuación se documentan todos los cambios aplicados al código para corregir los hallazgos de seguridad identificados en la auditoría de la integración con Wompi Web Checkout. Todos los cambios compilan correctamente.

---

## H9 — Separar estado PAGADO de la deducción de stock

**Hallazgo original (ALTA):** En `procesarWebhookWompi()`, si `deducirInventario()` lanzaba `IllegalStateException` por stock insuficiente, TODA la transacción hacía rollback — incluyendo el cambio `PENDIENTE_PAGO → PAGADO`. El cliente ya había pagado via Wompi, pero el pedido quedaba como "pendiente de pago" para siempre, ya que el webhook retornaba 200 y Wompi no reintenta.

**Archivos afectados:** `PedidoServiceImpl.java`, `EmailService.java`, `EmailServiceImpl.java`

**Cambios:**

| Archivo | Cambio |
|---|---|
| `PedidoServiceImpl.java` | Nuevo método privado `marcarPedidoPagado()` con `@Transactional(propagation = REQUIRES_NEW)` que commitea el estado PAGADO de forma aislada |
| `PedidoServiceImpl.java` | `procesarWebhookWompi()` ahora llama `marcarPedidoPagado()` PRIMERO, luego intenta `deducirInventario()` en try-catch |
| `PedidoServiceImpl.java` | Si `deducirInventario()` falla: log error con código del pedido + email de alerta al admin (correoMaestro) |
| `PedidoServiceImpl.java` | Nuevo método `notificarAlertaStockInsuficiente()` que envía email HTML al admin con detalle del pedido y error |
| `PedidoServiceImpl.java` | Nuevo método `construirHtmlAlertaStock()` que genera el HTML de la alerta |
| `EmailService.java` | Nuevo método `enviarCorreoDirecto(String toEmail, String toName, String subject, String htmlContent)` |
| `EmailServiceImpl.java` | Implementación de `enviarCorreoDirecto()` que delega a `enviarCorreoBrevo()` |

**Mecanismo de aislamiento transaccional:**

```
procesarWebhookWompi() [@Transactional — para guards de idempotencia]
  ├─ Guards (PAGADO? PENDIENTE_PAGO?) ← se leen en la transacción outer
  ├─ marcarPedidoPagado() [@Transactional(REQUIRES_NEW)] ← commitea PAGADO aislado
  ├─ deducirInventario() en try-catch
  │   └─ catch: log error + email alerta admin
  └─ emailService.notificarNuevaVenta() ← solo si stock OK (o con stock parcial)
```

**Comportamiento anterior:** Stock insuficiente → rollback total → pedido queda PENDIENTE_PAGO con dinero cobrado → estado inconsistente sin notificación.

**Comportamiento actual:** Stock insuficiente → pedido queda PAGADO (commit aislado) → se notifica al admin por email con detalle → resolución manual del inventario.

---

## H3 — Validar `amount_in_cents` contra el total real del pedido

**Hallazgo original (MEDIA):** Una vez validada la firma SHA-256 del webhook, el `amount_in_cents` del payload se aceptaba sin cotejar contra el `total` guardado en la tabla Pedidos. Si un actor comprometiera `EVENTS_SECRET`, podría enviar un webhook con monto manipulado.

**Archivo afectado:** `PedidoServiceImpl.java`

**Cambio:**

```java
// Nueva validación después de encontrar el pedido por referencia
long montoEsperadoCentavos = pedido.getTotal().multiply(BigDecimal.valueOf(100)).longValue();
if (!String.valueOf(montoEsperadoCentavos).equals(amountInCents)) {
    log.error("Webhook Wompi - monto mismatch: esperado={} recibido={} pedido={}",
            montoEsperadoCentavos, amountInCents, pedido.getCodigo());
    return;
}
```

**Comportamiento anterior:** El monto del webhook se confiaba ciegamente una vez que la firma era válida.

**Comportamiento actual:** Se compara `amount_in_cents` del webhook contra `pedido.getTotal() * 100`. Si hay mismatch, se loguea el error con código del pedido y se retorna sin procesar.

---

## H1 — Comparación de checksum en tiempo constante

**Hallazgo original (BAJA):** La validación de firma del webhook usaba `String.equals()` que no es constante en tiempo, vulnerable teóricamente a timing attacks.

**Archivo afectado:** `PedidoServiceImpl.java`

**Cambio:**

```java
// Antes:
if (!firmaCalculada.equals(checksum)) {

// Ahora:
if (!MessageDigest.isEqual(firmaCalculada.getBytes(StandardCharsets.UTF_8), checksum.getBytes(StandardCharsets.UTF_8))) {
```

**Comportamiento anterior:** `String.equals()` compara byte por byte y retorna `false` en el primer byte distinto.

**Comportamiento actual:** `MessageDigest.isEqual()` compara todos los bytes independientemente del resultado, eliminando el vector de timing attack.

---

## H10 — Rate limiting en endpoint de webhook

**Hallazgo original (MEDIA):** El endpoint `POST /api/v1/webhooks/wompi` es público (`permitAll()`) y no tiene throttling. Un atacante podría inundar el endpoint con payloads inválidos.

**Archivos afectados:** **Nuevo** `WebhookRateLimitFilter.java`, `SecurityConfig.java`

**Cambios:**

| Archivo | Cambio |
|---|---|
| **Nuevo**: `WebhookRateLimitFilter.java` | Filtro que intercepta `POST /api/v1/webhooks/wompi` con límite de 100 requests/minuto por IP |
| `SecurityConfig.java` | Inyectado `WebhookRateLimitFilter` y registrado antes de `UsernamePasswordAuthenticationFilter` |

### Lógica del filtro

- **Sliding window de 60 segundos** por IP
- **Máximo 100 requests por minuto** por IP (suficiente para reintentas legítimos de Wompi)
- **Sin lockout** (a diferencia del login rate limit) — Wompi reintentos son legítimos
- **Extracción de IP** via `X-Forwarded-For` (primer salto) o `request.getRemoteAddr()`
- En exceso: HTTP 429 con JSON `{"status":429,"error":"Too Many Requests","mensaje":"..."}`

---

## H11 — Completar `.env.example`

**Hallazgo original (BAJA):** Faltaban `DB_URL` y `ADMIN_SEED_PASSWORD` en el template de variables de entorno.

**Archivo afectado:** `.env.example`

**Cambios:**

| Variable | Estado |
|---|---|
| `DB_URL` | Agregada con placeholder `jdbc:postgresql://localhost:5432/floristeria_db` |
| `ADMIN_SEED_PASSWORD` | Agregada con placeholder `***` |
| Wompi | Ya existía — agregado comentario de sandbox vs producción |
| Todas las 14 variables | Ahora documentadas en el archivo |

---

## Archivos no modificados (confirmado)

| Archivo | Motivo |
|---|---|
| `Pedido.java` | Sin cambios necesarios |
| `EstadoPedido.java` | Sin cambios necesarios |
| `WebhookController.java` | Sin cambios — el manejo de excepciones ya es correcto |
| `PedidoRepository.java` | Sin cambios — `findByReferenciaPago()` funciona correctamente |
| Lógica de idempotencia (guards PAGADO/PENDIENTE_PAGO) | Confirmado correcto en auditoría — no se toca |
| Bloqueo de PAGADO manual en `actualizarEstadoPedido()` | Confirmado correcto en auditoría — no se toca |
| Credenciales de Wompi | Se mantienen en sandbox — pendiente para Fase R7 (lanzamiento) |
