import { Link } from "react-router-dom";

export function LoginGate() {
  return (
    <div className="auth-page">
      <h1 className="auth-page__title">Guthi Governance</h1>
      <div className="auth-page__divider" />
      <p className="auth-page__lede">
        Choose who you are. Accounts are issued in advance, the same way a college issues a staff or
        student email. There is no sign-up page. Sign in with the email and password stored for you.
      </p>
      <div className="choice-grid">
        <Link to="/login/user" className="choice-tile">
          <h2>Community member</h2>
          <p>Donations, requests, volunteering, work posts, and festival reviews.</p>
          <span className="choice-tile__cta">Sign in →</span>
        </Link>
        <Link to="/login/guthi" className="choice-tile choice-tile--accent">
          <h2>Guthi member</h2>
          <p>Approve requests, manage volunteer events, and publish festivals.</p>
          <span className="choice-tile__cta">Sign in →</span>
        </Link>
      </div>
    </div>
  );
}
