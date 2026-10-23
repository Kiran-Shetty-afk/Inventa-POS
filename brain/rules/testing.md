# Testing and verification

- Use the checks supported by the project: Maven builds for Java services, `npm run build` for the Vite frontend, and Docker Compose configuration/build checks for container changes.
- Run automated tests when requested or when the change clearly needs focused regression coverage.
- Report exactly which commands ran and distinguish compile/build checks from behavioral or end-to-end verification.
- Never claim an integration passed unless its result was observed.
