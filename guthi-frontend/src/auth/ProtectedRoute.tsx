import { Navigate, useLocation } from "react-router-dom";
import { useAuth } from "./AuthContext";
import type { Role } from "../types";

export function ProtectedRoute({
  children,
  role,
}: {
  children: React.ReactNode;
  role?: Role;
}) {
  const { auth } = useAuth();
  const loc = useLocation();

  if (!auth) {
    const loginPath = loc.pathname.startsWith("/guthi") ? "/login/guthi" : "/login/user";
    return <Navigate to={loginPath} state={{ from: loc }} replace />;
  }
  if (role && auth.role !== role) {
    return <Navigate to={auth.role === "ADMIN" ? "/guthi" : "/u"} replace />;
  }
  return <>{children}</>;
}
