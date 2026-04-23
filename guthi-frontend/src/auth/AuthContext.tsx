import {
  createContext,
  useCallback,
  useContext,
  useMemo,
  useState,
  type ReactNode,
} from "react";
import type { AuthResponse, Role } from "../types";
import { clearAuth, loadAuth, saveAuth } from "../api/client";

type AuthContextValue = {
  auth: AuthResponse | null;
  login: (data: AuthResponse) => void;
  logout: () => void;
  isAdmin: boolean;
  isUser: boolean;
};

const AuthContext = createContext<AuthContextValue | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [auth, setAuth] = useState<AuthResponse | null>(() => loadAuth());

  const login = useCallback((data: AuthResponse) => {
    saveAuth(data);
    setAuth(data);
  }, []);

  const logout = useCallback(() => {
    clearAuth();
    setAuth(null);
  }, []);

  const value = useMemo<AuthContextValue>(() => {
    const role: Role | undefined = auth?.role;
    return {
      auth,
      login,
      logout,
      isAdmin: role === "ADMIN",
      isUser: role === "USER",
    };
  }, [auth, login, logout]);

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthContextValue {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error("useAuth outside AuthProvider");
  return ctx;
}
