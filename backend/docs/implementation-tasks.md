# Implementation Tasks — Veterinary Clinic Management System

This is the step-by-step checklist used to guide small, incremental Claude Code sessions. Each task lists what to implement, which files/packages it may touch, and what command to run afterward to verify. Work through tasks in order; do not jump ahead. Each task should be its own small session/commit where possible.

Verification commands referenced below:
- `./mvnw compile` — compiles the project.
- `./mvnw test` — runs the test suite (uses H2, per `src/test/resources/application.properties`).
- `./mvnw spring-boot:run` — runs the app locally against PostgreSQL.

---

## Week 1 — Foundations

1. ~~**Add missing dependencies**~~ ✅ done
   - Add `springdoc-openapi-starter-webmvc-ui` and a JWT library (e.g. `io.jsonwebtoken:jjwt-api` + `jjwt-impl` + `jjwt-jackson`) to `pom.xml`.
   - Files: `pom.xml`.
   - Verify: `./mvnw compile`.

2. ~~**Swagger/OpenAPI setup**~~ ✅ done
   - Add minimal OpenAPI config (title, description, security scheme for Bearer JWT).
   - Files: `config/OpenApiConfig.java`.
   - Verify: `./mvnw spring-boot:run`, check `/swagger-ui.html`.

3. ~~**CORS configuration**~~ ✅ done
   - Allow the frontend dev origin(s) for local development.
   - Files: `config/CorsConfig.java` (or within security config).
   - Verify: `./mvnw compile`.

4. ~~**Package structure**~~ ✅ done
   - Create empty module packages: `auth`, `owner`, `pet`, `vet`, `visit`, `vaccination`, `invoice`, `dashboard`, `common`, `security`, `config`.
   - Verify: `./mvnw compile`.

5. ~~**Global error handling**~~ ✅ done
   - `GlobalExceptionHandler` producing the standard error shape from `docs/api-contract.md`.
   - Files: `common/exception/GlobalExceptionHandler.java`, `common/exception/*Exception.java`.
   - Verify: `./mvnw test`.

6. ~~**DTO / PageResponse standard**~~ ✅ done
   - Generic `PageResponse<T>` DTO used by all listing endpoints.
   - Files: `common/dto/PageResponse.java`.
   - Verify: `./mvnw compile`.

7. ~~**User/Role entity + JWT auth**~~ ✅ done
   - `User` entity, `Role` enum, password hashing, JWT issuing/validation, Spring Security config.
   - Files: `security/*`, `auth/*`.
   - Verify: `./mvnw test`.

8. ~~**Auth endpoints**~~ ✅ done
   - `POST /api/auth/register`, `POST /api/auth/login`, `GET /api/auth/me`.
   - Files: `auth/AuthController.java`, `auth/AuthService.java`, `auth/dto/*`.
   - Verify: `./mvnw test`, manual login via Swagger.

9. ~~**Seed users**~~ ✅ done (2026-07-07)
   - Data seeding for at least one ADMIN, one VET, one RECEPTIONIST for local/demo use.
   - Files: `config/DataSeeder.java` (or `CommandLineRunner`).
   - Verify: `./mvnw spring-boot:run`, confirm login works for seeded users.

10. ~~**Owner CRUD (start)**~~ ✅ done (2026-07-07)
    - `Owner` entity, repository, basic create/list.
    - Files: `owner/*`.
    - Verify: `./mvnw test`.

11. ~~**Pet/Vet CRUD (start)**~~ ✅ done (2026-07-07)
    - `Pet`, `Vet` entities, repositories, basic create/list.
    - Files: `pet/*`, `vet/*`.
    - Verify: `./mvnw test`.

---

## Week 2 — Core CRUD + Visits

12. ~~**Complete Owner CRUD**~~ ✅ done (2026-07-07)
    - Full CRUD + validation.
    - Files: `owner/*`.
    - Verify: `./mvnw test`.

