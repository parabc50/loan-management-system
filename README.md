# Core Banking Loan Management System (LMS)

An enterprise-grade, RESTful Core Banking Solution (CBS) for Loan Origination, Amortization Schedule Generation, EMI Servicing, and Loan Foreclosure. Built with **Java 17**, **Spring Boot 4**, **Spring Security 6 (JWT + RBAC)**, and **Spring Data JPA**.

---

## Overview

In banking and credit operations, managing loan origination, dynamic interest amortization, and partial/foreclosure repayments requires high arithmetic precision and rigorous role-based access.

This system provides a full-stack loan management terminal modeled after financial technology workflows (e.g., IDBI Intech, Finacle, Temenos). It supports multi-facility assignment per customer, exact reducing-balance EMI calculations via Java's `BigDecimal`, dynamic installment tracking, loan pre-closure truncation, and a clean single-page administrative dashboard.

---

## Key Features

* **Role-Based Access Control (RBAC):**
* `ROLE_OFFICER`: Full portfolio visibility, credit facility origination, disbursal execution, manual EMI receipt entry, advance principal prepayment, and loan cancellation/deletion.
* `ROLE_CUSTOMER`: Secure customer view restricted strictly to their own CIF records, personal profile metadata, designated loan officer details, and real-time installment payment statuses.


* **Stateless JWT Authentication:**
* Standard JSON Web Token (HMAC-SHA256) pipeline securing all REST endpoints.
* Granular method security enabled via Spring Security `@PreAuthorize`.


* **Financial Amortization Engine:**
* Implements the standard actuarial reducing-balance annuity formula:

$$EMI = \frac{P \times r \times (1 + r)^n}{(1 + r)^n - 1}$$


* Full precision handling using `BigDecimal` and `RoundingMode.HALF_UP` to prevent floating-point drift in monetary ledgers.


* **Installment Servicing & Early Foreclosure:**
* Real-time tracking of individual installments (`PAID` vs `UNPAID`) with transaction reference codes.
* Advance principal prepayments automatically reduce current principal, waive future unaccrued interest, truncate redundant future schedules, and update status to `CLOSED`.


* **Single Page Application (SPA) Terminal:**
* Embedded responsive banking dashboard served directly from Spring Boot resources without requiring external Node.js tooling.



---

## Architecture & Tech Stack

* **Backend:** Java 17, Spring Boot 4.x
* **Security:** Spring Security 6, JJWT (io.jsonwebtoken 0.11.5), BCrypt Password Hashing
* **Persistence:** Spring Data JPA, Hibernate ORM
* **Database:** In-memory H2 Database (with zero-config switch capability for Oracle Database XE via `ojdbc17`)
* **Build Tool:** Apache Maven
* **Frontend:** Vanilla JavaScript (ES6+ Fetch API), Tailwind CSS, FontAwesome

---

## Database Model

```
       +-------------------------+
       |          USERS          |
       +-------------------------+
       | PK  id                  |
       |     username            |
       |     password (BCrypt)   |
       |     role                |
       |     customer_id (CIF)   |
       |     full_name           |
       |     email / phone       |
       |     assigned_officer    |
       +------------+------------+
                    | 1
                    |
                    | (maps CIF)
                    |
                    | *
       +------------v------------+              1 : N
       |    LOAN_APPLICATIONS    +----------------------------------+
       +-------------------------+                                  |
       | PK  loan_id             |                                  |
       |     customer_id         |                                  |
       |     principal_amount    |                                  |
       |     annual_interest_rate|                                  |
       |     tenure_months       |                                  |
       |     loan_type           |                                  v
       |     status              |                      +-----------+------------+
       |     created_at          |                      |  REPAYMENT_SCHEDULES   |
       +-------------------------+                      +------------------------+
                                                        | PK  schedule_id        |
                                                        | FK  loan_id            |
                                                        |     installment_no     |
                                                        |     due_date           |
                                                        |     emi_amount         |
                                                        |     principal_comp     |
                                                        |     interest_comp      |
                                                        |     outstanding_balance|
                                                        |     status             |
                                                        |     paid_at            |
                                                        |     payment_reference  |
                                                        +------------------------+

```

---

## API Endpoints

### 1. Authentication

| Method | Endpoint | Access | Description |
| --- | --- | --- | --- |
| `POST` | `/api/v1/auth/login` | Public | Authenticates credentials and returns JWT token & user metadata |
| `GET` | `/api/v1/auth/me` | Authenticated | Fetches profile of the active session token |
| `GET` | `/api/v1/auth/customers` | Officer Only | Lists all registered retail customers (for facility assignment) |

### 2. Loan Management & Servicing

| Method | Endpoint | Access | Description |
| --- | --- | --- | --- |
| `POST` | `/api/v1/loans/apply` | Customer / Officer | Submits or assigns a new loan application under a CIF |
| `GET` | `/api/v1/loans` | Customer / Officer | Returns all loans for Officers; returns only CIF-owned loans for Customers |
| `POST` | `/api/v1/loans/{id}/disburse` | Officer Only | Disburses loan, locks facility, and computes amortization table |
| `GET` | `/api/v1/loans/{id}/schedule` | Customer / Officer | Retrieves full amortization and repayment status schedule |
| `POST` | `/api/v1/loans/schedule/{scheduleId}/pay` | Officer Only | Records an EMI installment receipt and sets status to `PAID` |
| `POST` | `/api/v1/loans/{id}/advance-pay` | Officer Only | Applies lump-sum principal prepayment and settles/truncates schedule |
| `DELETE` | `/api/v1/loans/{id}` | Officer Only | Deletes or archives a pending loan application |

---

## Getting Started

### Prerequisites

* **JDK 17** or higher installed (e.g. Eclipse Temurin 17)
* **Maven 3.8+** (or use the included Maven Wrapper `./mvnw`)

### Installation & Run

1. **Clone the repository:**
```bash
git clone https://github.com/your-username/loan-management-system.git
cd loan-management-system

```


2. **Build and package:**
```bash
mvn clean package -DskipTests

```


3. **Run the application:**
```bash
mvn spring-boot:run

```


*(Or run `LoanManagementSystemApplication.java` directly from IntelliJ IDEA / Eclipse).*
4. **Access the terminal:**
Open your browser and navigate to:
```text
http://localhost:8080/

```



---

## Pre-seeded Credentials

On application boot, `DataSeeder` initializes test accounts with BCrypt-hashed passwords:

| Role | Username | Password | Purpose / Scope |
| --- | --- | --- | --- |
| **Credit Officer** | `officer1` | `officer123` | Sanction loans, disburse facilities, record payments, and execute prepayments |
| **Customer 1** | `customer1` | `customer123` | Assigned CIF-101 (Aarav Sharma), view own loans and amortization ledgers |
| **Customer 2** | `customer2` | `customer123` | Assigned CIF-102 (Pooja Patel), view own loans and amortization ledgers |

---

## Developer Diagnostics

* **H2 In-Memory Database Web Console:**
URL: `http://localhost:8080/h2-console`
JDBC URL: `jdbc:h2:mem:loandb` | Username: `sa` | Password: *(blank)*
* **Spring Actuator Health Endpoint:**
URL: `http://localhost:8080/actuator/health`

---

## License

This project is licensed under the MIT License - feel free to use and adapt it for educational, enterprise evaluation, or commercial prototype purposes.
