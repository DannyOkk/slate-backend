# Roadmap - Slate Backend

## Sprint 1 - Setup & Autenticación + Clientes
- [x] `chore/initial-spring-boot-setup`: Estructura inicial de Spring Boot (Web, JPA, Postgres, Security).
- [x] `docs/roadmap-and-pr-template`: Agregar ROADMAP.md y plantilla de PR.
- [x] `chore/env-and-db-config`: Configuración de PostgreSQL y `.env.example`.
- [x] `feat/auth-endpoints`: Endpoints de Registro e Iniciar Sesión con JWT.
- [x] `feat/client-crud`: Endpoints CRUD de Clientes asociados al comerciante autenticado.

## Sprint 2 - Fiados y Pagos
- [x] `feat/transactions-crud`: Endpoints para registrar fiados y pagos por cliente.
- [x] `feat/client-balance`: Endpoint de saldo acumulado y lista "Quién me debe".

## Sprint 3 - Recordatorios y Scoring
- [x] `feat/risk-score-engine`: Lógica de puntaje de riesgo por cliente según reglas negocio.
- [x] `test/core-services-tests`: Pruebas unitarias básicas de cálculos de saldo y scoring.

## Colchón - Ajustes y Entrega
- [x] `docs/final-readme`: Documentación de API y setup de desarrollo.
- [ ] `fix/pre-demo-adjustments`: Correcciones finales detectadas en pruebas integradas.