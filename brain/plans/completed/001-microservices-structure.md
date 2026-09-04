---
id: "001"
title: "Microservices Structure and README Update"
tier: full-feature
type: refactor
status: done
source: kenmark-plan
created: 2026-09-04
approved: 2026-09-04
completed: 2026-09-04
files:
  - README.md
  - pom.xml
  - start-services.ps1
---

# Plan — Microservices Structure and README Update

## Summary
The current branch (`Kiran-microservices-update`) has the microservices implementation scattered in the root directory alongside the monolithic `POS---System` application and other folders. We will organize the microservices into a dedicated `POS---Microservices` directory and update the root `README.md` to reflect both the monolithic and microservices architectures.

## Goal
To properly organize the project folder structure by moving all microservice-related projects and files into a separate `POS---Microservices` folder. Update the root README file to document this new structure clearly, offering instructions for both architectures.

## Current understanding
The repository currently contains:
- `POS---System/` (Monolithic Spring Boot app)
- `POS---System-frontend/` (Frontend app)
- `POS---ML/` (Machine Learning app)
- Microservices in root (`api-gateway`, `billing-analytics-service`, `eureka-server`, `inventory-catalog-service`, `order-sales-service`, `user-org-service`)
- A root `pom.xml` for the microservices build.
- `start-services.ps1` in the root.
- A root `README.md` that needs improvement compared to the original monolithic version.

## Recommended approach
Create a separate `POS---Microservices` folder. Moving the microservices implementation into the existing `POS---System` folder would mix the monolithic codebase with the distributed one, leading to confusion and complicated build scripts (since `POS---System` already has its own `pom.xml`). A dedicated folder cleanly separates the two architectural approaches.

## Phased plan

### Phase 1 — Preparation / discovery
- [ ] Inspect the root `README.md` and the monolithic `README.md` to understand what needs to be improved.

### Phase 2 — Implementation
- [ ] Create the `POS---Microservices` directory.
- [ ] Move the root `pom.xml`, `.mvn/`, `mvnw`, `mvnw.cmd`, `start-services.ps1` into `POS---Microservices/`.
- [ ] Move the microservice directories (`api-gateway`, `billing-analytics-service`, `eureka-server`, `inventory-catalog-service`, `order-sales-service`, `user-org-service`) into `POS---Microservices/`.
- [ ] Update the root `README.md` to document the new structure. Include sections for "Monolithic Architecture" and "Microservices Architecture".

### Phase 3 — Verification
- [ ] Verify that building `POS---Microservices` works by running `mvn clean install` within that directory.
- [ ] Verify that building `POS---System` still works independently.

### Phase 4 — Documentation / KB update
- [ ] Update `brain/kb` if needed.

## Files likely involved

| File/area | Expected change |
| --- | --- |
| `POS---Microservices/` | [NEW] Directory for microservices |
| `api-gateway/`, `eureka-server/`, etc. | [MOVE] Moved into `POS---Microservices/` |
| `pom.xml`, `mvnw*`, `start-services.ps1` | [MOVE] Moved into `POS---Microservices/` |
| `README.md` | [MODIFY] Revamp documentation for the whole repo |

## Risks

| Risk | Impact | Mitigation |
| --- | --- | --- |
| Broken build | High | Test Maven build in the new `POS---Microservices` folder before finalizing. |
| Missing dependencies | Medium | Ensure all modules are moved and the parent `pom.xml` correctly references them in the new location. |

## Acceptance criteria
- [ ] All microservice folders are inside `POS---Microservices/`.
- [ ] The root folder contains only top-level project folders (`POS---System`, `POS---Microservices`, `POS---System-frontend`, `POS---ML`) and essential meta-files (`README.md`, `.gitignore`, `brain/`).
- [ ] Root `README.md` is updated and clear.
- [ ] Both monolithic and microservices applications can be built successfully.

## Commands/checks to run
- `cd POS---Microservices && ./mvnw clean compile`
- `cd POS---System && ./mvnw clean compile`

## Open questions
- None
