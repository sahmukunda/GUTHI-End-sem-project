import { useEffect, useState } from "react";
import { apiJson } from "../../api/client";
import { LIVE_TOPICS } from "../../live/topics";
import { useStompSubscription } from "../../live/StompContext";

type Row = {
  id: string;
  donorName: string;
  donorEmail: string;
  amount: number;
  note: string | null;
  upiTransactionId: string;
  status: string;
  submittedAt: string;
  resolvedAt: string | null;
};

function formatInr(n: number) {
  return Number(n).toLocaleString("en-IN", { maximumFractionDigits: 0 });
}

export function AdminDonationClaimsPage() {
  const [rows, setRows] = useState<Row[]>([]);
  const [err, setErr] = useState("");
  const [msg, setMsg] = useState("");

  async function load() {
    const data = await apiJson<Row[]>("/api/admin/donation-claims");
    setRows(data);
  }

  useEffect(() => {
    load().catch((e) => setErr(e instanceof Error ? e.message : "Load failed"));
  }, []);

  useStompSubscription(LIVE_TOPICS.adminDonationClaims, (body) => {
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
      await apiJson(`/api/admin/donation-claims/${row.id}/approve`, { method: "PUT" });
      setMsg(`Claim from ${row.donorName} approved — donation credited to the pool.`);
      await load();
    } catch (e) {
      setErr(e instanceof Error ? e.message : "Approve failed");
    }
  }

  async function reject(row: Row) {
    setErr("");
    setMsg("");
    try {
      await apiJson(`/api/admin/donation-claims/${row.id}/reject`, { method: "PUT" });
      setMsg(`Claim from ${row.donorName} rejected.`);
      await load();
    } catch (e) {
      setErr(e instanceof Error ? e.message : "Reject failed");
    }
  }

  return (
    <div className="page">
      <header className="page__header">
        <h1 className="page__title">UPI donation claims</h1>
        <p className="page__lede">
          Review the 12-digit UPI reference from the member&apos;s app. Approve only when the payment matches the
          amount; approving credits the shared pool and their verified donation total.
        </p>
      </header>
      {err ? <p className="error">{err}</p> : null}
      {msg ? <p className="success">{msg}</p> : null}
      <div className="panel" style={{ padding: 0, overflow: "hidden" }}>
        <div className="table-scroll">
          <table className="data-table">
            <thead>
              <tr>
                <th>Donor</th>
                <th>Email</th>
                <th>Amount</th>
                <th>UPI txn ID</th>
                <th>Note</th>
                <th>Submitted</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {rows.length === 0 ? (
                <tr>
                  <td colSpan={7} className="data-table__cell--detail">
                    <span className="empty-hint">No pending claims.</span>
                  </td>
                </tr>
              ) : (
                rows.map((r) => (
                  <tr key={r.id}>
                    <td>{r.donorName}</td>
                    <td>{r.donorEmail}</td>
                    <td>₹{formatInr(Number(r.amount))}</td>
                    <td>
                      <code>{r.upiTransactionId}</code>
                    </td>
                    <td className="data-table__cell--detail">{r.note ?? "—"}</td>
                    <td className="data-table__cell--narrow">{new Date(r.submittedAt).toLocaleString()}</td>
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
                        <span className="empty-hint">{r.status}</span>
                      )}
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
}
