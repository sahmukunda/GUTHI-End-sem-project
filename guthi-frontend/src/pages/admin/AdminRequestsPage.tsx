import { useEffect, useState } from "react";
import { apiJson } from "../../api/client";
import { LIVE_TOPICS } from "../../live/topics";
import { useStompSubscription } from "../../live/StompContext";
import type { RequestStatus, RequestType } from "../../types";
import { RequestStatusChip } from "../../components/RequestStatusChip";
import { requestTypeLabel } from "../../lib/requestTypeLabel";
import { VolunteerRosterList, type VolunteerRoster } from "../../components/VolunteerRosterList";

type Row = {
  id: string;
  requesterName: string;
  type: RequestType;
  description: string;
  landName: string | null;
  rentPeriodFrom: string | null;
  rentPeriodTo: string | null;
  fundAmountRequested: number | null;
  peopleRequested: number | null;
  volunteerRoster: VolunteerRoster | null;
  status: RequestStatus;
  adminNote: string | null;
  createdAt: string;
  landQueueRank: number | null;
  landQueueSize: number | null;
  queueRank: number | null;
  queueSize: number | null;
  fundPlanSelected: boolean | null;
  landOccupied: boolean | null;
  landOccupancyNote: string | null;
  landCalendarPick: boolean | null;
};

