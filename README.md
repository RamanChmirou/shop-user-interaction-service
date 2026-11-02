# User Interaction Service

API Gateway and Backend-For-Frontend (BFF) microservice of the online shop. Serves as the single point of entry for frontend clients, orchestrates the complete sales process across the other microservices, and integrates with Keycloak for authentication and role-based authorization.

The frontend communicates solely with this microservice; direct calls to `product-service`, `cart-service`, and `order-service` are restricted.

## Tech stack

Java 21, Spring Boot 4.1, Spring Cloud OpenFeign, Resilience4j Circuit Breaker, Spring Security (OAuth2 Resource Server with JWT), Spring Data JPA, PostgreSQL (JSONB for checkout drafts), Liquibase, springdoc-openapi, Logstash Logback Encoder (ELK), JUnit 5, Mockito, Testcontainers, WireMock.

## 3-Screen Sales Process Flow

1. **Screen 1: Product Selection & Cart**
   - Browse catalog (`GET /api/products`, `GET /api/products?type=...`) and view product configuration (`GET /api/products/{id}/configuration`).
   - Calculate unit price quotes based on custom configurations (`POST /api/products/{id}/price-quote`).
   - Add configured products to the user's shopping cart (`POST /api/cart/items`), view cart (`GET /api/cart`), update quantities, remove items, or reprice against the latest catalog prices.
2. **Screen 2: Checkout Data / Draft**
   - Enter and edit customer details, shipping/billing addresses, delivery method (`COURIER`, `PARCEL_LOCKER`, `PICKUP`), and payment method (`CARD`, `BLIK`, `TRANSFER`).
   - Automatically persist checkout details as a draft in `user_interaction_db` (`GET/PUT /api/checkout/draft`).
3. **Screen 3: Order Confirmation & Submission**
   - Retrieve aggregated checkout summary (`GET /api/checkout/summary`) combining cart items, draft details, calculated delivery cost, and grand total.
   - Submit order (`POST /api/checkout/submit`): places order in `order-service`, re-verifies prices, and clears the checkout draft upon success.
   - Follow-up order actions: pay (`POST /api/orders/{id}/pay`), cancel (`POST /api/orders/{id}/cancel`), and view order history (`GET /api/orders`).

## API Endpoints

| Method | Path | Access | Description |
|---|---|---|---|
| GET | `/api/products` | Public | Paginated list of active products |
| GET | `/api/products?type=COMPUTER` | Public | Filter products by type (`COMPUTER`, `SMARTPHONE`, `ELECTRONICS`) |
| GET | `/api/products/{id}` | Public | Product details |
| GET | `/api/products/{id}/configuration` | Public | Product configuration option groups and values |
| POST | `/api/products/{id}/price-quote` | Public | Calculate price quote for configured options |
| POST | `/api/products` | `ADMIN` | Create product record |
| PUT | `/api/products/{id}` | `ADMIN` | Update product record |
| DELETE | `/api/products/{id}` | `ADMIN` | Delete product record |
| GET | `/api/cart` | `USER` | Get authenticated user's active shopping cart |
| POST | `/api/cart/items` | `USER` | Add item with configured options to cart |
| PATCH | `/api/cart/items/{itemId}` | `USER` | Update item quantity |
| DELETE | `/api/cart/items/{itemId}` | `USER` | Remove item from cart |
| DELETE | `/api/cart` | `USER` | Clear cart |
| POST | `/api/cart/reprice` | `USER` | Reprice cart items against catalog |
| GET | `/api/checkout/draft` | `USER` | Get saved checkout draft |
| PUT | `/api/checkout/draft` | `USER` | Save or update checkout draft |
| GET | `/api/checkout/summary` | `USER` | Aggregated order summary before final placement |
| POST | `/api/checkout/submit` | `USER` | Submit order from cart and draft |
| GET | `/api/orders` | `USER` | Paged order history of user |
| GET | `/api/orders/{id}` | `USER` | Order details |
| POST | `/api/orders/{id}/pay` | `USER` | Pay for order |
| POST | `/api/orders/{id}/cancel` | `USER` | Cancel unpaid order |
| POST | `/api/orders/{id}/invoice` | `USER` | Retry issuing invoice |
| POST | `/api/orders/{id}/ship` | `ADMIN` | Mark order as shipped |

Swagger UI: `http://localhost:8080/swagger-ui.html`, OpenAPI: `http://localhost:8080/v3/api-docs`.

## Security & Keycloak

- Integrated as an OAuth2 Resource Server validating JWT Bearer tokens from Keycloak realm `shop`.
- User ID is securely extracted from JWT subject claim (`sub`) via `CurrentUser`, preventing spoofing.
- Roles are converted from `realm_access.roles` (prefix `ROLE_`).

## Observability (Health & ELK)

- **Actuator Healthcheck:** `GET /actuator/health` is exposed publicly for Docker health checks.
- **ELK Logging:** Profile `docker` forwards structured JSON logs via Logstash TCP appender to `logstash:5000` with tag `service: user-interaction-service`.
