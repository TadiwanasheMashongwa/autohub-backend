# AutoHub API 🚗 🇿🇼
High-performance backend for Zimbabwean car spare parts e-commerce.

## Tech Stack
- **Framework:** Spring Boot 3.x
- **Database:** PostgreSQL (Railway)
- **Security:** Spring Security + JWT
- **Documentation:** Swagger UI (OpenAPI 3)

## Local Setup
1. Clone the repo.
2. Set environment variables (see `.env.example`).
3. Run `./mvnw spring-boot:run`.

## MVP Features Tracking
- [ ] Phase 1: Foundation (Current)
- [ ] Phase 2: Database & Domain Modeling

AutoHub API Documentation (v2026.1.6)
This documentation covers the full integration of the 11 controllers, ensuring a production-ready flow for the Zimbabwe automotive market.

🛠️ Infrastructure & Security
1. Health & Monitoring
   GET /api/v1/health: Checks application and database connectivity.

2. Authentication
   POST /api/v1/auth/register: Create a new user account.

POST /api/v1/auth/login: Authenticate and check if MFA is required.

POST /api/v1/auth/verify-mfa: Verify code and receive JWT.

POST /api/v1/auth/logout: Blacklist the current Bearer token.

3. Password Recovery
   POST /api/v1/auth/password-reset/request: Trigger reset email.

POST /api/v1/auth/password-reset/confirm: Update password using token.

📦 Catalog Management
4. Part Catalog
   GET /api/v1/parts: Paginated list of all parts.

GET /api/v1/parts/{id}: View specific part details.

GET /api/v1/parts/search: Filter by name, SKU, or keyword.

GET /api/v1/parts/vehicle/{vehicleId}: Filter parts by compatibility.

POST /api/v1/parts: (Admin) Create new part with compatibility mapping.

5. Categories
   GET /api/v1/categories: List all sections (e.g., Engines, Suspension).

GET /api/v1/categories/{id}: View specific category.

POST /api/v1/categories: (Admin) Create new category.

PUT /api/v1/categories/{id}: (Admin) Update category details.

DELETE /api/v1/categories/{id}: (Admin) Remove category.

6. Vehicle Fleet (The Discovery Engine)
   GET /api/v1/vehicles: List all supported vehicles.

GET /api/v1/vehicles/{id}: View specific vehicle (engine code, etc).

GET /api/v1/vehicles/makes: Get unique list of brands.

GET /api/v1/vehicles/models: Get models for a brand.

GET /api/v1/vehicles/years: Get specific year ranges for a model.

POST /api/v1/vehicles: (Admin) Add new vehicle to the blank system.

PUT /api/v1/vehicles/{id}: (Admin) Correct vehicle data.

DELETE /api/v1/vehicles/{id}: (Admin) Remove vehicle.

🛒 Shopping & Payments
7. Cart
   GET /api/v1/cart: View current items and subtotal.

POST /api/v1/cart/add: Add part to cart (updates quantity if exists).

DELETE /api/v1/cart/item/{id}: Remove specific item.

DELETE /api/v1/cart/clear: Empty the cart.

POST /api/v1/cart/coupon: Apply a discount code to the session.

8. Orders
   POST /api/v1/orders/checkout: Convert cart to order (Idempotent).

GET /api/v1/orders/my-orders: List authenticated user's history.

GET /api/v1/orders/{id}: Securely view specific order details.

GET /api/v1/orders/all: (Admin) View every order in the system.

PATCH /api/v1/orders/{id}/status: (Admin) Manually override status.

9. Payments
   POST /api/v1/payments/initiate/{orderId}: Lock price and start intent.

POST /api/v1/payments/confirm: Verify payment success and reduce stock.

📈 Management & Social
10. Unified Admin Management
    GET /api/v1/admin/stats: Revenue, Order count, Customer count.

GET /api/v1/admin/low-stock: List parts below threshold.

GET /api/v1/admin/customers: List all registered customers.

PATCH /api/v1/admin/inventory/{id}/stock: Manual stock adjustment.

POST /api/v1/admin/orders/{id}/ship: Add tracking and set to SHIPPED.

POST /api/v1/admin/orders/{id}/refund: Process full/partial refund.

11. Reviews
    POST /api/v1/reviews/add: Submit review (Requires verified purchase).

GET /api/v1/reviews/part/{partId}: View community feedback for a part.