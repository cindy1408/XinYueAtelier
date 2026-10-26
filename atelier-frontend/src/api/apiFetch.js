import { API_URL } from "../config";
export const DEMO_MODE = import.meta.env.VITE_DEMO_MODE === "true";

const SAFE_METHODS = new Set(["GET", "HEAD", "OPTIONS", "TRACE"]);

async function getCsrfToken() {
  const response = await fetch(`${API_URL}/csrf`, { credentials: "include" });
  if (!response.ok) {
    throw new Error(`Failed to fetch CSRF token (${response.status})`);
  }
  return response.json();
}

export async function apiFetch(path, options = {}) {
  if (DEMO_MODE) {
    const { demoFetch } = await import("../demo/demoApi");
    return demoFetch(path, options);
  }

  const isFormData = options.body instanceof FormData;
  const method = (options.method || "GET").toUpperCase();
  const csrfHeaders = {};
  if (!SAFE_METHODS.has(method)) {
    const csrfToken = await getCsrfToken();
    csrfHeaders[csrfToken.headerName] = csrfToken.token;
  }

  return fetch(`${API_URL}${path}`, {
    ...options,
    credentials: "include",
    headers: {
      ...(isFormData ? {} : { "Content-Type": "application/json" }),
      ...options.headers,
      ...csrfHeaders,
    },
  });
}