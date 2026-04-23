import { Navigate, Route, Routes, useLocation } from "react-router-dom";
import { useAuth } from "./auth/AuthContext";
import { ProtectedRoute } from "./auth/ProtectedRoute";
import { LoginGate } from "./pages/LoginGate";
import { LoginPage } from "./pages/LoginPage";
import { AuthLayout } from "./pages/AuthLayout";
import { RegisterCommunityPage } from "./pages/RegisterCommunityPage";
import { RegisterGuthiMemberPage } from "./pages/RegisterGuthiMemberPage";
import { UserLayout } from "./pages/user/UserLayout";
import { UserHome } from "./pages/user/UserHome";
import { DonationsPage } from "./pages/user/DonationsPage";
import { RequestsPage } from "./pages/user/RequestsPage";
import { VolunteerPage } from "./pages/user/VolunteerPage";
import { WorkPostsPage } from "./pages/user/WorkPostsPage";
import { FestivalsPage } from "./pages/user/FestivalsPage";
import { AdminLayout } from "./pages/admin/AdminLayout";
import { AdminHome } from "./pages/admin/AdminHome";
import { AdminRequestsPage } from "./pages/admin/AdminRequestsPage";
import { AdminVolunteerPage } from "./pages/admin/AdminVolunteerPage";
import { AdminFestivalsPage } from "./pages/admin/AdminFestivalsPage";
import { AdminDonationClaimsPage } from "./pages/admin/AdminDonationClaimsPage";

function HomeRedirect() {
  const { auth } = useAuth();
  if (!auth) return <Navigate to="/login" replace />;
  if (auth.role === "ADMIN") return <Navigate to="/guthi" replace />;
  return <Navigate to="/u" replace />;
}

/** Old bookmarks: /admin/... → /guthi/... */
function LegacyAdminRedirect() {
  const loc = useLocation();
  const suffix = loc.pathname.replace(/^\/admin/, "") || "";
  return <Navigate to={`/guthi${suffix}${loc.search}${loc.hash}`} replace />;
}

export default function App() {
  return (
    <Routes>
      <Route path="/" element={<HomeRedirect />} />
      <Route path="/login/admin" element={<Navigate to="/login/guthi" replace />} />
      <Route path="/admin/*" element={<LegacyAdminRedirect />} />
      <Route path="/login" element={<AuthLayout><LoginGate /></AuthLayout>} />
      <Route path="/login/user" element={<AuthLayout><LoginPage portal="user" /></AuthLayout>} />
      <Route path="/login/guthi" element={<AuthLayout><LoginPage portal="guthi" /></AuthLayout>} />
      <Route path="/register" element={<Navigate to="/register/community" replace />} />
      <Route path="/register/community" element={<AuthLayout><RegisterCommunityPage /></AuthLayout>} />
      <Route path="/register/guthi" element={<AuthLayout><RegisterGuthiMemberPage /></AuthLayout>} />

      <Route
        path="/u"
        element={
          <ProtectedRoute role="USER">
            <UserLayout />
          </ProtectedRoute>
        }
      >
        <Route index element={<UserHome />} />
        <Route path="donations" element={<DonationsPage />} />
        <Route path="requests" element={<RequestsPage />} />
        <Route path="volunteer" element={<VolunteerPage />} />
        <Route path="work" element={<WorkPostsPage />} />
        <Route path="festivals" element={<FestivalsPage />} />
      </Route>

      <Route
        path="/guthi"
        element={
          <ProtectedRoute role="ADMIN">
            <AdminLayout />
          </ProtectedRoute>
        }
      >
        <Route index element={<AdminHome />} />
        <Route path="requests" element={<AdminRequestsPage />} />
        <Route path="volunteer" element={<AdminVolunteerPage />} />
        <Route path="festivals" element={<AdminFestivalsPage />} />
        <Route path="donations" element={<AdminDonationClaimsPage />} />
      </Route>

      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}
