# Project standards

## Scope and quality

- Make the smallest complete change that solves the requested problem.
- Read the surrounding code and preserve the existing service boundaries.
- Keep credentials in environment variables or secret stores; never commit `.env` files.
- Update the relevant `brain/kb/` documents and `brain/CHANGELOG.md` after meaningful behavior or deployment changes.

## Project layout

- Java microservices live under `POS---Microservices/`.
- The React/Vite frontend lives under `POS---System-frontend/`.
- The FastAPI model service lives under `POS---ML/`.
- Docker Compose and monitoring configuration live at the repository root.
- Historical notes in `brain/` are retained; current project facts live in `brain/kb/`.
