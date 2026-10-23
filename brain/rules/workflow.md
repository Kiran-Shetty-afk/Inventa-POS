# Development workflow

## Git branches

- Use feature or fix branches for normal work.
- Treat `main`, `master`, `dev`, `develop`, `staging`, and `production` as protected deployment branches unless the repository policy is deliberately changed.
- Merge through a pull request unless the user explicitly approves a direct push and understands CI/CD may run.
- Never force-push or include `.env`, credentials, keys, or tokens in commits.

## Project knowledge

- Keep current facts in `brain/kb/`; retain the numbered historical notes at the `brain/` root.
- Update the changelog and affected KB pages in the same change batch as code that changes behavior, API contracts, permissions, or deployment.
