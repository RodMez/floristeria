# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is a Spring Boot-based e-commerce multi-tenant application for a flower store with separate tenant management for different store locations (Bogotá and Medellín). The project uses Java 21, Spring Boot 4.0.6, PostgreSQL, and JWT-based authentication.

### Business Logic

The application supports multiple store tenants with isolated inventory and pricing:
- **Sedes**: Physical store locations (Bogotá, Medellín) with independent whatsapp numbers for order routing
- **Productos**: Global catalog (name, description, image URL) - prices are location-specific
- **Inventario**: Pivot table linking products to sedes with local price, stock, and availability
- **Pedidos**: Orders are recorded in the system but the actual sale closes via WhatsApp
- **Usuarios_Admin**: RBAC with `sede_id` null for superadmin (global access) or assigned for local admin

## Build & Run Commands

### Development
- **Run tests**: `./mvnw test`
- **Compile**: `./mvnw clean compile`
- **Run application**: `./mvnw spring-boot:run`
- **Package**: `./mvnw clean package`
- **Check health**: `curl http://localhost:8080/actuator/health`

### Testing
- **Run all tests**: `./mvnw test`
- **Run specific test class**: `./mvnw test -Dtest=FloristeriaApplicationTests`
- **Run with compilation**: `./mvnw clean test`

### Windows (PowerShell)
- Use `mvnw.cmd` instead of `./mvnw`

## Architecture

### Technology Stack
- **Java**: 21
- **Spring Boot**: 4.0.6 (parent)
- **Database**: PostgreSQL
- **ORM**: Spring Data JPA / Hibernate
- **Security**: Spring Security + JWT (jjwt 0.12.3) + BCrypt
- **Build**: Maven
- **Utilities**: Lombok 1.18.30

### Package Structure
- `config/`: DataSeeder for initial data
- `controller/`: REST endpoints (Catalogo, Pedido, Auth, InventarioAdmin)
- `dto/`: Request/Response DTOs
- `entity/`: JPA entities with relationships
- `exception/`: Global exception handling (@ControllerAdvice)
- `repository/`: Spring Data JPA interfaces
- `security/`: JWT (JwtService, JwtAuthenticationFilter), UserDetails, SecurityConfig
- `service/`: Business logic interfaces and implementations

### Security Model

JWT-based authentication with role-based access control:
- **Public endpoints**: `/api/auth/**`, `/api/productos/**`, `/api/sedes/**`, `/api/categorias/**`, `/api/pedidos/**`
- **Protected endpoints**: All others require JWT token
- **Roles**: `superadmin` (sede_id=null, global access) or `admin` (restricted to assigned sede)
- The `JwtAuthenticationFilter` validates tokens and sets the security context

### Database Configuration

Uses environment variables injected via `application.yml`:
- `DB_URL`: PostgreSQL JDBC URL
- `DB_USER`: Database username
- `DB_PASS`: Database password
- `JWT_SECRET`: Secret key for JWT signing

Hibernate DDL is set to `update` for automatic schema updates during development.

## Environment Notes

- Application runs on port 8080
- SQL logging enabled with formatted output
- JWT tokens expire after 24 hours (86400000 ms)
- CORS is configured with defaults in SecurityConfig
- BCrypt is used for password hashing
