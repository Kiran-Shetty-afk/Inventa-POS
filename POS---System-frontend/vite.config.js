import path from "path";
import { fileURLToPath } from "url";
import tailwindcss from "@tailwindcss/vite";
import react from "@vitejs/plugin-react";
import { defineConfig } from "vite";

const __dirname = path.dirname(fileURLToPath(import.meta.url));

/** Spring (and similar stacks) may reject proxied requests if the browser Origin is forwarded. */
const devApiProxy = {
  target: "http://localhost:5000",
  changeOrigin: true,
  configure: (proxy) => {
    proxy.on("proxyReq", (proxyReq) => {
      proxyReq.removeHeader("origin");
    });
  },
};

/**
 * Same-origin SPA routes live under `/auth/*` (e.g. `/auth/login`). The API also uses POST `/auth/login`.
 * A browser GET (refresh, Back) must serve the Vite app, not proxy to Spring — otherwise Spring returns
 * "Request method 'GET' is not supported" for page navigations.
 */
const devAuthApiProxy = {
  ...devApiProxy,
  bypass(req) {
    if (req.method === "GET") {
      return "/index.html";
    }
  },
};

export default defineConfig({
  plugins: [react(), tailwindcss()],
  server: {
    proxy: {
      // Dev-only: same-origin API calls avoid browser CORS; backend runs on :5000
      "/api": devApiProxy,
      "/auth": devAuthApiProxy,
      "/onboarding": devApiProxy,
      "/users": devApiProxy,
    },
  },
  resolve: {
    alias: {
      "@": path.resolve(__dirname, "./src"),
    },
  },
  build: {
    outDir: "dist", // 👈 revert to default
  },
});
