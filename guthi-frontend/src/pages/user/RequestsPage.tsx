import { useEffect, useState } from "react";
import { apiJson } from "../../api/client";
import { LIVE_TOPICS } from "../../live/topics";
import { useStompSubscription } from "../../live/StompContext";
import type { RequestType, RequestStatus } from "../../types";
import { RequestStatusChip } from "../../components/RequestStatusChip";
import { requestTypeLabel } from "../../lib/requestTypeLabel";
import { VolunteerRosterList, type VolunteerRoster } from "../../components/VolunteerRosterList";

/** Values shown in the request-type dropdown (ritual people+fund merged in UI). */
type RequestFormType = "LAND_RENT" | "RITUAL_HELP" | "INFRASTRUCTURE_FUND";

type Row = {
  id: string;
  type: RequestType;
  description: string;
  landName: string | null;
  rentPeriodFrom: string | null;
  rentPeriodTo: string | null;
  fundAmountRequested: number | null;
  peopleRequested: number | null;
  status: RequestStatus;
  adminNote: string | null;
  createdAt: string;
  landQueueRank: number | null;
  landQueueSize: number | null;
  landOccupied: boolean | null;
  landOccupancyNote: string | null;
  volunteerRoster: VolunteerRoster | null;
};

type VolunteerCall = {
  id: string;
  requesterName: string;
  type: RequestType;
  summary: string;
  peopleNeeded: number | null;
  status: "CALLED" | "ACCEPTED" | "DECLINED";
};

