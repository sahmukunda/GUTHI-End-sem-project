import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { useAuth } from "../auth/AuthContext";
import { apiJson } from "../api/client";
import type { AuthResponse } from "../types";

export function RegisterGuthiMemberPage() {
  const { login } = useAuth();
  const navigate = useNavigate();
  const [fullName, setFullName] = useState("");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [mobileNumber, setMobileNumber] = useState("");
  const [inviteCode, setInviteCode] = useState("");
  const [error, setError] = useState("");

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    setError("");
    try {
      const data = await apiJson<AuthResponse>("/api/auth/register-guthi-member", {
        method: "POST",
        body: JSON.stringify({
          fullName,
          email,
          password,
          mobileNumber: mobileNumber || undefined,
          inviteCode,
        }),
      });
      login(data);
      navigate("/guthi", { replace: true });
    } catch (err) {
      setError(err instanceof Error ? err.message : "Registration failed");
    }
  }

  return (
    <div className="auth-page auth-page--narrow">
      <p className="auth-page__back">
        <Link to="/login">← Sign-in choices</Link>
      </p>
      <h1 className="auth-page__title auth-page__title--left">Create Guthi member account</h1>
      <p className="auth-page__lede auth-page__lede--left">
        For trustees and operational staff who run approvals, volunteer programmes, and festival records.
        Requires an <strong>invite code</strong> from your governing circle (disabled in production unless your
        server enables it).
      </p>
      <form className="panel panel--emphasis form-stack" onSubmit={handleSubmit}>
        {error ? <p className="error">{error}</p> : null}
        <label htmlFor="reg-guthi-invite">Invite code</label>
        <input
          id="reg-guthi-invite"
          value={inviteCode}
          onChange={(e) => setInviteCode(e.target.value)}
          required
          autoComplete="off"
          placeholder="Local dev example: guthi-local-member-2026"
        />
        <label htmlFor="reg-guthi-name">Full name</label>
        <input id="reg-guthi-name" value={fullName} onChange={(e) => setFullName(e.target.value)} required />
        <label htmlFor="reg-guthi-email">Email</label>
        <input id="reg-guthi-email" type="email" value={email} onChange={(e) => setEmail(e.target.value)} required />
        <label htmlFor="reg-guthi-password">Password (min 8 characters)</label>
        <input
          id="reg-guthi-password"
          type="password"
          value={password}
          onChange={(e) => setPassword(e.target.value)}
          minLength={8}
          required
        />
        <label htmlFor="reg-guthi-mobile">Mobile (optional)</label>
        <input
          id="reg-guthi-mobile"
          type="tel"
          value={mobileNumber}
          onChange={(e) => setMobileNumber(e.target.value)}
          maxLength={30}
          placeholder="e.g. 9801122334"
        />
        <button type="submit" className="auth-btn-block">
          Create Guthi member account
        </button>
      </form>
      <p className="auth-page__foot">
        <Link to="/login/guthi">Already registered? Sign in</Link>
        {" · "}
        <Link to="/register/community">Community member instead?</Link>
      </p>
    </div>
  );
}
