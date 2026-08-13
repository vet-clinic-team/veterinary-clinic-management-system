# Backend Specification — Veterinary Clinic Management System

## 1. Purpose

This document converts the original React frontend learning assignment into a concrete backend specification. In the original assignment the backend was expected to be mocked (MSW or JSON Server). In this project, the backend is a real Spring Boot API that the React frontend consumes directly.

## 2. Persona / Usage Profile

- Users: 1 receptionist, 2 veterinarians.
- Primary device: desktop Chrome. Must also be usable on tablet.
- Daily volume: ~30 appointments/day, ~50 patient files opened or updated per day.
- Receptionist responsibilities: create/update owners and pets, schedule visits, issue invoices.
- Vet responsibilities: manage examination, diagnosis, treatment notes, vaccination, and follow-up planning.

## 3. Roles

| Role | Responsibilities |
|---|---|
| `ADMIN` | Can manage all resources (system/dev convenience role — see note below). |
| `VET` | Manages medical data: diagnosis, treatment notes, follow-up data, vaccinations, and weight records. |
| `RECEPTIONIST` | Creates/updates owners, pets, visits, and invoices; can add pet weight records (weighing happens at check-in). **Cannot** edit treatment notes and **cannot** create/update vaccinations. |

> **ADMIN role clarification**: The original PDF-derived personas are only `RECEPTIONIST` and `VET` — the PDF does not define `ADMIN` as a required persona or entity. `ADMIN` is a backend/system convenience role added for development, seed data, full-access testing, and administration. It is **not** a separate domain entity — there is no `Admin` entity/table, only the `ADMIN` value of the `Role` enum. The three roles remain `ADMIN`, `VET`, `RECEPTIONIST`.

## 4. Main User Flow

1. Login.
2. Create owner.
3. Add pet to owner.
4. Create or select vet.
5. Create appointment/visit for pet.
6. Move visit status through `CHECKED_IN` → `IN_EXAM` → `COMPLETED`.
7. Add diagnosis and treatment notes.
8. Add vaccination record.
9. Add weight record.
10. Create invoice.
11. Mark invoice paid.
12. Dashboard reflects updated revenue, appointment, vaccination, and invoice data.

## 5. Modules

`auth`, `owner`, `pet`, `vet`, `visit`, `vaccination`, `invoice`, `dashboard`, `support`, `common`, `security`, `config`.

> **`support` module addition**: not part of the original PDF-derived spec — an internal contact-support ticket flow added after launch so clinic staff can report application problems to the admin. See `decisions.md` for rationale.

## 6. Entities

### 6.1 Required Entities

1. `User`
2. `Role`
3. `Owner`
4. `Pet`
5. `Vet`
6. `Visit`
7. `Vaccination`
8. `Invoice`
9. `InvoiceItem`
10. `PetWeightRecord`

> Note: `ADMIN` is a value of the `Role` enum only, not a domain entity — there is no `Admin` entity/table (see Roles clarification in section 3).

### 6.2 Optional / Later-Phase Entities

- `Drug`
- `TreatmentDrug`
- `PetDocument`
- `PetPhoto`

### 6.2.1 `support` Module Entities (post-launch addition, not in the original PDF spec)

**SupportRequest**
- id, requestedByUserId, subject, message, status, adminResponse, createdAt, updatedAt

**SupportRequestStatus (enum)**
- OPEN, IN_PROGRESS, RESOLVED

### 6.3 Entity Fields

**User**
- id, fullName, email, passwordHash, role, enabled, createdAt, updatedAt

**Role (enum)**
- ADMIN, VET, RECEPTIONIST

**Owner**
- id, firstName, lastName, phone, email, address, createdAt, updatedAt

**Pet**
- id, ownerId, name, species, breed, speciesNote, birthDate, sex, weightKg, allergies, chronicConditions, archived, createdAt, updatedAt

**Vet**
- id, name, specialty, licenseNo, workHours, active, createdAt, updatedAt

**Visit**
- id, petId, vetId, scheduledAt, status, chiefComplaint, diagnosis, treatmentNotes, followUpDate, reminderSentAt, createdAt, updatedAt

> `reminderSentAt` (nullable): stamped by the appointment reminder scheduled job once a reminder email has been sent for this visit (see `docs/business-rules.md` §14). Not client-settable — read-only on `VisitResponse`.

**VisitStatus (enum)**
- SCHEDULED, CHECKED_IN, IN_EXAM, COMPLETED, CANCELLED

**Vaccination**
- id, petId, vaccineType, administeredAt, lotNumber, nextDueDate, administeredBy, createdAt, updatedAt

**PetWeightRecord**
- id, petId, weightKg, recordedAt, note

**Invoice**
- id, visitId, issuedAt, subtotal, vatRate, vatAmount, total, status, createdAt, updatedAt

**InvoiceStatus (enum)**
- DRAFT, SENT, PAID

**InvoiceItem**
- id, invoiceId, description, category, quantity, unitPrice, lineTotal

**InvoiceItemCategory (enum)**
- CONSULTATION, VACCINATION, SURGERY, HOSPITAL, OTHER

