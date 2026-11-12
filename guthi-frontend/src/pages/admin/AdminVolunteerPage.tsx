import { useEffect, useRef, useState } from "react";
import { apiJson } from "../../api/client";
import { LIVE_TOPICS } from "../../live/topics";
import { useStompSubscription } from "../../live/StompContext";

type Event = {
  id: string;
  title: string;
  description: string | null;
  volunteersRequired: number;
  eventDate: string;
  active: boolean;
  currentSignups: number;
  currentUserSignedUp?: boolean;
  completed?: boolean;
  schedulePast?: boolean;
};

type SignupRow = {
  participantName: string;
  mobileNumber: string;
  signedUpAt: string;
};

type Signups = { count: number; signups: SignupRow[] };

/** Earliest allowed value for {@code datetime-local} (this minute, local). */
function minDateTimeLocalNow(): string {
  const d = new Date();
  d.setSeconds(0, 0);
  const pad = (n: number) => String(n).padStart(2, "0");
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}`;
}

/** Default picker: tomorrow 09:00 local. */
function defaultEventDateLocal(): string {
  const d = new Date();
  d.setDate(d.getDate() + 1);
  d.setHours(9, 0, 0, 0);
  const pad = (n: number) => String(n).padStart(2, "0");
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}`;
}

export function AdminVolunteerPage() {
  const [events, setEvents] = useState<Event[]>([]);
  const [title, setTitle] = useState("");
  const [description, setDescription] = useState("");
  const [volunteersRequired, setVolunteersRequired] = useState("10");
  const [eventDate, setEventDate] = useState(() => defaultEventDateLocal());
  const [detailId, setDetailId] = useState<string | null>(null);
  const [signups, setSignups] = useState<Signups | null>(null);
  const [err, setErr] = useState("");
  const [msg, setMsg] = useState("");
  const detailIdRef = useRef<string | null>(null);
  detailIdRef.current = detailId;

  async function loadEvents() {
    const data = await apiJson<Event[]>("/api/volunteer-events");
    setEvents(data);
  }

  useEffect(() => {
    loadEvents().catch((e) => setErr(e instanceof Error ? e.message : "Load failed"));
  }, []);

  useStompSubscription(LIVE_TOPICS.volunteerEvents, (body) => {
    try {
      setEvents(JSON.parse(body) as Event[]);
    } catch {
      /* ignore */
    }
  });

  useStompSubscription(LIVE_TOPICS.adminVolunteerSignups, (body) => {
    try {
      const push = JSON.parse(body) as { eventId: string; payload: Signups };
      if (push.eventId === detailIdRef.current) {
        setSignups(push.payload);
      }
    } catch {
      /* ignore */
    }
  });

  useEffect(() => {
    if (detailId == null) {
      setSignups(null);
      return;
    }
    apiJson<Signups>(`/api/volunteer-events/${detailId}/signups`)
      .then(setSignups)
      .catch((e) => setErr(e instanceof Error ? e.message : "Signups failed"));
  }, [detailId]);

  async function create(e: React.FormEvent) {
    e.preventDefault();
    setErr("");
    setMsg("");
    const chosen = new Date(eventDate);
    if (Number.isNaN(chosen.getTime()) || chosen.getTime() < Date.now()) {
      setErr("Pick a date and time that is today or in the future — not in the past.");
      return;
    }
    try {
      await apiJson("/api/volunteer-events", {
        method: "POST",
        body: JSON.stringify({
          title,
          description: description || undefined,
          volunteersRequired: Number(volunteersRequired),
          eventDate,
        }),
      });
      setMsg("Event created.");
      setTitle("");
      setDescription("");
      setEventDate(defaultEventDateLocal());
      await loadEvents();
    } catch (e) {
      setErr(e instanceof Error ? e.message : "Create failed");
    }
  }

  return (
    <div className="page">
      <header className="page__header">
        <h1 className="page__title">Volunteer events</h1>
        <p className="page__lede">Create shifts and inspect who signed up (name and mobile they provided).</p>
      </header>
      {err ? <p className="error">{err}</p> : null}
      {msg ? <p className="success">{msg}</p> : null}
      <div className="panel">
        <h2 className="panel__title">Create event</h2>
        <form className="form-stack" onSubmit={create}>
          <div className="field-row">
            <div>
              <label htmlFor="adm-vol-title">Title</label>
              <input id="adm-vol-title" value={title} onChange={(e) => setTitle(e.target.value)} required />
            </div>
            <div>
              <label htmlFor="adm-vol-req">Required volunteers</label>
              <input
                id="adm-vol-req"
                type="number"
                min={1}
                value={volunteersRequired}
                onChange={(e) => setVolunteersRequired(e.target.value)}
                required
              />
            </div>
            <div>
              <label htmlFor="adm-vol-when">Event date & time</label>
              <input
                id="adm-vol-when"
                type="datetime-local"
                min={minDateTimeLocalNow()}
                step={60}
                value={eventDate}
                onChange={(e) => setEventDate(e.target.value)}
                required
              />
              <p className="empty-hint" style={{ margin: "0.35rem 0 0", fontSize: "0.82rem" }}>
                Must be this minute or later (today or a future day). Defaults to tomorrow 9:00.
              </p>
            </div>
          </div>
          <label htmlFor="adm-vol-desc">Description</label>
          <textarea id="adm-vol-desc" rows={3} value={description} onChange={(e) => setDescription(e.target.value)} />
          <button type="submit">Create event</button>
        </form>
      </div>
      <h2 className="page__section-title">All events</h2>
      {events.map((ev) => {
        const open = detailId === ev.id;
        return (
          <div key={ev.id} className="panel">
            <p style={{ margin: 0 }}>
              <strong>{ev.title}</strong> — {new Date(ev.eventDate).toLocaleString()} — signups: {ev.currentSignups}/
              {ev.volunteersRequired}
            </p>
            {ev.description ? <p className="empty-hint">{ev.description}</p> : null}
            <div className="btn-row" style={{ marginTop: "0.5rem" }}>
              <button
                type="button"
                className="secondary"
                onClick={() => setDetailId(open ? null : ev.id)}
              >
                {open ? "Hide signups" : "View signups"}
              </button>
            </div>
            {open ? (
              <div style={{ marginTop: "0.85rem" }}>
                {signups == null ? (
                  <p className="empty-hint">Loading signups…</p>
                ) : signups.signups.length === 0 ? (
                  <p className="empty-hint">No one has signed up yet.</p>
                ) : (
                  <div className="table-scroll">
                    <table className="data-table">
                      <thead>
                        <tr>
                          <th>Name</th>
                          <th>Mobile</th>
                          <th>Signed up</th>
                        </tr>
                      </thead>
                      <tbody>
                        {signups.signups.map((s, i) => (
                          <tr key={`${s.participantName}-${s.mobileNumber}-${i}`}>
                            <td>{s.participantName || "—"}</td>
                            <td>{s.mobileNumber || "—"}</td>
                            <td>{s.signedUpAt ? new Date(s.signedUpAt).toLocaleString() : "—"}</td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                )}
              </div>
            ) : null}
          </div>
        );
      })}
    </div>
  );
}
