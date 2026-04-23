import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { apiJson } from "../../api/client";
import { useAuth } from "../../auth/AuthContext";
import {
  mergePersonalIntoSummary,
  mergePoolIntoSummary,
  type PersonalPayload,
  type PoolPayload,
  type Summary,
} from "../../live/donationSummaryMerge";
import { LIVE_TOPICS } from "../../live/topics";
import { useStompSubscription } from "../../live/StompContext";

function formatInr(n: number) {
  return Number(n).toLocaleString("en-IN", { maximumFractionDigits: 0 });
}

export function UserHome() {
  const { auth } = useAuth();
  const [summary, setSummary] = useState<Summary | null>(null);
  const [err, setErr] = useState("");

  useEffect(() => {
    let cancelled = false;
    setErr("");
    apiJson<Summary>("/api/donations/summary")
      .then((s) => {
        if (!cancelled) setSummary(s);
      })
      .catch((e) => {
        if (!cancelled) setErr(e instanceof Error ? e.message : "Failed to load summary");
      });
    return () => {
      cancelled = true;
    };
  }, [auth?.token]);

  useStompSubscription(LIVE_TOPICS.donationsPool, (body) => {
    try {
      const pool = JSON.parse(body) as PoolPayload;
      setSummary((prev) => mergePoolIntoSummary(prev, pool));
    } catch {
      /* ignore */
    }
  });

  useStompSubscription(auth?.token ? LIVE_TOPICS.donationsPersonalQueue : null, (body) => {
    try {
      const p = JSON.parse(body) as PersonalPayload;
      setSummary((prev) => mergePersonalIntoSummary(prev, p));
    } catch {
      /* ignore */
    }
  });

  return (
    <div className="page page--home">
      <div className="home-hero">
        <div className="home-hero__left">
          <div className="home-hero__box">
            <header className="page__header">
              <h1 className="page__title">Welcome</h1>
              <p className="page__lede">
                Use the navigation to donate, submit community requests, volunteer, browse work posts, and review
                festivals.
              </p>
            </header>
            {err ? <p className="error">{err}</p> : null}
            {summary ? (
              <>
                <h2 className="home-hero__pool-title">Community donation pool</h2>
                <p className="empty-hint" style={{ marginTop: 0, marginBottom: "1rem" }}>
                  <strong>Available pool</strong> is shared by everyone (verified donations minus approved allocations
                  across all accounts). <strong>Your donations</strong>, <strong>Awaiting verification</strong>, and{" "}
                  <strong>You used from pool</strong> are only for this login — submitted on the Donations page and
                  credited after a Guthi admin approves your UPI claim or fund request.
                </p>
                <div className="stat-grid">
                  <div className="stat-card stat-card--gold">
                    <span className="stat-card__label">Available pool</span>
                    <span className="stat-card__value">₹{formatInr(Number(summary.availablePool))}</span>
                  </div>
                  <div className="stat-card">
                    <span className="stat-card__label">Your donations (verified)</span>
                    <span className="stat-card__value">
                      ₹{formatInr(Number(summary.myContributionsTotal ?? 0))}
                    </span>
                  </div>
                  <div className="stat-card">
                    <span className="stat-card__label">Awaiting verification</span>
                    <span className="stat-card__value">
                      ₹{formatInr(Number(summary.myPendingClaimsTotal ?? 0))}
                    </span>
                  </div>
                  <div className="stat-card">
                    <span className="stat-card__label">You used from pool (approved)</span>
                    <span className="stat-card__value">
                      ₹{formatInr(Number(summary.myUsedFromPoolTotal ?? 0))}
                    </span>
                  </div>
                </div>
                <p style={{ marginTop: "1.1rem", marginBottom: 0 }}>
                  <Link to="/u/donations">Make a donation</Link>
                </p>
              </>
            ) : null}
          </div>
        </div>
        <div className="home-hero__right">
          <img src="/buddha.jpg" alt="Buddha" className="home-hero__img" />
          <div className="home-hero__img-fade" />
        </div>
      </div>
    </div>
  );
}
