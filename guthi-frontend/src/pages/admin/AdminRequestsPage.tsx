import { useEffect, useState } from "react";
import { apiJson } from "../../api/client";
import { LIVE_TOPICS } from "../../live/topics";
import { useStompSubscription } from "../../live/StompContext";
import type { RequestStatus, RequestType } from "../../types";
import { RequestStatusChip } from "../../components/RequestStatusChip";
import { requestTypeLabel } from "../../lib/requestTypeLabel";

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
  status: RequestStatus;
  adminNote: string | null;
  createdAt: string;
};

export function AdminRequestsPage() {
  const [rows, setRows] = useState<Row[]>([]);
  const [note, setNote] = useState("");
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

  return (
    <div className="page">
      <header className="page__header">
        <h1 className="page__title">All community requests</h1>
        <p className="page__lede">Approve or reject with an optional note applied to the next action.</p>
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
      <div className="panel" style={{ padding: 0, overflow: "hidden" }}>
        <div className="table-scroll">
          <table className="data-table">
            <thead>
              <tr>
                <th>Requester</th>
                <th>Type</th>
                <th>Land & rent</th>
                <th>Status</th>
                <th>Fund</th>
                <th>People</th>
                <th>Details</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {rows.map((r) => (
                <tr key={r.id}>
                  <td>{r.requesterName}</td>
                  <td>{requestTypeLabel(r.type)}</td>
                  <td className="data-table__cell--narrow">
                    {r.type === "LAND_RENT" && r.landName
                      ? `${r.landName}\n${r.rentPeriodFrom ?? ""} → ${r.rentPeriodTo ?? ""}`
                      : "—"}
                  </td>
                  <td>
                    <RequestStatusChip status={r.status} />
                  </td>
                  <td>{r.fundAmountRequested ?? "—"}</td>
                  <td>{r.peopleRequested ?? "—"}</td>
                  <td className="data-table__cell--detail">{r.description}</td>
                  <td>
                    {r.status === "PENDING" ? (
                      <div className="btn-row btn-row--stack">
                        <button type="button" onClick={() => approve(r)}>
                          Approve
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