## 7. Relationships

- One `Owner` has many `Pet`s.
- One `Pet` belongs to one `Owner`.
- One `Pet` has many `Visit`s, many `Vaccination`s, many `PetWeightRecord`s.
- One `Vet` has many `Visit`s.
- One `Visit` belongs to one `Pet` and one `Vet`.
- One `Visit` can have many `Invoice`s (`Invoice` → `Visit` is many-to-one; a visit is not limited to a single invoice, e.g. re-issue or split billing).
- One `Invoice` has many `InvoiceItem`s.
- One `User` has many `SupportRequest`s (`SupportRequest` → `User` is many-to-one, via `requestedBy`).

## 8. Date/Time Rules

- Use ISO-8601 date/time strings on the wire.
- Use `LocalDate` for date-only fields (e.g. `birthDate`, `followUpDate`, `nextDueDate`).
- Use `LocalDateTime` for `scheduledAt`, `issuedAt`, `administeredAt`, `reminderSentAt`, `createdAt`, `updatedAt`.

## 9. Frontend Pages the Backend Must Support

1. Dashboard
2. Pets listing
3. Owners listing
4. Visits / Appointments listing
5. Invoices listing
6. Pet detail
7. Owner detail
8. Visit detail
9. Vet detail
10. New Pet wizard
11. New Visit modal
12. New Vaccination modal

## 10. Listing Page Requirements

All major listing endpoints support: search (where relevant), filtering (where relevant), sorting, pagination, and the standard `PageResponse` DTO (see `docs/api-contract.md`).

- **Pets**: search, filter by species/owner/active-inactive, sort, pagination.
- **Owners**: search, include pet count or expandable pet summary.
- **Visits**: filter by vet, date range, status; support both table and calendar views.
- **Invoices**: filter by status and date range; support bulk mark-paid.

## 11. Detail Page Requirements

### 11.1 Pet Detail

- Profile data.
- Visit history.
- Vaccination history.
- Invoice history.
- Weight records for the last 12 months.
- Documents/photo upload: documented as a later phase, not implemented now.
- Conditional species/breed behavior: if species is CAT or DOG, breed applies; otherwise breed is not required and `speciesNote` is used.

### 11.2 Owner Detail

- Owner profile data.
- Owner's pets.
- Owner-related invoices, either directly or through the pet/visit relationship chain.
- `petCount` and `archived` are included on owner list/detail responses so the frontend can enable/disable the archive, activate, and delete actions or show a confirmation/error message before attempting them.
- An owner can be archived (soft-deleted) once all of its pets are archived (or it has none); hard-deleting an owner requires it to already be archived and to have no pet records at all. Pets are never cascade-deleted or auto-archived as a side effect of owner deletion. See `docs/business-rules.md` Rule 12.

### 11.3 Visit Detail

Supports a wizard-style frontend flow: Check-in → Examination → Diagnosis → Treatment → Invoice.

Backed by: visit status update endpoint, visit update endpoint, medical notes update endpoint, invoice creation endpoint.

## 12. Dashboard Requirements

`GET /api/dashboard/summary` must support:

- `todayAppointments` count
- `activePatients` count
- `pendingVaccinations` count
- `unpaidInvoices` count
- Monthly revenue bar chart data
- Revenue category split pie/donut chart data
- 30-day appointment trend line chart data
- Cumulative appointments area chart data (year-to-date)
- Vet-by-vet appointment count stacked bar data
- Today's appointment schedule mini-list
- Alerts for upcoming vaccinations
- Alerts for overdue follow-ups

## 13. Chart Data Expected by Frontend

1. Bar chart: monthly revenue.
2. Line chart: pet weight trend for the last 12 months.
3. Pie/donut chart: revenue category split.
4. Area chart: cumulative appointments year-to-date.
5. Stacked bar chart: vet-by-vet appointment count.

## 14. Invoice Rules

- Backend calculates `subtotal`, VAT rate (18%), `vatAmount`, and `total`.
- Frontend-submitted totals are never trusted.
- Invoice item categories: CONSULTATION, VACCINATION, SURGERY, HOSPITAL, OTHER.

Full detail in `docs/business-rules.md`.

## 15. Final Demo / Acceptance Flow

At the end of the project, the backend must support this full end-to-end flow:

1. Register/login user.
2. Create a new owner.
3. Create a new pet under that owner.
4. Create a vet.
5. Add today's appointment for that pet.
6. Move the visit through check-in, examination, diagnosis, treatment, and completed states.
7. Add a drug/treatment and return an allergy warning if it conflicts with the pet's allergies.
8. Issue an invoice.
9. Mark the invoice as paid.
10. See dashboard revenue and appointment data updated.
11. See pet weight trend data.
12. See vaccination `nextDueDate` auto-calculated.
13. All of the above works with no build errors.

## 16. Tech Stack

- Java 17
- Spring Boot 3
- Spring Web, Spring Data JPA, Spring Security
- JWT authentication
- PostgreSQL (development), H2 (tests)
- Bean Validation
- springdoc-openapi / Swagger
- JUnit / Spring Boot Test
- Maven
