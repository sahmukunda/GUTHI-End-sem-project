import { Link } from "react-router-dom";

export function LoginGate() {
  return (
    <div className="auth-page">
      <h1 className="auth-page__title">Guthi Governance</h1>
      <div className="auth-page__divider" />
      <p className="auth-page__lede">
        Choose who you are. Each path has its own sign-in and account creation. The same secure server
        powers both; you are routed to the right workspace after authentication.
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
      <p className="auth-page__foot">
        New? Create an account for your path:{" "}
        <Link to="/register/community">Community member</Link>
        {" · "}
        <Link to="/register/guthi">Guthi member</Link>
      </p>
    </div>
  );
}
