# Stack and dependencies

Last updated: 2026-10-24
Status: reviewed

## Confirmed facts

- Java backend: Java 17, Maven, Spring Boot 3.4.3, Spring Cloud 2024.0.0, Spring Data JPA, MySQL Connector/J, Eureka, Gateway, and Actuator.
- Frontend: React 19, Vite 7, Redux Toolkit, React Router, Axios, Tailwind CSS, and npm scripts.
- ML API: Python 3.11, FastAPI, Uvicorn, scikit-learn, joblib, pandas, NumPy, and `py-eureka-client`.
- Container images use multi-stage builds for Java and frontend, and the ML image includes the checked-in model directory.

## Important files inspected

- `POS---Microservices/pom.xml` — shared backend versions.
- `POS---System-frontend/package.json` — frontend dependencies and build script.
- `POS---ML/requirements.txt` — Python dependencies; scikit-learn is pinned to match model serialization.
- `POS---Microservices/*/Dockerfile`, `POS---ML/Dockerfile`, `POS---System-frontend/Dockerfile` — image build stages and runtime commands.

## Assumptions

- The checked-in model artifacts are intended for inference and are not retrained by the container build.

## Unknowns / documentation gaps

- Dependency vulnerability status was not re-audited during this update.

## Maintenance notes

- Update language, framework, runtime, or package-manager versions when their build files change.
