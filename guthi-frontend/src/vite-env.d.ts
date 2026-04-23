/// <reference types="vite/client" />

interface ImportMetaEnv {
  /** Set to `""` to use the Vite proxy; otherwise absolute API origin (e.g. `http://localhost:9090`). */
  readonly VITE_API_BASE?: string;
}
