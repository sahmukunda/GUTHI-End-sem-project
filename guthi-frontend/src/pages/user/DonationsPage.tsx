import { useEffect, useState, useCallback } from "react";
import { apiJson, loadAuth } from "../../api/client";
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

/** Preset amounts → PNG in `guthi/qra/` (served as `/qra/…`). */
const DONATION_PRESETS = [
  { rupees: 50, label: "₹50", file: "50.png", hint: "Quick" },
  { rupees: 1000, label: "₹1k", file: "1.png", hint: "1,000" },
  { rupees: 2000, label: "₹2k", file: "2.png", hint: "2,000" },
  { rupees: 3000, label: "₹3k", file: "3.png", hint: "3,000" },
  { rupees: 4000, label: "₹4k", file: "4.png", hint: "4,000" },
  { rupees: 5000, label: "₹5k", file: "5.png", hint: "5,000" },
] as const;

function formatInr(n: number) {
  return Number(n).toLocaleString("en-IN", { maximumFractionDigits: 0 });
}

export function DonationsPage() {
  const { auth } = useAuth();
  const [summary, setSummary] = useState<Summary | null>(null);
  const [presetFile, setPresetFile] = useState<string>(DONATION_PRESETS[1].file);
  const [note, setNote] = useState("");
  const [err, setErr] = useState("");
  const [overlayOpen, setOverlayOpen] = useState(false);
  const [overlayStep, setOverlayStep] = useState<"scan" | "txn">("scan");
  const [overlayFile, setOverlayFile] = useState<string | null>(null);
  const [overlayLabel, setOverlayLabel] = useState("");
  const [pendingRupees, setPendingRupees] = useState<number | null>(null);
  const [upiTxnId, setUpiTxnId] = useState("");
  const [txnFieldErr, setTxnFieldErr] = useState("");
  const [imgBroken, setImgBroken] = useState(false);
  const [celebrate, setCelebrate] = useState(false);
  const [recording, setRecording] = useState(false);

  const selectedPreset = DONATION_PRESETS.find((p) => p.file === presetFile) ?? DONATION_PRESETS[1];

  useEffect(() => {
    let cancelled = false;
    apiJson<Summary>("/api/donations/summary")
      .then((s) => {
        if (!cancelled) setSummary(s);
      })
      .catch((e) => {
        if (!cancelled) setErr(e instanceof Error ? e.message : "Load failed");
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
      /* ignore malformed */
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

  const closeOverlay = useCallback(() => {
    setOverlayOpen(false);
    setOverlayStep("scan");
    setOverlayFile(null);
    setPendingRupees(null);
    setUpiTxnId("");
    setTxnFieldErr("");
    setImgBroken(false);
  }, []);

  useEffect(() => {
    if (!overlayOpen) return;
    const onKey = (e: KeyboardEvent) => {
      if (e.key === "Escape") closeOverlay();
    };
    window.addEventListener("keydown", onKey);
    return () => window.removeEventListener("keydown", onKey);
  }, [overlayOpen, closeOverlay]);

  function openPayOverlay(e: React.FormEvent) {
    e.preventDefault();
    setErr("");
    const preset = DONATION_PRESETS.find((p) => p.file === presetFile) ?? DONATION_PRESETS[0];
    setOverlayStep("scan");
    setUpiTxnId("");
    setTxnFieldErr("");
    setOverlayFile(preset.file);
    setOverlayLabel(preset.label);
    setPendingRupees(preset.rupees);
    setImgBroken(false);
    setOverlayOpen(true);
  }

  function goToTxnStep() {
    if (!loadAuth()?.token) {
      setErr("You are not signed in. Open community sign-in and try again.");
      closeOverlay();
      return;
    }
    setErr("");
    setTxnFieldErr("");
    setOverlayStep("txn");
    setUpiTxnId("");
  }

  const digitsOnly = upiTxnId.replace(/\D/g, "");
  const txnIdValid = digitsOnly.length === 12;

  async function handleSubmitClaim() {
    if (pendingRupees == null) return;
    setTxnFieldErr("");
    setErr("");
    const raw = upiTxnId.replace(/\D/g, "");
    if (!loadAuth()?.token) {
      setTxnFieldErr("You are not signed in. Use community sign-in, then try again.");
      return;
    }
    if (raw.length < 12) {
      setTxnFieldErr(`Transaction ID must be exactly 12 digits (${12 - raw.length} more needed).`);
      return;
    }
    if (raw.length > 12) {
      setTxnFieldErr(`Transaction ID must be exactly 12 digits (you entered ${raw.length}).`);
      return;
    }
    setRecording(true);
    try {
      await apiJson("/api/donations/claims", {
        method: "POST",
        body: JSON.stringify({
          amount: pendingRupees,
          note: note.trim() || undefined,
          upiTransactionId: raw,
        }),
      });
      const s = await apiJson<Summary>("/api/donations/summary");
      setSummary(s);
      closeOverlay();
      setCelebrate(true);
      window.setTimeout(() => setCelebrate(false), 3200);
    } catch (e) {
      const msg = e instanceof Error ? e.message : "Could not submit payment details";
      setTxnFieldErr(msg);
      setErr(msg);
    } finally {
      setRecording(false);
    }
  }

  const qrSrc = overlayFile ? `/qra/${overlayFile}?t=${overlayFile}` : "";

  return (
    <div className="page">
      <div className="donations-page">
      <header className="donations-hero">
        <p className="donations-hero__eyebrow">Community treasury</p>
        <h1 className="donations-hero__title">Donations</h1>
        <p className="donations-hero__sub">
          Support the Guthi pool with a preset amount. Scan the QR, pay via UPI, then enter your 12-digit transaction ID
          so a Guthi admin can verify and credit the pool.
        </p>
      </header>

      {celebrate ? (
        <div className="donation-celebrate" aria-hidden>
          <div className="donation-celebrate__burst" />
          <p className="donation-celebrate__msg">Thank you!</p>
          <p className="donation-celebrate__sub">
            Your claim is pending admin verification — see “Awaiting verification” in the pool summary above. Once an
            admin approves it, the amount joins the shared pool and your donation total.
          </p>
          {Array.from({ length: 22 }).map((_, i) => (
            <span
              key={i}
              className="donation-celebrate__confetti"
              style={{
                left: `${4 + (i * 41) % 92}%`,
                animationDelay: `${i * 0.04}s`,
                background: ["#c4a35a", "#81c784", "#64b5f6", "#e57373", "#ba68c8", "#ffd54f"][i % 6],
              }}
            />
          ))}
        </div>
      ) : null}

      {overlayOpen ? (
        <div
          className="donation-overlay"
          role="dialog"
          aria-modal="true"
          aria-labelledby="donation-overlay-title"
        >
          <div className="donation-overlay__backdrop" onClick={closeOverlay} aria-hidden />
          <div className={`donation-overlay__panel donation-overlay__panel--step-${overlayStep}`}>
            <div className="donation-overlay__accent-bar" aria-hidden />
            <div className="donation-overlay__icon" aria-hidden>
              <svg width="36" height="36" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5">
                <rect x="3" y="3" width="7" height="7" rx="1" />
                <rect x="14" y="3" width="7" height="7" rx="1" />
                <rect x="3" y="14" width="7" height="7" rx="1" />
                <rect x="14" y="14" width="3" height="3" />
                <rect x="18" y="14" width="3" height="3" />
                <rect x="14" y="18" width="3" height="3" />
                <rect x="18" y="18" width="3" height="3" />
              </svg>
            </div>
            {overlayStep === "scan" ? (
              <div className="donation-overlay__step" key="scan">
                <h2 id="donation-overlay-title" className="donation-overlay__title">
                  Scan to pay
                </h2>
                <p className="donation-overlay__amount-line">
                  <span className="donation-overlay__badge">{overlayLabel}</span>
                  <span className="donation-overlay__hint">Open PhonePe, GPay, or any UPI app</span>
                </p>
                {note.trim() ? (
                  <p className="donation-overlay__note">
                    <span className="donation-overlay__note-label">Your note</span>
                    {note.trim()}
                  </p>
                ) : null}
                <div className="donation-overlay__qr-wrap">
                  {imgBroken ? (
                    <p className="error donations-overlay-error">
                      Missing <code>/qra/{overlayFile}</code>. Add PNGs under <code>guthi/qra/</code> (see README) and
                      restart the API.
                    </p>
                  ) : (
                    <img
                      src={qrSrc}
                      alt={`Payment QR for ${overlayLabel}`}
                      className="donation-overlay__qr"
                      onError={() => setImgBroken(true)}
                    />
                  )}
                </div>
                <p className="donation-overlay__steps">
                  <span>1</span> Scan & pay <span className="donation-overlay__steps-dot">·</span> <span>2</span> Tap done
                  below
                </p>
                <div className="donation-overlay__actions">
                  <button type="button" className="secondary" onClick={closeOverlay}>
                    Cancel
                  </button>
                  <button type="button" className="donations-btn-primary" onClick={goToTxnStep}>
                    Done — I paid
                  </button>
                </div>
              </div>
            ) : (
              <div className="donation-overlay__step" key="txn">
                <h2 id="donation-overlay-title" className="donation-overlay__title">
                  UPI transaction ID
                </h2>
                <p className="donation-overlay__amount-line">
                  <span className="donation-overlay__badge">{overlayLabel}</span>
                  <span className="donation-overlay__hint">Enter the 12-digit reference from your UPI app</span>
                </p>
                {note.trim() ? (
                  <p className="donation-overlay__note">
                    <span className="donation-overlay__note-label">Your note</span>
                    {note.trim()}
                  </p>
                ) : null}
                {txnFieldErr ? (
                  <p className="error donation-overlay__txn-error" role="alert">
                    {txnFieldErr}
                  </p>
                ) : null}
                <div className="donation-overlay__txn-field">
                  <label htmlFor="donation-upi-txn">Transaction ID</label>
                  <input
                    id="donation-upi-txn"
                    type="text"
                    inputMode="numeric"
                    autoComplete="off"
                    placeholder="000000000000"
                    value={upiTxnId}
                    onChange={(e) => {
                      setUpiTxnId(e.target.value.replace(/\D/g, "").slice(0, 20));
                      setTxnFieldErr("");
                    }}
                    aria-invalid={digitsOnly.length > 0 && !txnIdValid}
                  />
                  <p className="donation-overlay__txn-hint">
                    {digitsOnly.length === 0
                      ? "Enter exactly 12 digits from your UPI or bank app."
                      : txnIdValid
                        ? "Looks good — tap Submit to send for verification."
                        : digitsOnly.length < 12
                          ? `${12 - digitsOnly.length} more digit${12 - digitsOnly.length === 1 ? "" : "s"} needed`
                          : `${digitsOnly.length} digits — use only the 12-digit reference (remove extras).`}
                  </p>
                </div>
                <div className="donation-overlay__actions">
                  <button type="button" className="secondary" onClick={() => setOverlayStep("scan")}>
                    Back
                  </button>
                  <button type="button" className="secondary" onClick={closeOverlay}>
                    Cancel
                  </button>
                  <button
                    type="button"
                    className="donations-btn-primary"
                    disabled={recording}
                    onClick={() => void handleSubmitClaim()}
                  >
                    {recording ? "Submitting…" : "Submit"}
                  </button>
                </div>
              </div>
            )}
          </div>
        </div>
      ) : null}

      <div className="donations-grid">
        <section className="card donations-card donations-card--pool">
          <div className="donations-card__head">
            <h2>Pool summary</h2>
            <span className="donations-card__tag">Live</span>
          </div>
          {err && !summary ? <p className="error">{err}</p> : null}
          {err && summary ? <p className="error donations-inline-alert">{err}</p> : null}
          {summary ? (
            <>
              <p className="empty-hint" style={{ margin: "0 0 0.85rem", fontSize: "0.88rem", maxWidth: "40rem" }}>
                <strong>Available</strong> is the shared community pool after verified donations and allocations.{" "}
                <strong>Your donations</strong> counts amounts a Guthi admin has approved;{" "}
                <strong>Awaiting verification</strong> is UPI claims still pending review.
              </p>
              <div className="donations-stat-grid">
                <div className="donations-stat donations-stat--green">
                  <span className="donations-stat__label">Available</span>
                  <span className="donations-stat__value">₹{formatInr(Number(summary.availablePool))}</span>
                </div>
                <div className="donations-stat">
                  <span className="donations-stat__label">Your donations (verified)</span>
                  <span className="donations-stat__value">
                    ₹{formatInr(Number(summary.myContributionsTotal ?? 0))}
                  </span>
                </div>
                <div className="donations-stat">
                  <span className="donations-stat__label">Awaiting verification</span>
                  <span className="donations-stat__value">
                    ₹{formatInr(Number(summary.myPendingClaimsTotal ?? 0))}
                  </span>
                </div>
                <div className="donations-stat">
                  <span className="donations-stat__label">You used from pool (approved)</span>
                  <span className="donations-stat__value">
                    ₹{formatInr(Number(summary.myUsedFromPoolTotal ?? 0))}
                  </span>
                </div>
              </div>
              <h3 className="donations-subheading">Allocations</h3>
              <div className="donations-table-wrap">
                <table className="donations-table">
                  <thead>
                    <tr>
                      <th>Purpose</th>
                      <th>Amount</th>
                      <th>Date</th>
                    </tr>
                  </thead>
                  <tbody>
                    {summary.allocations.length === 0 ? (
                      <tr>
                        <td colSpan={3} className="donations-table__empty">
                          No allocations yet — approved requests will appear here.
                        </td>
                      </tr>
                    ) : (
                      summary.allocations.map((a, i) => (
                        <tr key={i}>
                          <td>{a.purpose}</td>
                          <td>₹{formatInr(Number(a.amountUsed))}</td>
                          <td className="donations-table__date">{a.date}</td>
                        </tr>
                      ))
                    )}
                  </tbody>
                </table>
              </div>
            </>
          ) : null}
        </section>

        <aside className="card donations-card donations-card--aside">
          <h2>How it works</h2>
          <ol className="donations-steps">
            <li>
              <span className="donations-steps__num">1</span>
              <span>Pick an amount and optional note.</span>
            </li>
            <li>
              <span className="donations-steps__num">2</span>
              <span>
                Tap <strong>Donate</strong> — your QR opens in a focused window.
              </span>
            </li>
            <li>
              <span className="donations-steps__num">3</span>
              <span>
                After UPI succeeds, tap <strong>Done — I paid</strong>, then enter the <strong>12-digit transaction ID</strong>{" "}
                from your bank or UPI app.
              </span>
            </li>
            <li>
              <span className="donations-steps__num">4</span>
              <span>
                A Guthi admin verifies the ID; once approved, your amount joins the pool and your verified donation total.
              </span>
            </li>
          </ol>
        </aside>
      </div>

      <section className="card donations-card donations-card--form">
        <div className="donations-card__head">
          <h2>Choose amount</h2>
          <span className="donations-card__tag donations-card__tag--muted">UPI</span>
        </div>
        <p className="donations-form-intro">Select a preset — each opens the matching QR for that tier.</p>

        <form className="donations-form" onSubmit={openPayOverlay}>
          <fieldset className="donations-fieldset">
            <legend className="donations-legend">Amount</legend>
            <div className="donations-pills" role="group" aria-label="Donation amount">
              {DONATION_PRESETS.map((p) => (
                <button
                  key={p.file}
                  type="button"
                  className={`donations-pill ${presetFile === p.file ? "donations-pill--active" : ""}`}
                  onClick={() => setPresetFile(p.file)}
                  aria-pressed={presetFile === p.file}
                >
                  <span className="donations-pill__main">{p.label}</span>
                  <span className="donations-pill__sub">{p.hint}</span>
                </button>
              ))}
            </div>
          </fieldset>

          <div className="donations-field">
            <label htmlFor="donation-note">Note (optional)</label>
            <input
              id="donation-note"
              value={note}
              onChange={(e) => setNote(e.target.value)}
              placeholder="e.g. Festival contribution, ward name…"
              autoComplete="off"
            />
          </div>

          <div className="donations-form-footer">
            <p className="donations-selection-hint">
              Paying <strong>₹{formatInr(selectedPreset.rupees)}</strong> · {selectedPreset.hint} INR
            </p>
            <button type="submit" className="donations-btn-primary donations-btn-primary--large">
              <span className="donations-btn-primary__icon" aria-hidden>
                ◆
              </span>
              Donate
            </button>
          </div>
        </form>
      </section>
      </div>
    </div>
  );
}
