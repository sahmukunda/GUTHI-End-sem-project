import { NavLink, Outlet, useNavigate } from "react-router-dom";
import { useAuth } from "../../auth/AuthContext";

export function UserLayout() {
  const { auth, logout } = useAuth();
  const navigate = useNavigate();

  function handleLogout() {
    logout();
    navigate("/login/user", { replace: true });
  }

  return (
    <div className="layout-root layout-root--user">
      <header className="app-header">
        <div className="app-header__brand">
          <strong>Guthi</strong>
          <span className="app-header__badge">Community member</span>
        </div>
        <nav className="app-header__nav" aria-label="Community portal">
          <NavLink to="/u" end className={({ isActive }) => `app-nav-link${isActive ? " app-nav-link--active" : ""}`}>
            Home
          </NavLink>
          <NavLink
            to="/u/donations"
            className={({ isActive }) => `app-nav-link${isActive ? " app-nav-link--active" : ""}`}
          >
            Donations
          </NavLink>
          <NavLink
            to="/u/requests"
            className={({ isActive }) => `app-nav-link${isActive ? " app-nav-link--active" : ""}`}
          >
            Requests
          </NavLink>
          <NavLink
            to="/u/volunteer"
            className={({ isActive }) => `app-nav-link${isActive ? " app-nav-link--active" : ""}`}
          >
            Volunteer
          </NavLink>
          <NavLink to="/u/work" className={({ isActive }) => `app-nav-link${isActive ? " app-nav-link--active" : ""}`}>
            Work posts
          </NavLink>
          <NavLink
            to="/u/festivals"
            className={({ isActive }) => `app-nav-link${isActive ? " app-nav-link--active" : ""}`}
          >
            Festivals
          </NavLink>
        </nav>
        <span className="app-header__user" title={auth?.fullName ?? ""}>
          {auth?.fullName}
        </span>
        <button type="button" className="secondary" onClick={handleLogout}>
          Log out
        </button>
      </header>
      <main className="app-main">
        <Outlet />
      </main>
    </div>
  );
}
