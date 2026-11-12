import { useEffect, useRef, useState } from "react";
import { Link } from "react-router-dom";
import { apiJson } from "../../api/client";
import { useAuth } from "../../auth/AuthContext";
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

export function FestivalsPage() {
  const { auth } = useAuth();
  const [festivals, setFestivals] = useState<Festival[]>([]);
  const [q, setQ] = useState("");
  const [selected, setSelected] = useState<string | null>(null);
  const [reviews, setReviews] = useState<Review[]>([]);
  const [rating, setRating] = useState(5);
  const [comment, setComment] = useState("");
  const [err, setErr] = useState("");
  const [msg, setMsg] = useState("");
  const selectedRef = useRef<string | null>(null);
  selectedRef.current = selected;

  useEffect(() => {
    apiJson<Festival[]>("/api/festivals")
      .then(setFestivals)
      .catch((e) => setErr(e instanceof Error ? e.message : "Load failed"));
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
    if (selected == null) {
      setReviews([]);
      return;
    }
    apiJson<Review[]>(`/api/festivals/${selected}/reviews`)
      .then(setReviews)
      .catch(() => setReviews([]));
  }, [selected]);

  const selectedFestival = festivals.find((f) => f.id === selected) ?? null;

  async function submitReview(e: React.FormEvent) {
    e.preventDefault();
    if (selected == null) return;
    setErr("");
    setMsg("");
    try {
      await apiJson(`/api/festivals/${selected}/reviews`, {
        method: "POST",
        body: JSON.stringify({ rating, comment: comment || undefined }),
      });
      setMsg("Review submitted — thanks for sharing your experience.");
      setComment("");
      const r = await apiJson<Review[]>(`/api/festivals/${selected}/reviews`);
      setReviews(r);
    } catch (e) {
      setErr(e instanceof Error ? e.message : "Review failed");
    }
  }

  return (
    <div className="page">
      <header className="page__header">
        <h1 className="page__title">Festivals</h1>
        <p className="page__lede">
          Browse festivals published by the Guthi. Read what others thought; after an event is marked complete, you can
          add your own rating.
        </p>
      </header>
      {err ? <p className="error">{err}</p> : null}
      {msg ? <p className="success">{msg}</p> : null}
      <div className="festivals-split">
        <div className="panel">
          <h2 className="panel__title">Calendar</h2>
          <label htmlFor="fest-search">Search festivals</label>
          <input
            id="fest-search"
            value={q}
            onChange={(e) => setQ(e.target.value)}
            placeholder="Name or description"
          />
          <ul className="festival-pick-list">
            {festivals
              .filter((f) => {
                const hay = `${f.name} ${f.description ?? ""}`.toLowerCase();
                return hay.includes(q.trim().toLowerCase());
              })
              .map((f) => (
              <li key={f.id} className="festival-pick-item">
                <button
                  type="button"
                  className={`festival-pick-btn${selected === f.id ? "" : " secondary"}`}
                  onClick={() => setSelected(f.id)}
                >
                  <span className="festival-pick-btn__title">{f.name}</span>
                  <span className="festival-pick-btn__meta">{formatFestivalDate(f.festivalDate)}</span>
                </button>
                {f.completed ? <span className="chip chip--approved">Complete</span> : <span className="chip">Scheduled</span>}
              </li>
            ))}
          </ul>
        </div>
        <div className="panel panel--wide">
          {selected == null ? (
            <p className="empty-hint">Select a festival to read its details and community reviews.</p>
          ) : selectedFestival == null ? (
            <p className="empty-hint">This festival is no longer in the list.</p>
          ) : (
            <>
              <h2 className="panel__title">{selectedFestival.name}</h2>
              <p className="festival-detail-meta">
                <strong>Date:</strong> {formatFestivalDate(selectedFestival.festivalDate)}
                {selectedFestival.completed ? (
                  <span className="chip chip--approved" style={{ marginLeft: "0.5rem" }}>
                    Complete
                  </span>
                ) : (
                  <span className="chip" style={{ marginLeft: "0.5rem" }}>
                    Scheduled
                  </span>
                )}
              </p>
              <div className="festival-detail-body">
                {selectedFestival.description?.trim() ? (
                  <p style={{ whiteSpace: "pre-wrap", margin: "0.75rem 0 0" }}>{selectedFestival.description}</p>
                ) : (
                  <p className="empty-hint" style={{ marginTop: "0.75rem" }}>
                    No extra description for this festival.
                  </p>
                )}
              </div>

              <h3 style={{ marginTop: "1.35rem", marginBottom: "0.5rem" }}>Reviews</h3>
              {!selectedFestival.completed ? (
                <p className="empty-hint" style={{ marginTop: 0 }}>
                  Ratings open only after a Guthi admin marks this festival as complete. You can read past reviews below
                  once they exist.
                </p>
              ) : null}

              {reviews.length === 0 ? (
                <p className="empty-hint" style={{ marginTop: "0.5rem" }}>
                  {selectedFestival.completed ? "No reviews yet — be the first to share yours." : "No reviews yet."}
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

              {selectedFestival.completed ? (
                auth?.token ? (
                  <>
                    <h3 style={{ marginTop: "1.25rem" }}>Your review</h3>
                    <form className="form-stack" onSubmit={submitReview}>
                      <label htmlFor="fest-rating">Rating (1–5)</label>
                      <input
                        id="fest-rating"
                        type="number"
                        min={1}
                        max={5}
                        value={rating}
                        onChange={(e) => setRating(Number(e.target.value))}
                      />
                      <label htmlFor="fest-comment">Comment (optional)</label>
                      <textarea id="fest-comment" rows={3} value={comment} onChange={(e) => setComment(e.target.value)} />
                      <button type="submit">Submit review</button>
                    </form>
                  </>
                ) : (
                  <p className="empty-hint" style={{ marginTop: "1.25rem" }}>
                    <Link to="/login/user">Sign in</Link> with your community account to leave a rating and comment.
                  </p>
                )
              ) : null}
            </>
          )}
        </div>
      </div>
    </div>
  );
}