export function RequestsPage() {
  const [rows, setRows] = useState<Row[]>([]);
  const [calls, setCalls] = useState<VolunteerCall[]>([]);
  const [statusFilter, setStatusFilter] = useState<"ALL" | RequestStatus>("ALL");
  const [editingId, setEditingId] = useState<string | null>(null);
  const [type, setType] = useState<RequestFormType>("INFRASTRUCTURE_FUND");

  const [lands, setLands] = useState<string[]>([]);
  const [landName, setLandName] = useState("");
  const [rentFrom, setRentFrom] = useState("");
  const [rentTo, setRentTo] = useState("");
  const [description, setDescription] = useState("");
  const [fund, setFund] = useState("");
  const [people, setPeople] = useState("");

  const [err, setErr] = useState("");
  const [msg, setMsg] = useState("");
  const [liveAvailablePool, setLiveAvailablePool] = useState<number | null>(null);

  async function load() {
    const data = await apiJson<Row[]>("/api/requests/my");
    setRows(data);
    const mine = await apiJson<VolunteerCall[]>("/api/requests/volunteer-calls");
    setCalls(mine);
  }

  useEffect(() => {
    load().catch((e) => setErr(e instanceof Error ? e.message : "Load failed"));
    apiJson<string[]>("/api/requests/lands")
      .then(setLands)
      .catch((e) => setErr(e instanceof Error ? e.message : "Could not load Guthi plots"));
  }, []);

  useStompSubscription(LIVE_TOPICS.donationsPool, (body) => {
    try {
      const pool = JSON.parse(body) as { availablePool: number };
      if (typeof pool.availablePool === "number" && Number.isFinite(pool.availablePool)) {
        setLiveAvailablePool(pool.availablePool);
      }
    } catch {
      /* ignore */
    }
  });

  useStompSubscription(LIVE_TOPICS.requestsMyQueue, (body) => {
    try {
      const data = JSON.parse(body) as Row[];
      setRows(data);
    } catch {
      /* ignore */
    }
  });

  useStompSubscription(LIVE_TOPICS.volunteerCalls, (body) => {
    try {
      setCalls(JSON.parse(body) as VolunteerCall[]);
    } catch {
      /* ignore */
    }
  });

  async function answerCall(id: string, action: "accept" | "decline") {
    const verb = action === "accept" ? "Accept" : "Decline";
    if (!window.confirm(`${verb} this volunteer call?`)) return;
    setErr("");
    setMsg("");
    try {
      const mine = await apiJson<VolunteerCall[]>(`/api/requests/volunteer-calls/${id}/${action}`, {
        method: "PUT",
        body: JSON.stringify({}),
      });
      setCalls(mine);
      setMsg(action === "accept" ? "You accepted. Please go and help." : "You declined. The next member in the queue will be called.");
    } catch (e) {
      setErr(e instanceof Error ? e.message : "Could not update the volunteer call");
    }
  }

  function resetFormForType(next: RequestFormType) {
    setLandName("");
    setRentFrom("");
    setRentTo("");
    setDescription("");
    setFund("");
    setPeople("");
    setErr("");
    setMsg("");
    setType(next);
  }

  function volunteerCount(): number | undefined {
    const raw = people.trim();
    if (!raw) return undefined;
    const parsed = Number(raw);
    if (!Number.isFinite(parsed) || parsed < 1) {
      throw new Error("Volunteers needed must be at least 1, or leave the field empty.");
    }
    return parsed;
  }

  async function submit(e: React.FormEvent) {
    e.preventDefault();
    setErr("");
    setMsg("");
    try {
      const body: Record<string, unknown> = {};

      if (type === "LAND_RENT") {
        body.type = "LAND_RENT";
        body.landName = landName.trim();
        body.rentPeriodFrom = rentFrom || undefined;
        body.rentPeriodTo = rentTo || undefined;
        body.description = description.trim() || undefined;
        const landPeople = volunteerCount();
        if (landPeople !== undefined) body.peopleRequested = landPeople;
      } else if (type === "RITUAL_HELP") {
        if (!description.trim()) {
          setErr("Description is required.");
          return;
        }
        const pRaw = people.trim();
        const fRaw = fund.trim();
        let parsedPeople: number | undefined;
        let parsedFund: number | undefined;
        if (pRaw) {
          const p = Number(pRaw);
          if (!Number.isFinite(p) || p < 1) {
            setErr("Number of people must be at least 1, or leave the field empty.");
            return;
          }
          parsedPeople = p;
        }
        if (fRaw) {
          const f = Number(fRaw);
          if (!Number.isFinite(f) || f <= 0) {
            setErr("Fund amount must be greater than 0, or leave the field empty.");
            return;
          }
          parsedFund = f;
        }
        // Fund-only ritual uses API type RITUAL_HELP_FUND; people and/or fund stay optional in the UI.
        const ritualApiType: RequestType =
          parsedFund !== undefined && parsedPeople === undefined ? "RITUAL_HELP_FUND" : "RITUAL_HELP_PEOPLE";
        body.type = ritualApiType;
        body.description = description.trim();
        if (parsedPeople !== undefined) body.peopleRequested = parsedPeople;
        if (parsedFund !== undefined) body.fundAmountRequested = parsedFund;
      } else {
        body.type = "INFRASTRUCTURE_FUND";
        body.description = description.trim();
        const fTrim = fund.trim();
        body.fundAmountRequested = fTrim !== "" ? Number(fTrim) : undefined;
        const infraPeople = volunteerCount();
        if (infraPeople !== undefined) body.peopleRequested = infraPeople;
      }

      const fundAsk = body.fundAmountRequested;
      if (typeof fundAsk === "number" && Number.isFinite(fundAsk) && fundAsk > 0) {
        const avail =
          liveAvailablePool != null && Number.isFinite(liveAvailablePool)
            ? liveAvailablePool
            : Number((await apiJson<{ availablePool: number }>("/api/donations/summary")).availablePool);
        if (Number.isFinite(avail) && fundAsk > avail) {
          setErr(
            `Insufficient funds: you asked for ${fundAsk} but the community donation pool only has ${avail} available right now. Lower the amount or try again after more donations. Your request was not sent to admins.`
          );
          return;
        }
      }

      await apiJson(editingId ? `/api/requests/${editingId}` : "/api/requests", {
        method: editingId ? "PUT" : "POST",
        body: JSON.stringify(body),
      });
      setLandName("");
      setRentFrom("");
      setRentTo("");
      setDescription("");
      setFund("");
      setPeople("");
      setEditingId(null);
      setErr("");
      setMsg(editingId ? "Request updated." : "Request submitted.");
      await load();
    } catch (e) {
      setMsg("");
      setErr(e instanceof Error ? e.message : "Submit failed");
    }
  }

  function cancelEdit() {
    setEditingId(null);
    setLandName("");
    setRentFrom("");
    setRentTo("");
    setDescription("");
    setFund("");
    setPeople("");
    setErr("");
    setMsg("");
  }

  function beginEdit(r: Row) {
    setErr("");
    setMsg("");
    setEditingId(r.id);
    setFund(r.fundAmountRequested != null ? String(r.fundAmountRequested) : "");
    setPeople(r.peopleRequested != null ? String(r.peopleRequested) : "");
    if (r.type === "LAND_RENT") {
      setType("LAND_RENT");
      setLandName(r.landName ?? "");
      setRentFrom(r.rentPeriodFrom ?? "");
      setRentTo(r.rentPeriodTo ?? "");
      const marker = "Notes: ";
      const at = r.description.indexOf(marker);
      setDescription(at >= 0 ? r.description.slice(at + marker.length) : "");
    } else if (r.type === "INFRASTRUCTURE_FUND") {
      setType("INFRASTRUCTURE_FUND");
      setLandName("");
      setDescription(r.description);
    } else {
      setType("RITUAL_HELP");
      setLandName("");
      setDescription(r.description);
    }
    window.scrollTo({ top: 0, behavior: "smooth" });
  }

  async function withdraw(r: Row) {
    if (!window.confirm("Withdraw this pending request? It will be removed and cannot be approved.")) {
      return;
    }
    setErr("");
    setMsg("");
    try {
      await apiJson(`/api/requests/${r.id}`, { method: "DELETE" });
      if (editingId === r.id) {
        cancelEdit();
      }
      setMsg("Request withdrawn.");
      await load();
    } catch (e) {
      setErr(e instanceof Error ? e.message : "Could not withdraw the request");
    }
  }

  function summaryCell(r: Row): string {
    if (r.type === "LAND_RENT" && r.landName) {
      const p =
        r.rentPeriodFrom && r.rentPeriodTo ? ` (${r.rentPeriodFrom} → ${r.rentPeriodTo})` : "";
      return `${r.landName}${p}`;
    }
    const d = r.description ?? "";
    return d.length > 100 ? `${d.slice(0, 100)}…` : d;
  }

  return (
    <div className="page">
      <header className="page__header">
        <h1 className="page__title">Community requests</h1>
        <p className="page__lede">
          Submit a request for land rent, ritual help (optional people and fund), or infrastructure funds. For land,
          you see your place in the plot queue. Dates that overlap an already approved rental of that plot are
          rejected. The Guthi ranks every pending request for admins; an admin can still approve any request whose
          dates are free.
        </p>
      </header>

      <div className="panel">
        <h2 className="panel__title">{editingId ? "Edit pending request" : "New request"}</h2>
        <form className="form-stack" onSubmit={submit}>
          {err ? <p className="error">{err}</p> : null}
          {msg ? <p className="success">{msg}</p> : null}

          <label htmlFor="req-type">Request type</label>
          <select
            id="req-type"
            value={type}
            onChange={(e) => resetFormForType(e.target.value as RequestFormType)}
          >
            <option value="LAND_RENT">Land rent</option>
            <option value="RITUAL_HELP">Ritual help</option>
            <option value="INFRASTRUCTURE_FUND">Infrastructure fund</option>
          </select>

          {type === "LAND_RENT" ? (
            <>
              <label htmlFor="req-land">Guthi plot</label>
              <select
                id="req-land"
                value={landName}
                onChange={(e) => setLandName(e.target.value)}
                required
              >
                <option value="">Choose a plot</option>
                {lands.map((name) => (
                  <option key={name} value={name}>
                    {name}
                  </option>
                ))}
              </select>
              <p className="empty-hint" style={{ margin: "0.35rem 0 0", fontSize: "0.85rem" }}>
                These are the plots the Guthi holds. Pick one from the list.
              </p>
              <div className="field-row">
                <div>
                  <label htmlFor="req-rent-from">Rent period from</label>
                  <input id="req-rent-from" type="date" value={rentFrom} onChange={(e) => setRentFrom(e.target.value)} required />
                </div>
                <div>
                  <label htmlFor="req-rent-to">Rent period to</label>
                  <input id="req-rent-to" type="date" value={rentTo} onChange={(e) => setRentTo(e.target.value)} required />
                </div>
              </div>
              <label htmlFor="req-land-people">Volunteers needed (optional)</label>
              <input
                id="req-land-people"
                type="number"
                min="1"
                inputMode="numeric"
                value={people}
                onChange={(e) => setPeople(e.target.value)}
                placeholder="Leave blank if nobody is needed"
              />
              <label htmlFor="req-land-notes">Additional notes (optional)</label>
              <textarea
                id="req-land-notes"
                rows={3}
                value={description}
                onChange={(e) => setDescription(e.target.value)}
                placeholder="Access, crop cycle, or other context for trustees…"
              />
            </>
          ) : null}

          {type === "RITUAL_HELP" ? (
            <>
              <label htmlFor="req-ritual-desc">Description</label>
              <textarea
                id="req-ritual-desc"
                rows={4}
                value={description}
                onChange={(e) => setDescription(e.target.value)}
                placeholder="Which ritual, when, what help, materials or costs…"
                required
              />
              <div className="field-row">
                <div>
                  <label htmlFor="req-ritual-fund">Fund amount (optional)</label>
                  <input
                    id="req-ritual-fund"
                    type="number"
                    min="0.01"
                    step="0.01"
                    value={fund}
                    onChange={(e) => setFund(e.target.value)}
                    placeholder="Leave blank if not needed"
                  />
                </div>
                <div>
                  <label htmlFor="req-ritual-people">Volunteers needed (optional)</label>
                  <input
                    id="req-ritual-people"
                    type="number"
                    min="1"
                    inputMode="numeric"
                    value={people}
                    onChange={(e) => setPeople(e.target.value)}
                    placeholder="Leave blank if nobody is needed"
                  />
                </div>
              </div>
              <p className="empty-hint" style={{ margin: "0.35rem 0 0", fontSize: "0.85rem" }}>
                If you enter a fund amount, it cannot exceed the <strong>community donation pool</strong> available
                now (Donations / Home). Volunteers needed is how many community members should be called after an
                admin approves. The first that many people in the member queue are asked. If one declines, the next
                person in the queue is asked instead.
              </p>
            </>
          ) : null}

          {type === "INFRASTRUCTURE_FUND" ? (
            <>
              <label htmlFor="req-infra-desc">Description</label>
              <textarea
                id="req-infra-desc"
                rows={4}
                value={description}
                onChange={(e) => setDescription(e.target.value)}
                placeholder="What infrastructure, location, and expected impact?"
                required
              />
              <label htmlFor="req-infra-fund">Funds required</label>
              <p className="empty-hint" style={{ margin: "0.25rem 0 0.35rem", fontSize: "0.85rem" }}>
                Must not exceed the <strong>community donation pool</strong> available right now (see Donations or
                Home — not a personal wallet). If it does, you get an insufficient funds error and the request is not
                sent to admins.
              </p>
              <input
                id="req-infra-fund"
                type="number"
                min="0.01"
                step="0.01"
                value={fund}
                onChange={(e) => setFund(e.target.value)}
                required
              />
              <label htmlFor="req-infra-people">Volunteers needed (optional)</label>
              <input
                id="req-infra-people"
                type="number"
                min="1"
                inputMode="numeric"
                value={people}
                onChange={(e) => setPeople(e.target.value)}
                placeholder="Leave blank if nobody is needed"
              />
            </>
          ) : null}

          <div className="btn-row">
            <button type="submit">{editingId ? "Save changes" : "Submit"}</button>
            {editingId ? (
              <button type="button" className="secondary" onClick={cancelEdit}>
                Cancel edit
              </button>
            ) : null}
          </div>
        </form>
      </div>

      <h2 className="page__section-title">Your volunteer calls</h2>
      <div className="panel">
        <p className="empty-hint" style={{ marginTop: 0 }}>
          After an admin approves a request that asks for volunteers, the first members in the join queue are called.
          If you are one of them, accept or decline here. A decline removes you from the middle of that list and calls
          the next person.
        </p>
        {calls.length === 0 ? (
          <p className="empty-hint">You have no open volunteer calls.</p>
        ) : (
          <div className="table-scroll">
            <table className="data-table">
              <thead>
                <tr>
                  <th>Request</th>
                  <th>From</th>
                  <th>Your answer</th>
                  <th>Actions</th>
                </tr>
              </thead>
              <tbody>
                {calls.map((call) => (
                  <tr key={call.id}>
                    <td className="data-table__cell--summary">
                      {requestTypeLabel(call.type)}
                      {call.peopleNeeded != null ? ` · ${call.peopleNeeded} volunteers` : ""}
                      <div>{call.summary}</div>
                    </td>
                    <td>{call.requesterName}</td>
                    <td>{call.status === "ACCEPTED" ? "Accepted — please go and help" : "Called — answer needed"}</td>
                    <td>
                      {call.status === "CALLED" ? (
                        <div className="btn-row btn-row--stack">
                          <button type="button" onClick={() => answerCall(call.id, "accept")}>
                            Accept
                          </button>
                          <button type="button" className="danger" onClick={() => answerCall(call.id, "decline")}>
                            Decline
                          </button>
                        </div>
                      ) : (
                        <span className="empty-hint">—</span>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>

      <h2 className="page__section-title">My requests</h2>
      <div className="panel">
        <label htmlFor="my-req-status">Filter by status</label>
        <select
          id="my-req-status"
          value={statusFilter}
          onChange={(e) => setStatusFilter(e.target.value as "ALL" | RequestStatus)}
        >
          <option value="ALL">All</option>
          <option value="PENDING">Pending</option>
          <option value="APPROVED">Approved</option>
          <option value="REJECTED">Rejected</option>
        </select>
      </div>
      <div className="panel" style={{ padding: 0, overflow: "hidden" }}>
        <div className="table-scroll">
          <table className="data-table">
            <thead>
              <tr>
                <th>Submitted</th>
                <th>Type</th>
                <th>Status</th>
                <th>Land queue</th>
                <th>Fund</th>
                <th>Summary</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {rows
                .filter((r) => statusFilter === "ALL" || r.status === statusFilter)
                .map((r) => (
                <tr key={r.id}>
                  <td className="data-table__cell--narrow">{new Date(r.createdAt).toLocaleDateString()}</td>
                  <td>{requestTypeLabel(r.type)}</td>
                  <td>
                    <RequestStatusChip status={r.status} />
                  </td>
                  <td>
                    {r.type === "LAND_RENT" ? (
                      <>
                        {r.status === "PENDING" && r.landQueueRank != null
                          ? r.landQueueRank === 1
                            ? `Rank ${r.landQueueRank} of ${r.landQueueSize} — longest wait for this plot`
                            : `Rank ${r.landQueueRank} of ${r.landQueueSize} — someone has waited longer`
                          : "—"}
                        {r.landOccupancyNote ? <div>{r.landOccupancyNote}</div> : null}
                      </>
                    ) : (
                      "—"
                    )}
                  </td>
                  <td>{r.fundAmountRequested ?? "—"}</td>
                  <td className="data-table__cell--summary">
                    {summaryCell(r)}
                    <VolunteerRosterList roster={r.volunteerRoster} />
                  </td>
                  <td>
                    {r.status === "PENDING" ? (
                      <div className="btn-row btn-row--stack">
                        <button type="button" className="secondary" onClick={() => beginEdit(r)}>
                          Edit
                        </button>
                        <button type="button" className="danger" onClick={() => withdraw(r)}>
                          Withdraw
                        </button>
                      </div>
                    ) : (
                      <span className="empty-hint">—</span>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
}
