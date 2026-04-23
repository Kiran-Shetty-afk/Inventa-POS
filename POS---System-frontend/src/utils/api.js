import axios from 'axios';

const api = axios.create({
  // In dev, use same origin so Vite can proxy to the API (see vite.config.js). Override with VITE_API_BASE_URL.
  baseURL:
    import.meta.env.VITE_API_BASE_URL ??
    (import.meta.env.DEV ? "" : "http://localhost:5000"),
  headers: {
    'Content-Type': 'application/json',
  },
});




export default api;