13. ~~**Owner detail with pet list/count**~~ ✅ done (2026-07-07)
    - `GET /api/owners/{id}` includes pets and pet count.
    - Files: `owner/*`.
    - Verify: `./mvnw test`.

14. ~~**Complete Pet CRUD**~~ ✅ done (2026-07-07)
    - Full CRUD + species/breed conditional validation rule.
    - Files: `pet/*`.
    - Verify: `./mvnw test`.

15. ~~**Pet archive/activate**~~ ✅ done (2026-07-07)
    - `PATCH /api/pets/{id}/archive`, `PATCH /api/pets/{id}/activate`.
    - Files: `pet/*`.
    - Verify: `./mvnw test`.

16. ~~**Pet search/filter/sort/pagination**~~ ✅ done (2026-07-07)
    - `GET /api/pets` with `search`, `species`, `ownerId`, `active`, sort, pagination.
    - Files: `pet/*`.
    - Verify: `./mvnw test`.

17. ~~**Complete Vet CRUD**~~ ✅ done (2026-07-08)
    - Full CRUD.
    - Files: `vet/*`.
    - Verify: `./mvnw test`.

18. ~~**Visit entity + status enum**~~ ✅ done (2026-07-08)
    - `Visit` entity, `VisitStatus` enum, repository.
    - Files: `visit/*`.
    - Verify: `./mvnw test`.

19. ~~**Visit CRUD**~~ ✅ done (2026-07-08)
    - Create/read/update visits.
    - Files: `visit/*`.
    - Verify: `./mvnw test`.

20. ~~**Visit status update endpoint**~~ ✅ done (2026-07-08)
    - `PATCH /api/visits/{id}/status`.
    - Files: `visit/*`.
    - Verify: `./mvnw test`.

21. ~~**Visit filter + calendar endpoint**~~ ✅ done (2026-07-08)
    - `GET /api/visits` with vet/date-range/status filters; `GET /api/visits/calendar`.
    - Files: `visit/*`.
    - Verify: `./mvnw test`.

22. ~~**Appointment overlap rule**~~ ✅ done (2026-07-08)
    - ±15 minute same-vet overlap check (see `docs/business-rules.md` §1).
    - Files: `visit/VisitService.java`.
    - Verify: `./mvnw test` (add overlap test case).

23. ~~**Pet visit history endpoint**~~ ✅ done (2026-07-09)
    - `GET /api/pets/{id}/visits`.
    - Files: `pet/*`, `visit/*`.
    - Verify: `./mvnw test`.

---

## Week 3 — Vaccinations, Weight, Invoices, Treatment Notes

24. ~~**Vaccination entity + CRUD**~~ ✅ done (2026-07-10)
    - `Vaccination` entity, full CRUD.
    - Files: `vaccination/*`.
    - Verify: `./mvnw test`.

25. ~~**Vaccination nextDueDate rule**~~ ✅ done (2026-07-10)
    - Server-side calculation (see `docs/business-rules.md` §3).
    - Files: `vaccination/VaccinationService.java`.
    - Verify: `./mvnw test`.

26. ~~**Pet vaccination history + age-based warnings**~~ ✅ done (2026-07-10)
    - `GET /api/pets/{id}/vaccinations`; document/support age-based vaccine warning data (§10 in business rules).
    - Files: `pet/*`, `vaccination/*`.
    - Verify: `./mvnw test`.

27. ~~**PetWeightRecord entity + endpoints**~~ ✅ done (2026-07-10)
    - `GET/POST /api/pets/{id}/weight-records`.
    - Files: `pet/*` (or a `weight` sub-package).
    - Verify: `./mvnw test`.

28. ~~**Invoice + InvoiceItem entities**~~ ✅ done (2026-07-10)
    - Entities, `InvoiceStatus` and `InvoiceItemCategory` enums.
    - Files: `invoice/*`.
    - Verify: `./mvnw test`.

