import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { useAuth } from "../auth/AuthContext";
import { apiJson } from "../api/client";
import type { AuthResponse } from "../types";

export function RegisterCommunityPage() {
  const { login } = useAuth();
  const navigate = useNavigate();
  const [fullName, setFullName] = useState("");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [mobileNumber, setMobileNumber] = useState("");
  const [error, setError] = useState("");

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    setError("");
    try {
      const data = await apiJson<AuthResponse>("/api/auth/register", {
        method: "POST",
        body: JSON.stringify({
          fullName,
          email,
          password,
          mobileNumber: mobileNumber || undefined,
        }),
      });
      login(data);
      navigate("/u", { replace: true });
    } catch (err) {
      setError(err instanceof Error ? err.message : "Registration failed");
    }
  }

  return (
    <div className="auth-page auth-page--narrow">
      <p className="auth-page__back">
        <Link to="/login">← Sign-in choices</Link>
      </p>
      <h1 className="auth-page__title auth-page__title--left">Create community account</h1>
      <p className="auth-page__lede auth-page__lede--left">
        For neighbours and families: donations, requests, volunteering, work posts, and festival reviews.
        You will get a <strong>community member</strong> account.
      </p>
      <form className="card form-stack" onSubmit={handleSubmit}>
        {error ? <p className="error">{error}</p> : null}
        <label htmlFor="reg-community-name">Full name</label>
        <input id="reg-community-name" value={fullName} onChange={(e) => setFullName(e.target.value)} required />
        <label htmlFor="reg-community-email">Email</label>
        <input
          id="reg-community-email"
          type="email"
          value={email}
          onChange={(e) => setEmail(e.target.value)}
          required
        />
        <label htmlFor="reg-community-password">Password (min 8 characters)</label>
        <input
          id="reg-community-password"
          type="password"
          value={password}
          onChange={(e) => setPassword(e.target.value)}
          minLength={8}
          required
        />
        <label htmlFor="reg-community-mobile">Mobile (optional)</label>
        <input
          id="reg-community-mobile"
          type="tel"
          value={mobileNumber}
          onChange={(e) => setMobileNumber(e.target.value)}
          maxLength={30}
          placeholder="e.g. 9801122334"
        />
        <button type="submit" className="auth-btn-block">
          Create community account
        </button>
      </form>
      <p className="auth-page__foot">
        <Link to="/login/user">Already registered? Sign in</Link>
        {" · "}
        <Link to="/register/guthi">Guthi member instead?</Link>
      </p>
    </div>
  );
}
