import { useEffect, useRef, useState } from "react";
import { apiJson } from "../../api/client";
import { LIVE_TOPICS } from "../../live/topics";
import { useStompSubscription } from "../../live/StompContext";

type Festival = {
  id: string;
  name: string;
  description: string | null;
  festivalDate: string;
  completed: boolean;
};

type Review = {
  id: string;
  reviewerName: string;
  rating: number;
  comment: string | null;
  createdAt: string;
};

function formatFestivalDate(iso: string) {
  try {
    return new Date(`${iso}T12:00:00`).toLocaleDateString(undefined, { dateStyle: "long" });
  } catch {
    return iso;
  }
}

export function AdminFestivalsPage() {
  const [festivals, setFestivals] = useState<Festival[]>([]);
  const [selectedId, setSelectedId] = useState<string | null>(null);
  const [reviews, setReviews] = useState<Review[]>([]);
  const [name, setName] = useState("");
  const [description, setDescription] = useState("");
  const [festivalDate, setFestivalDate] = useState("");
  const [err, setErr] = useState("");
  const [msg, setMsg] = useState("");
  const selectedRef = useRef<string | null>(null);
  selectedRef.current = selectedId;

  async function load() {
    const data = await apiJson<Festival[]>("/api/festivals");
    setFestivals(data);
  }

  useEffect(() => {
    load().catch((e) => setErr(e instanceof Error ? e.message : "Load failed"));
  }, []);

  useStompSubscription(LIVE_TOPICS.festivals, (body) => {
    try {
      setFestivals(JSON.parse(body) as Festival[]);
    } catch {
      /* ignore */
    }
  });

  useStompSubscription(LIVE_TOPICS.festivalReviews, (body) => {
    try {
      const push = JSON.parse(body) as { festivalId: string; reviews: Review[] };
      if (push.festivalId === selectedRef.current) {
        setReviews(push.reviews);
      }
    } catch {
      /* ignore */
    }
  });

  useEffect(() => {
    if (selectedId == null) {
      setReviews([]);
      return;
    }
    apiJson<Review[]>(`/api/festivals/${selectedId}/reviews`)
      .then(setReviews)
      .catch(() => setReviews([]));
  }, [selectedId]);

  async function create(e: React.FormEvent) {
    e.preventDefault();
    setErr("");
    setMsg("");
    if (!festivalDate) {
      setErr("Pick a festival date.");
      return;
    }
    try {
      await apiJson("/api/festivals", {
        method: "POST",
        body: JSON.stringify({
          name,
          description: description || undefined,
          festivalDate,
        }),
      });
      setMsg("Festival created. Mark it complete after the event so members can leave reviews.");
      setName("");
      setDescription("");
      setFestivalDate("");
      await load();
    } catch (e) {
      setErr(e instanceof Error ? e.message : "Create failed");
    }
  }

  async function markComplete(id: string) {
    setErr("");
    setMsg("");
    try {
      await apiJson(`/api/admin/festivals/${id}/complete`, { method: "PUT" });
      setMsg("Marked complete — community members can now rate this festival.");
      await load();
      if (selectedId === id) {
        const r = await apiJson<Review[]>(`/api/festivals/${id}/reviews`);
        setReviews(r);
      }
    } catch (e) {
      setErr(e instanceof Error ? e.message : "Could not mark complete");
    }
  }

  const selected = festivals.find((f) => f.id === selectedId) ?? null;

  return (
    <div className="page">
      <header className="page__header">
        <h1 className="page__title">Festivals</h1>
        <p className="page__lede">
          Create festivals for the community calendar. After the event, mark one as complete so members can review and
          rate it. Everyone can read reviews once they are posted.
        </p>
      </header>
      {err ? <p className="error">{err}</p> : null}
      {msg ? <p className="success">{msg}</p> : null}

      <div className="festivals-split">
        <div className="panel">
          <h2 className="panel__title">Create festival</h2>
          <form className="form-stack" onSubmit={create}>
            <label htmlFor="adm-fest-name">Name</label>
            <input id="adm-fest-name" value={name} onChange={(e) => setName(e.target.value)} required />
            <label htmlFor="adm-fest-desc">Description</label>
            <textarea id="adm-fest-desc" rows={4} value={description} onChange={(e) => setDescription(e.target.value)} />
            <label htmlFor="adm-fest-date">Festival date</label>
            <input
              id="adm-fest-date"
              type="date"
              value={festivalDate}
              onChange={(e) => setFestivalDate(e.target.value)}
              required
            />
            <button type="submit">Publish festival</button>
          </form>

          <h3 className="panel__subtitle" style={{ marginTop: "1.5rem" }}>
            All festivals (newest first)
          </h3>
          <p className="empty-hint" style={{ marginTop: 0 }}>
            Select a row to see full details and community reviews on the right.
          </p>
          <ul className="festival-pick-list">
            {festivals.map((f) => (
              <li key={f.id} className="festival-pick-item festival-pick-item--admin">
                <button
                  type="button"
                  className={`festival-pick-btn${selectedId === f.id ? "" : " secondary"}`}
                  onClick={() => setSelectedId(f.id)}
                >
                  <span className="festival-pick-btn__title">{f.name}</span>
                  <span className="festival-pick-btn__meta">{formatFestivalDate(f.festivalDate)}</span>
                </button>
                {f.completed ? (
                  <span className="chip chip--approved">Complete</span>
                ) : (
                  <button type="button" className="secondary festival-complete-btn" onClick={() => void markComplete(f.id)}>
                    Mark complete
                  </button>
                )}
              </li>
            ))}
          </ul>
        </div>

        <div className="panel panel--wide">
          {selected == null ? (
            <p className="empty-hint">Choose a festival from the list to see its description and reviews.</p>
          ) : (
            <>
              <h2 className="panel__title">{selected.name}</h2>
              <p className="festival-detail-meta">
                <strong>Date:</strong> {formatFestivalDate(selected.festivalDate)}
                {selected.completed ? (
                  <span className="chip chip--approved" style={{ marginLeft: "0.5rem" }}>
                    Complete — reviews open
                  </span>
                ) : (
                  <span className="chip" style={{ marginLeft: "0.5rem" }}>
                    Scheduled — reviews locked
                  </span>
                )}
              </p>
              <div className="festival-detail-body">
                {selected.description?.trim() ? (
                  <p style={{ whiteSpace: "pre-wrap", margin: "0.75rem 0 0" }}>{selected.description}</p>
                ) : (
                  <p className="empty-hint" style={{ marginTop: "0.75rem" }}>
                    No description was added for this festival.
                  </p>
                )}
              </div>

              <h3 style={{ marginTop: "1.35rem", marginBottom: "0.5rem" }}>Community reviews</h3>
              <p className="empty-hint" style={{ marginTop: 0 }}>
                Ratings appear here after the festival is marked complete and members submit reviews.
              </p>
              {reviews.length === 0 ? (
                <p className="empty-hint" style={{ marginTop: "0.5rem" }}>
                  No reviews yet.
                </p>
              ) : (
                <ul className="review-list">
                  {reviews.map((r) => (
                    <li key={r.id}>
                      <strong>{r.reviewerName}</strong> — {r.rating}/5
                      {r.comment ? <span className="review-comment">: {r.comment}</span> : null}
                      <span className="review-date"> · {new Date(r.createdAt).toLocaleString()}</span>
                    </li>
                  ))}
                </ul>
              )}
            </>
          )}
        </div>
      </div>
    </div>
  );
}