export function AdminRequestsPage() {
  const [rows, setRows] = useState<Row[]>([]);
  const [note, setNote] = useState("");
  const [q, setQ] = useState("");
  const [err, setErr] = useState("");
  const [msg, setMsg] = useState("");

  async function load() {
    const data = await apiJson<Row[]>("/api/requests/all");
    setRows(data);
  }

  useEffect(() => {
    load().catch((e) => setErr(e instanceof Error ? e.message : "Load failed"));
  }, []);

  useStompSubscription(LIVE_TOPICS.adminRequests, (body) => {
    try {
      setRows(JSON.parse(body) as Row[]);
    } catch {
      /* ignore */
    }
  });

  async function approve(row: Row) {
    const fund =
      row.fundAmountRequested != null && Number(row.fundAmountRequested) > 0
        ? ` This allocates ₹${Number(row.fundAmountRequested).toLocaleString("en-IN")} from the pool.`
        : "";
    if (!window.confirm(`Approve the request from ${row.requesterName}?${fund}`)) {
      return;
    }
    setErr("");
    setMsg("");
    try {
      await apiJson(`/api/requests/${row.id}/approve`, {
        method: "PUT",
        body: JSON.stringify({ adminNote: note || undefined }),
      });
      setMsg(`Request from ${row.requesterName} approved.`);
      setNote("");
      await load();
    } catch (e) {
      setErr(e instanceof Error ? e.message : "Approve failed");
    }
  }

  async function reject(row: Row) {
    if (!window.confirm(`Reject the request from ${row.requesterName}?`)) {
      return;
    }
    setErr("");
    setMsg("");
    try {
      await apiJson(`/api/requests/${row.id}/reject`, {
        method: "PUT",
        body: JSON.stringify({ adminNote: note || undefined }),
      });
      setMsg(`Request from ${row.requesterName} rejected.`);
      setNote("");
      await load();
    } catch (e) {
      setErr(e instanceof Error ? e.message : "Reject failed");
    }
  }

  const visible = rows.filter((r) => {
    const hay = `${r.requesterName} ${r.type} ${r.landName ?? ""} ${r.description}`.toLowerCase();
    return hay.includes(q.trim().toLowerCase());
  });
  const planned = rows.filter((r) => r.fundPlanSelected);
  const plannedSpend = planned.reduce((sum, r) => sum + Number(r.fundAmountRequested ?? 0), 0);

  return (
    <div className="page">
      <header className="page__header">
        <h1 className="page__title">All community requests</h1>
        <p className="page__lede">
          Pending requests are listed in rank order: ritual help with people, then ritual funds, then infrastructure,
          then land. Inside one type, the member who has waited longest is first. Rank is a suggestion — you can
          approve or reject any pending request. A land request is blocked only when its dates overlap an approved
          rental of the same plot.
        </p>
      </header>
      {err ? <p className="error">{err}</p> : null}
      {msg ? <p className="success">{msg}</p> : null}
      <div className="panel">
        <h2 className="panel__title">Admin note</h2>
        <p className="empty-hint" style={{ marginTop: 0 }}>
          Optional — reused when you press Approve or Reject on a row.
        </p>
        <label htmlFor="admin-req-note">Note for next action</label>
        <textarea id="admin-req-note" rows={2} value={note} onChange={(e) => setNote(e.target.value)} />
      </div>
      <div className="panel">
        <label htmlFor="admin-req-search">Search requests</label>
        <input
          id="admin-req-search"
          value={q}
          onChange={(e) => setQ(e.target.value)}
          placeholder="Requester, type, land, or description"
        />
      </div>
      <div className="panel">
        <h2 className="panel__title">Suggested fund plan</h2>
        <p className="empty-hint" style={{ marginTop: 0 }}>
          {planned.length === 0
            ? "No pending fund request fits the donation pool. Highlighted rows appear when the knapsack finds a set that does."
            : `${planned.length} request${planned.length === 1 ? "" : "s"} fit the pool for the best score, about ₹${plannedSpend.toLocaleString("en-IN")}. Highlighted rows are the suggestion. Approving a different request is still allowed.`}
        </p>
      </div>
      <div className="panel" style={{ padding: 0, overflow: "hidden" }}>
        <div className="table-scroll">
          <table className="data-table">
            <thead>
              <tr>
                <th>Rank</th>
                <th>Requester</th>
                <th>Type</th>
                <th>Land & rent</th>
                <th>Land calendar</th>
                <th>Status</th>
                <th>Fund</th>
                <th>People</th>
                <th>Details</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {visible.map((r) => (
                <tr key={r.id} className={r.fundPlanSelected ? "data-table__row--plan" : undefined}>
                  <td>
                    {r.status === "PENDING" && r.queueRank != null ? (
                      <span className={r.queueRank === 1 ? "chip chip--approved" : "chip"}>
                        {r.queueRank} of {r.queueSize}
                      </span>
                    ) : (
                      "—"
                    )}
                  </td>
                  <td>{r.requesterName}</td>
                  <td>{requestTypeLabel(r.type)}</td>
                  <td className="data-table__cell--narrow">
                    {r.type === "LAND_RENT" && r.landName
                      ? `${r.landName}\n${r.rentPeriodFrom ?? ""} → ${r.rentPeriodTo ?? ""}`
                      : "—"}
                  </td>
                  <td className="data-table__cell--narrow">
                    {r.type === "LAND_RENT" ? (
                      <>
                        {r.status === "PENDING" && r.landQueueRank != null
                          ? `Plot queue ${r.landQueueRank} of ${r.landQueueSize}`
                          : null}
                        {r.landOccupancyNote ? (
                          <div>{r.landOccupancyNote}</div>
                        ) : null}
                      </>
                    ) : (
                      "—"
                    )}
                  </td>
                  <td>
                    <RequestStatusChip status={r.status} />
                  </td>
                  <td>
                    {r.fundAmountRequested ?? "—"}
                    {r.fundPlanSelected ? (
                      <div>
                        <span className="chip chip--approved">In fund plan</span>
                      </div>
                    ) : null}
                  </td>
                  <td>
                    {r.peopleRequested ?? "—"}
                    <VolunteerRosterList roster={r.volunteerRoster} />
                  </td>
                  <td className="data-table__cell--detail">{r.description}</td>
                  <td>
                    {r.status === "PENDING" ? (
                      <div className="btn-row btn-row--stack">
                        <button type="button" onClick={() => approve(r)} disabled={r.landOccupied === true}>
                          {r.landOccupied ? "Dates taken" : "Approve"}
                        </button>
                        <button type="button" className="danger" onClick={() => reject(r)}>
                          Reject
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
