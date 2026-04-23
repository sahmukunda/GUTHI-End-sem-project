import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { apiJson } from "../../api/client";
import { LIVE_TOPICS } from "../../live/topics";
import { useStompSubscription } from "../../live/StompContext";
import type { RequestStatus } from "../../types";

type Me = { fullName: string; email: string; role: string };

type RequestRow = { status: RequestStatus };
type ClaimRow = { status: string };
type VolunteerEventRow = { active: boolean };
type FestivalRow = { id: string };

export function AdminHome() {
  const [me, setMe] = useState<Me | null>(null);
  const [err, setErr] = useState("");
  const [overviewErr, setOverviewErr] = useState("");
  const [requests, setRequests] = useState<RequestRow[] | null>(null);
  const [claims, setClaims] = useState<ClaimRow[] | null>(null);
  const [events, setEvents] = useState<VolunteerEventRow[] | null>(null);
  const [festivals, setFestivals] = useState<FestivalRow[] | null>(null);

  useEffect(() => {
    apiJson<Me>("/api/admin/me")
      .then(setMe)
      .catch((e) => setErr(e instanceof Error ? e.message : "Failed to verify session"));
  }, []);

  useEffect(() => {
    let cancelled = false;
    setOverviewErr("");
    Promise.all([
      apiJson<RequestRow[]>("/api/requests/all"),
      apiJson<ClaimRow[]>("/api/admin/donation-claims"),
      apiJson<VolunteerEventRow[]>("/api/volunteer-events"),
      apiJson<FestivalRow[]>("/api/festivals"),
    ])
      .then(([r, c, e, f]) => {
        if (!cancelled) {
          setRequests(r);
          setClaims(c);
          setEvents(e);
          setFestivals(f);
        }
      })
      .catch((e) => {
        if (!cancelled) {
          setOverviewErr(e instanceof Error ? e.message : "Failed to load workspace overview");
        }
      });
    return () => {
      cancelled = true;
    };
  }, []);

  useStompSubscription(LIVE_TOPICS.adminRequests, (body) => {
    try {
      setRequests(JSON.parse(body) as RequestRow[]);
    } catch {
      /* ignore */
    }
  });

  useStompSubscription(LIVE_TOPICS.adminDonationClaims, (body) => {
    try {
      setClaims(JSON.parse(body) as ClaimRow[]);
    } catch {
      /* ignore */
    }
  });

  useStompSubscription(LIVE_TOPICS.volunteerEvents, (body) => {
    try {
      setEvents(JSON.parse(body) as VolunteerEventRow[]);
    } catch {
      /* ignore */
    }
  });

  useStompSubscription(LIVE_TOPICS.festivals, (body) => {
    try {
      setFestivals(JSON.parse(body) as FestivalRow[]);
    } catch {
      /* ignore */
    }
  });

  const pendingRequests = requests == null ? null : requests.filter((x) => x.status === "PENDING").length;
  const pendingClaims = claims == null ? null : claims.filter((x) => x.status === "PENDING").length;
  const activeEvents = events == null ? null : events.filter((x) => x.active).length;
  const festivalCount = festivals == null ? null : festivals.length;

  return (
    <div className="page page--home">
      <div className="home-hero">
        <div className="home-hero__left">
          <div className="home-hero__box">
            <header className="page__header">
              <h1 className="page__title">Guthi member workspace</h1>
              <p className="page__lede">
                Manage requests, volunteer programmes, and the festival calendar from here.
              </p>
            </header>
            {err ? <p className="error">{err}</p> : null}
            {me ? (
              <p className="empty-hint" style={{ marginTop: 0, marginBottom: "1rem" }}>
                Signed in as <strong style={{ color: "var(--text)" }}>{me.fullName}</strong> ({me.email}) — role{" "}
                <span className="chip">{me.role}</span>
              </p>
            ) : null}
            {overviewErr ? <p className="error">{overviewErr}</p> : null}
            {requests != null && claims != null && events != null && festivals != null ? (
              <>
                <h2 className="home-hero__pool-title">Live overview</h2>
                <p className="empty-hint" style={{ marginTop: 0, marginBottom: "0.75rem" }}>
                  These counts refresh over the WebSocket connection when anything changes elsewhere in the workspace.
                </p>
                <ul className="empty-hint" style={{ marginBottom: 0, paddingLeft: "1.25rem" }}>
                  <li>
                    <Link to="/guthi/requests">Pending community requests</Link>: <strong>{pendingRequests}</strong>
                  </li>
                  <li>
                    <Link to="/guthi/donations">Pending donation claims</Link>: <strong>{pendingClaims}</strong>
                  </li>
                  <li>
                    <Link to="/guthi/volunteer">Active volunteer events</Link>: <strong>{activeEvents}</strong>
                  </li>
                  <li>
                    <Link to="/guthi/festivals">Festivals on the calendar</Link>: <strong>{festivalCount}</strong>
                  </li>
                </ul>
              </>
            ) : null}
            <div className="dashboard-grid">
              <Link to="/guthi/requests" className="dashboard-tile">
                <h2>Community requests</h2>
                <p>Review pending fund and ritual requests.</p>
                <span className="choice-tile__cta">Open requests →</span>
              </Link>
              <Link to="/guthi/volunteer" className="dashboard-tile">
                <h2>Volunteer events</h2>
                <p>Create events and view signups.</p>
                <span className="choice-tile__cta">Manage volunteer →</span>
              </Link>
              <Link to="/guthi/festivals" className="dashboard-tile">
                <h2>Festivals</h2>
                <p>Add festivals for the community calendar.</p>
                <span className="choice-tile__cta">Manage festivals →</span>
              </Link>
            </div>
          </div>
        </div>
        <div className="home-hero__right">
          <img src="/buddha.jpg" alt="Buddha" className="home-hero__img" decoding="async" />
          <div className="home-hero__img-fade" />
        </div>
      </div>
    </div>
  );
}
