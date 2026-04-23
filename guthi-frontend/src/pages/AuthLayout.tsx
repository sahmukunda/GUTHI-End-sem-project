import type { ReactNode } from "react";

export function AuthLayout({ children }: { children: ReactNode }) {
  return (
    <div className="auth-bg">
      <div className="auth-bg__overlay" />
      <div className="auth-bg__content">{children}</div>
    </div>
  );
}
