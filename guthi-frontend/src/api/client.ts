import type { AuthResponse } from "../types";

/**
 * Per-tab auth storage so multiple accounts can be signed in on the same browser in different
 * tabs: login/logout in one tab does not overwrite or clear sessions in other tabs. The API
 * remains stateless (JWT on each request); only where the browser keeps the token changes.
 */
const STORAGE_KEY = "guthi.auth";

function store(): Storage {
  return sessionStorage;
}

export function loadAuth(): AuthResponse | null {
  try {
    const raw = store().getItem(STORAGE_KEY);
    if (!raw) return null;
    return JSON.parse(raw) as AuthResponse;
  } catch {
    return null;
  }
}

export function saveAuth(auth: AuthResponse): void {
  store().setItem(STORAGE_KEY, JSON.stringify(auth));
}

export function clearAuth(): void {
  store().removeItem(STORAGE_KEY);
}

async function parseError(res: Response): Promise<string> {
  const text = await res.text();
  try {
    const j = JSON.parse(text) as { message?: string };
    const msg = j.message ?? (text || res.statusText);
    if (res.status === 403) {
      return `${msg} If you are signed in, try signing out and back in.`;
    }
    return msg;
  } catch {
    if (res.status === 403) {
      return "Access denied (403). Sign out and sign in again if you expected this request to work.";
    }
    return text || res.statusText;
  }
}

/**
 * In dev, default to calling Spring on port 9090 so `Authorization` is never dropped by a proxy.
 * Set `VITE_API_BASE=` (empty) in `.env.development` to use relative `/api` + Vite proxy only.
 */
function apiBaseUrl(): string {
  const envBase = import.meta.env.VITE_API_BASE;
  if (envBase !== undefined) {
    return envBase.replace(/\/$/, "");
  }
  if (import.meta.env.DEV) {
    return "http://localhost:9090";
  }
  return "";
}

export function apiUrl(path: string): string {
  const base = apiBaseUrl();
  if (!path.startsWith("/")) {
    return base ? `${base}/${path}` : path;
  }
  return base ? `${base}${path}` : path;
}

/** WebSocket STOMP endpoint (no trailing slash). */
export function wsBrokerConnectUrl(accessToken: string | null): string {
  const base = apiBaseUrl();
  const wsOrigin =
    base.length > 0
      ? base.replace(/^http/, "ws")
      : `${typeof window !== "undefined" && window.location.protocol === "https:" ? "wss:" : "ws:"}//${typeof window !== "undefined" ? window.location.host : "localhost:5173"}`;
  const path = `${wsOrigin}/ws`;
  if (accessToken) {
    return `${path}?access_token=${encodeURIComponent(accessToken)}`;
  }
  return path;
}

/** Do not send a stored JWT on public auth calls (avoids filter errors + wrong role context). */
function resolveRequestCache(path: string, init: RequestInit): RequestCache {
  if (init.cache !== undefined) {
    return init.cache;
  }
  const method = (init.method ?? "GET").toUpperCase();
  if (method !== "GET") {
    return "no-store";
  }
  if (
    path === "/api/donations/summary" ||
    path === "/api/requests/my" ||
    path === "/api/requests/volunteer-calls" ||
    path === "/api/requests/all" ||
    path === "/api/admin/donation-claims"
  ) {
    return "no-store";
  }
  return "default";
}

function shouldAttachAuth(path: string): boolean {
  if (
    path === "/api/auth/login" ||
    path === "/api/auth/register" ||
    path === "/api/auth/register-guthi-member"
  ) {
    return false;
  }
  return true;
}

export async function apiJson<T>(
  path: string,
  init: RequestInit = {}
): Promise<T> {
  const auth = loadAuth();
  const headers = new Headers(init.headers);
  if (!headers.has("Content-Type") && init.body != null) {
    headers.set("Content-Type", "application/json");
  }
  if (auth?.token && shouldAttachAuth(path)) {
    headers.set("Authorization", `Bearer ${auth.token}`);
  }
  // Some GET URLs are the same with or without Authorization; avoid caching anonymous/forbidden bodies.
  const cache = resolveRequestCache(path, init);
  const url = apiUrl(path);
  const res = await fetch(url, { ...init, headers, cache });
  if (!res.ok) {
    throw new Error(await parseError(res));
  }
  if (res.status === 204) {
    return undefined as T;
  }
  const ct = res.headers.get("content-type");
  if (ct?.includes("application/json")) {
    return (await res.json()) as T;
  }
  return (await res.text()) as T;
}

/** Optional query params for PhonePe dynamic QR amount (paise) and payer-visible message. */
export function donationQrUrl(amountPaise?: number, message?: string): string {
  const params = new URLSearchParams();
  if (amountPaise != null && amountPaise > 0) {
    params.set("amountPaise", String(amountPaise));
  }
  if (message?.trim()) {
    params.set("message", message.trim());
  }
  const q = params.toString();
  const path = q ? `/api/donations/qr?${q}` : "/api/donations/qr";
  return apiUrl(path);
}
