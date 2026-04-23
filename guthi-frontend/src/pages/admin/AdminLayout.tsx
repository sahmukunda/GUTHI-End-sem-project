import { NavLink, Outlet, useNavigate } from "react-router-dom";
import { useAuth } from "../../auth/AuthContext";

export function AdminLayout() {
  const { auth, logout } = useAuth();
  const navigate = useNavigate();

  function handleLogout() {
    logout();
    navigate("/login/guthi", { replace: true });
  }

  return (
    <div className="layout-root layout-root--admin">
      <header className="app-header">
        <div className="app-header__brand">
          <strong>Guthi member</strong>
          <span className="app-header__badge">Workspace</span>
        </div>
        <nav className="app-header__nav" aria-label="Guthi workspace">
          <NavLink to="/guthi" end className={({ isActive }) => `app-nav-link${isActive ? " app-nav-link--active" : ""}`}>
            Home
          </NavLink>
          <NavLink
            to="/guthi/donations"
            className={({ isActive }) => `app-nav-link${isActive ? " app-nav-link--active" : ""}`}
          >
            Donations
          </NavLink>
          <NavLink
            to="/guthi/requests"
            className={({ isActive }) => `app-nav-link${isActive ? " app-nav-link--active" : ""}`}
          >
            Requests
          </NavLink>
          <NavLink
            to="/guthi/volunteer"
            className={({ isActive }) => `app-nav-link${isActive ? " app-nav-link--active" : ""}`}
          >
            Volunteer
          </NavLink>
          <NavLink
            to="/guthi/festivals"
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
