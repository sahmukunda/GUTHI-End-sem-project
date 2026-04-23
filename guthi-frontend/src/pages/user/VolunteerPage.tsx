import { useEffect, useMemo, useState } from "react";
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
  completed: boolean;
  schedulePast: boolean;
  signupOpen: boolean;
};

function EventListButton({
  ev,
  selected,
  onSelect,
}: {
  ev: Event;
  selected: string | null;
  onSelect: () => void;
}) {
  return (
    <li>
      <button
        type="button"
        className={`festival-pick-btn${selected === ev.id ? "" : " secondary"}`}
        onClick={onSelect}
      >
        {ev.title} — {new Date(ev.eventDate).toLocaleString()}
      </button>
    </li>
  );
}

export function VolunteerPage() {
  const [events, setEvents] = useState<Event[]>([]);
  const [selected, setSelected] = useState<string | null>(null);
  const [participantName, setParticipantName] = useState("");
  const [mobile, setMobile] = useState("");
  const [signupFor, setSignupFor] = useState<string | null>(null);
  const [err, setErr] = useState("");
  const [msg, setMsg] = useState("");

  const { upcoming, pastClosed } = useMemo(() => {
    const now = Date.now();
    const isPast = (ev: Event) => {
      const t = new Date(ev.eventDate).getTime();
      return !Number.isNaN(t) && t < now;
    };
    const u = events.filter((e) => !isPast(e));
    u.sort((a, b) => new Date(a.eventDate).getTime() - new Date(b.eventDate).getTime());
    const p = events.filter((e) => isPast(e));
    p.sort((a, b) => new Date(b.eventDate).getTime() - new Date(a.eventDate).getTime());
    return { upcoming: u, pastClosed: p };
  }, [events]);

  async function load() {
    const data = await apiJson<Event[]>("/api/volunteer-events");
    setEvents(data);
  }

  useEffect(() => {
    load().catch((e) => setErr(e instanceof Error ? e.message : "Load failed"));
  }, []);

  useStompSubscription(LIVE_TOPICS.volunteerEvents, (body) => {
    try {
      setEvents(JSON.parse(body) as Event[]);
    } catch {
      /* ignore */
    }
  });

  const selectedEvent = selected == null ? null : events.find((e) => e.id === selected) ?? null;

  async function signup(eventId: string) {
    setErr("");
    setMsg("");
    try {
      const name = participantName.trim();
      if (!name) {
        setErr("Enter your name for this signup.");
        return;
      }
      if (!mobile.trim()) {
        setErr("Enter a mobile number for this signup.");
        return;
      }
      await apiJson(`/api/volunteer-events/${eventId}/signup`, {
        method: "POST",
        body: JSON.stringify({ participantName: name, mobileNumber: mobile.trim() }),
      });
      setMsg(`Signed up for this event.`);
      setSignupFor(null);
      setParticipantName("");
      setMobile("");
      setEvents((prev) =>
        prev.map((e) =>
          e.id === eventId
            ? {
                ...e,
                currentUserSignedUp: true,
                currentSignups: e.currentSignups + 1,
              }
            : e,
        ),
      );
    } catch (e) {
      const message = e instanceof Error ? e.message : "Signup failed";
      setErr(message);
      if (message.toLowerCase().includes("already signed")) {
        setSignupFor(null);
        await load();
      }
    }
  }

  return (
    <div className="page">
      <header className="page__header">
        <h1 className="page__title">Volunteer events</h1>
        <p className="page__lede">
          Browse upcoming and past shifts. When registration is open, sign up with your name and mobile number so
          organisers can reach you.
        </p>
      </header>
      {err ? <p className="error">{err}</p> : null}
      {msg ? <p className="success">{msg}</p> : null}
      <div className="festivals-split">
        <div className="panel">
          <h2 className="panel__title">Upcoming & past</h2>
          {events.length === 0 ? (
            <p className="empty-hint">No events yet.</p>
          ) : (
            <>
              <h3 className="panel__subtitle" style={{ marginTop: 0, marginBottom: "0.5rem", fontSize: "0.95rem" }}>
                Upcoming
              </h3>
              {upcoming.length === 0 ? (
                <p className="empty-hint" style={{ marginBottom: "1rem" }}>
                  No upcoming events right now.
                </p>
              ) : (
                <ul className="festival-pick-list" style={{ marginBottom: "1.25rem" }}>
                  {upcoming.map((ev) => (
                    <EventListButton
                      key={ev.id}
                      ev={ev}
                      selected={selected}
                      onSelect={() => {
                        setSelected(ev.id);
                        setSignupFor(null);
                        setErr("");
                        setMsg("");
                      }}
                    />
                  ))}
                </ul>
              )}
              <h3 className="panel__subtitle" style={{ marginTop: 0, marginBottom: "0.5rem", fontSize: "0.95rem" }}>
                Past & inactive
              </h3>
              {pastClosed.length === 0 ? (
                <p className="empty-hint">No past events yet.</p>
              ) : (
                <ul className="festival-pick-list">
                  {pastClosed.map((ev) => (
                    <EventListButton
                      key={ev.id}
                      ev={ev}
                      selected={selected}
                      onSelect={() => {
                        setSelected(ev.id);
                        setSignupFor(null);
                        setErr("");
                        setMsg("");
                      }}
                    />
                  ))}
                </ul>
              )}
            </>
          )}
        </div>
        <div className="panel panel--wide">
          {selectedEvent == null ? (
            <p className="empty-hint">Select an event to see details and sign up.</p>
          ) : (
            <>
              <h2 className="panel__title">{selectedEvent.title}</h2>
              {selectedEvent.description ? <p className="empty-hint">{selectedEvent.description}</p> : null}
              <p className="event-meta">
                <strong>When:</strong> {new Date(selectedEvent.eventDate).toLocaleString()}
              </p>
              <p className="event-meta">
                <strong>Volunteers:</strong> {selectedEvent.currentSignups} / {selectedEvent.volunteersRequired}
              </p>

              {selectedEvent.signupOpen && !selectedEvent.currentUserSignedUp ? (
                <>
                  <h3 style={{ marginTop: "1rem" }}>Sign up</h3>
                  {signupFor === selectedEvent.id ? (
                    <div className="form-stack" style={{ marginTop: "0.5rem" }}>
                      <label htmlFor="vol-name">Your name</label>
                      <input
                        id="vol-name"
                        type="text"
                        value={participantName}
                        onChange={(e) => setParticipantName(e.target.value)}
                        maxLength={120}
                        required
                        autoComplete="name"
                      />
                      <label htmlFor="vol-mobile">Mobile number</label>
                      <input
                        id="vol-mobile"
                        type="tel"
                        value={mobile}
                        onChange={(e) => setMobile(e.target.value)}
                        maxLength={30}
                        placeholder="e.g. 9801122334"
                        pattern="^[0-9+\\-\\s]{6,30}$"
                        title="6–30 characters: digits, +, spaces, or dashes"
                      />
                      <div className="btn-row" style={{ marginTop: "0.5rem" }}>
                        <button type="button" onClick={() => signup(selectedEvent.id)}>
                          Confirm signup
                        </button>
                        <button type="button" className="secondary" onClick={() => setSignupFor(null)}>
                          Cancel
                        </button>
                      </div>
                    </div>
                  ) : (
                    <button type="button" style={{ marginTop: "0.5rem" }} onClick={() => setSignupFor(selectedEvent.id)}>
                      Sign up
                    </button>
                  )}
                </>
              ) : selectedEvent.signupOpen && selectedEvent.currentUserSignedUp ? (
                <p className="empty-hint" style={{ marginTop: "1rem" }}>
                  <span className="chip chip--approved">Signed up</span> You are on the list for this event.
                </p>
              ) : !selectedEvent.signupOpen ? (
                <p className="empty-hint" style={{ marginTop: "1rem" }}>
                  Sign-up is closed for this event (date has passed or the event is inactive).
                </p>
              ) : null}
            </>
          )}
        </div>
      </div>
    </div>
  );
}
