---
id: "002"
title: "README Improvements (Microservices context, endpoints, cleanup)"
tier: quick
type: repo-maintenance
status: done
source: kenmark-plan
created: 2026-09-04
approved: 2026-09-04
completed: 2026-09-04
files:
  - README.md
---

# Plan — README Improvements

## Summary
The user has requested a few specific modifications to the `README.md` file to further emphasize the microservices architecture, add a table of service endpoints, adjust headings to remove "monolithic" terminology, and remove the "Roadmap Opportunities" section.

## Goal
To refine the `README.md` documentation by integrating microservice-focused language, providing a clear endpoint reference table, adjusting confusing legacy headings, and removing unnecessary sections.

## Recommended approach
Since this is a straightforward documentation update, a `quick` tier plan is sufficient.
1. **Project Definition**: Update the opening paragraph to explicitly mention that the project is a microservices architecture.
2. **Endpoints Table**: Extract the route mappings from the Gateway architecture diagram and add a new section `## Microservices Endpoints Overview` presenting them in a clear table format.
3. **Heading Change**: The user noted the "Running the Monolithic System Locally" heading. Since we already have a dedicated section for starting the microservices backend, I'll rename this section to `## Running the System Locally (Frontend & Legacy Backend)` or merge it, ensuring the word "Monolithic" is replaced with something more acceptable or aligned with the microservices context as requested.
4. **Remove Roadmap**: Delete the `## Roadmap Opportunities` section entirely.

## Phased plan

### Phase 1 — Implementation
- [ ] Edit `README.md` to update the opening paragraph with microservices terminology.
- [ ] Insert a new table `## Microservices Endpoints Overview` listing the service-wise endpoints based on Gateway routes.
- [ ] Rename the `## Running the Monolithic System Locally` heading to remove "Monolithic".
- [ ] Delete the `## Roadmap Opportunities` section.

### Phase 2 — Verification
- [ ] Review `README.md` formatting to ensure tables and headings render correctly in Markdown.

## Files likely involved

| File/area | Expected change |
| --- | --- |
| `README.md` | [MODIFY] Apply the requested changes |

## Acceptance criteria
- [ ] The first paragraph mentions the project is a microservices-based system.
- [ ] A table of service-wise endpoints is present.
- [ ] The heading "Running the Monolithic System Locally" is changed.
- [ ] "Roadmap Opportunities" is removed.

## Commands/checks to run
- None (Documentation only)

## Open questions
- None
