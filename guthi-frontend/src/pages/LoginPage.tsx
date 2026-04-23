import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { useAuth } from "../auth/AuthContext";
import { apiJson } from "../api/client";
import type { AuthResponse } from "../types";

type Portal = "user" | "guthi";

export function LoginPage({ portal }: { portal: Portal }) {
  const { login } = useAuth();
  const navigate = useNavigate();

  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState("");

  const isCommunity = portal === "user";

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    setError("");
    try {
      const data = await apiJson<AuthResponse>("/api/auth/login", {
        method: "POST",
        body: JSON.stringify({ email, password }),
      });

      if (isCommunity && data.role === "ADMIN") {
        setError(
          "This account is a Guthi member (trustee/staff). Use the Guthi member sign-in to open that workspace."
        );
        return;
      }
      if (!isCommunity && data.role !== "ADMIN") {
        setError(
          "This account is a community member. Use the community member sign-in for the public portal."
        );
        return;
      }

      login(data);
      if (data.role === "ADMIN") {
        navigate("/guthi", { replace: true });
      } else {
        navigate("/u", { replace: true });
      }
    } catch (err) {
      setError(err instanceof Error ? err.message : "Login failed");
    }
  }

  const title = isCommunity ? "Community member sign-in" : "Guthi member sign-in";
  const blurb = isCommunity
    ? "Use the email and password for your community account."
    : "Use the email and password issued to trustees and operational Guthi members.";

  return (
    <div className="auth-page auth-page--narrow">
      <p className="auth-page__back">
        <Link to="/login">← Back to sign-in choices</Link>
      </p>
      <h1 className="auth-page__title auth-page__title--left">{title}</h1>
      <p className="auth-page__lede auth-page__lede--left">{blurb}</p>
      <form className="card form-stack" onSubmit={handleSubmit}>
        {error ? <p className="error">{error}</p> : null}
        <label htmlFor="login-email">Email</label>
        <input
          id="login-email"
          type="email"
          autoComplete="username"
          value={email}
          onChange={(e) => setEmail(e.target.value)}
          required
        />
        <label htmlFor="login-password">Password</label>
        <input
          id="login-password"
          type="password"
          autoComplete="current-password"
          value={password}
          onChange={(e) => setPassword(e.target.value)}
          required
        />
        <button type="submit" className="auth-btn-block">
          {isCommunity ? "Sign in to community portal" : "Sign in to Guthi workspace"}
        </button>
      </form>
      {isCommunity ? (
        <p className="auth-page__foot">
          <Link to="/register/community">Create community account</Link>
          {" · "}
          <Link to="/login/guthi">I am a Guthi member</Link>
        </p>
      ) : (
        <p className="auth-page__foot">
          <Link to="/register/guthi">Create Guthi member account</Link>
          {" · "}
          <Link to="/login/user">I am a community member</Link>
        </p>
      )}
    </div>
  );
}
