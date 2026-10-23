# UI conventions

- Preserve the existing React component, Redux Toolkit, routing, and Tailwind patterns.
- Show actionable errors for failed user actions and clear empty states when data is absent.
- Keep authenticated requests on the existing API client and avoid logging credentials, tokens, or payment callbacks.
- For payment return pages, treat browser callback parameters as untrusted until the backend verifies them.