29. ~~**Invoice create + subtotal/VAT/total calculation**~~ ✅ done (2026-07-10)
    - `POST /api/invoices` with server-side calculation (see `docs/business-rules.md` §4).
    - Files: `invoice/InvoiceService.java`.
    - Verify: `./mvnw test` (add calculation test cases).

30. ~~**Invoice listing/filtering**~~ ✅ done (2026-07-11)
    - `GET /api/invoices` with status/date-range filters.
    - Files: `invoice/*`.
    - Verify: `./mvnw test`.

31. ~~**Invoice send/mark-paid/bulk-paid**~~ ✅ done (2026-07-11)
    - `PATCH /api/invoices/{id}/send`, `PATCH /api/invoices/{id}/mark-paid`, `PATCH /api/invoices/bulk-mark-paid`.
    - Files: `invoice/*`.
    - Verify: `./mvnw test`.

32. ~~**Treatment notes update + role rule**~~ ✅ done (2026-07-11)
    - `PATCH /api/visits/{id}/medical-notes`, RECEPTIONIST forbidden (see `docs/business-rules.md` §5).
    - Files: `visit/*`, `security/*`.
    - Verify: `./mvnw test` (add a test asserting RECEPTIONIST gets 403).

33. ~~**Allergy warning infrastructure**~~ ✅ done (2026-07-11)
    - Non-blocking warning when treatment references a drug conflicting with `Pet.allergies` (see `docs/business-rules.md` §7).
    - Files: `visit/*` or a new `common`/`treatment` helper.
    - Verify: `./mvnw test`.

---

## Week 4 — Dashboard, Polish, Demo Readiness

34. **Admin-driven password reset (no self-service forgot-password, no remember-me)**
    - `PATCH /api/auth/users/{id}/reset-password`, ADMIN-only — admin sets/generates a new password for a user; no email, no token, no expiry logic. Replaces self-service "forgot password" and "remember me" for this phase (see `decisions.md` entry 20).
    - Files: `auth/AuthController.java`, `auth/AuthService.java`, `auth/dto/*`, `security/User.java` (add password-update capability).
    - Verify: `./mvnw test` (assert non-ADMIN gets 403).

35. ~~**Password strength policy**~~ ✅ done (2026-07-12)
    - Beyond the existing min-8-characters rule, require at least one uppercase letter, one lowercase letter, one digit, and one punctuation character in `RegisterRequest.password` and `ResetPasswordRequest.newPassword` (see `decisions.md` entry 21).
    - Files: `auth/dto/RegisterRequest.java`, `auth/dto/ResetPasswordRequest.java`.
    - Verify: `./mvnw test` (assert weak passwords get 400).

36. ~~**Dashboard summary DTO + KPI queries**~~ ✅ done (2026-07-12)
    - `todayAppointments`, `activePatients`, `pendingVaccinations`, `unpaidInvoices`.
    - Files: `dashboard/*`.
    - Verify: `./mvnw test`.

37. ~~**Monthly revenue + revenue category chart data**~~ ✅ done (2026-07-12)
    - Files: `dashboard/*`.
    - Verify: `./mvnw test`.

38. ~~**30-day appointment trend + cumulative appointments YTD**~~ ✅ done (2026-07-12)
    - Files: `dashboard/*`.
    - Verify: `./mvnw test`.

39. ~~**Vet-by-vet appointment count**~~ ✅ done (2026-07-13)
    - Files: `dashboard/*`.
    - Verify: `./mvnw test`.

40. ~~**Today schedule + alerts (vaccination, overdue follow-up)**~~ ✅ done (2026-07-13)
    - Files: `dashboard/*`.
    - Verify: `./mvnw test`.

41. ~~**Dashboard endpoint assembly**~~ ✅ done (2026-07-13)
    - `GET /api/dashboard/summary` wiring all of the above.
    - Files: `dashboard/DashboardController.java`.
    - Verify: `./mvnw test`, manual check via Swagger.

42. ~~**Vet performance endpoint**~~ ✅ done (2026-07-13)
    - `GET /api/vets/{id}/performance`.
    - Files: `vet/*`.
    - Verify: `./mvnw test`.

43. ~~**Follow-up endpoint**~~ ✅ done (2026-07-13)
    - `POST /api/visits/{id}/follow-up` (see `docs/business-rules.md` §9).
    - Files: `visit/*`.
    - Verify: `./mvnw test`.

44. ~~**Inactive pet logic**~~ ✅ done (2026-07-13)
    - Computed `inactive` flag (>2 years without a visit) (see `docs/business-rules.md` §6).
    - Files: `pet/*`.
    - Verify: `./mvnw test`.

45. ~~**Final search/filter cleanup**~~ ✅ done (2026-07-14)
    - Review all listing endpoints for consistent query params and `PageResponse` usage.
    - Files: across modules.
    - Verify: `./mvnw test`.

46. ~~**Swagger cleanup**~~ ✅ done (2026-07-14)
    - Ensure all endpoints have descriptions, examples, and correct security annotations.
    - Files: across modules, `config/OpenApiConfig.java`.
    - Verify: manual check via `/swagger-ui.html`.

47. ~~**Seed demo data**~~ ✅ done (2026-07-14)
    - Realistic demo dataset covering the full acceptance flow (owners, pets, vets, visits, vaccinations, invoices).
    - Files: `config/DataSeeder.java`, `config/DemoDataSeeder.java`.
    - Verify: `./mvnw spring-boot:run`, manually walk the demo flow.

48. ~~**Tests pass**~~ ✅ done (2026-07-14)
    - Full suite green.
    - Verify: `./mvnw test`.

49. **README**
    - Setup instructions, env vars, how to run, how to seed data.
    - Files: `README.md`.
    - Verify: manual read-through.

50. **Frontend integration fixes**
    - Address any contract mismatches found while wiring up the real frontend.
    - Files: as needed, cross-referencing `docs/api-contract.md`.
    - Verify: `./mvnw test`, manual frontend smoke test.

51. **Final demo flow validation**
    - Walk through the full acceptance flow in `docs/backend-spec.md` §15 end-to-end.
    - Verify: manual run through Swagger or the real frontend, no build errors.

---

## Week 5 — Post-launch additions

52. ~~**Contact Support module**~~ ✅ done (2026-07-26)
    - `SupportRequest` entity/enum, ticket CRUD (create/list/detail/status-update), own-ticket scoping for non-admin, admin-only status transitions. Email notification to configured admin recipients on ticket creation (Gmail SMTP via `spring-boot-starter-mail`, best-effort/non-blocking, disabled in tests). See `decisions.md` entries 36-37.
    - Files: `support/*`, `security/SecurityConfig.java`, `pom.xml`, `application.properties` (+ test variants), `docs/backend-spec.md`, `docs/api-contract.md`.
    - Verify: `./mvnw test`.

53. ~~**Appointment reminder emails**~~ ✅ done (2026-08-08)
    - `Visit.reminderSentAt` column; `VisitReminderScheduler` (`@Scheduled(cron = "0 0 9 * * *")`) finds tomorrow's `SCHEDULED` visits without a reminder sent yet and emails the owner via a new `VisitReminderNotifier` (mirrors `SupportRequestNotifier`, reuses existing Gmail SMTP infra), then stamps `reminderSentAt`. Gated by `app.reminders.enabled`, forced `false` in test config. See `decisions.md` entry 43, `docs/business-rules.md` §14.
    - Files: `visit/*`, `application.properties` (+ test variants), `docs/backend-spec.md`, `docs/api-contract.md`, `docs/business-rules.md`.
    - Verify: `./mvnw test` (scheduler logic testable by calling the job method directly with a fixed clock, not by waiting for 09:00).
