import { useEffect, useState } from "react";
import { apiJson } from "../../api/client";

type DonorTotal = {
  donorId: string;
  donorName: string;
  donorEmail: string;
  totalAmount: number;
  donationCount: number;
};

type PurposeTotal = {
  purpose: string;
  totalAmount: number;
  allocationCount: number;
};

type Report = {
  totalDonated: number;
  totalAllocated: number;
  availablePool: number;
  donors: DonorTotal[];
  allocationsByPurpose: PurposeTotal[];
};

function formatInr(n: number) {
  return Number(n).toLocaleString("en-IN", { maximumFractionDigits: 0 });
}

export function AdminReportPage() {
  const [q, setQ] = useState("");
  const [report, setReport] = useState<Report | null>(null);
  const [err, setErr] = useState("");

  useEffect(() => {
    const handle = window.setTimeout(() => {
      const params = q.trim() ? `?q=${encodeURIComponent(q.trim())}` : "";
      apiJson<Report>(`/api/admin/reports/contributions${params}`)
        .then((data) => {
          setReport(data);
          setErr("");
        })
        .catch((e) => setErr(e instanceof Error ? e.message : "Could not load the report"));
    }, 200);
    return () => window.clearTimeout(handle);
  }, [q]);

  return (
    <div className="page">
      <header className="page__header">
        <h1 className="page__title">Contribution report</h1>
        <p className="page__lede">
          Totals are calculated in MongoDB. Donor rows join each donation to the member who gave it. Spending rows
          group allocations by purpose. Search filters both lists.
        </p>
      </header>
      {err ? <p className="error">{err}</p> : null}
      <div className="panel">
        <label htmlFor="report-q">Search donor or purpose</label>
        <input
          id="report-q"
          value={q}
          onChange={(e) => setQ(e.target.value)}
          placeholder="Name, email, or allocation purpose"
        />
      </div>
      {report ? (
        <>
          <div className="stat-grid">
            <div className="stat-card">
              <span className="stat-card__label">Total donated</span>
              <span className="stat-card__value">₹{formatInr(Number(report.totalDonated))}</span>
            </div>
            <div className="stat-card">
              <span className="stat-card__label">Allocated</span>
              <span className="stat-card__value">₹{formatInr(Number(report.totalAllocated))}</span>
            </div>
            <div className="stat-card stat-card--gold">
              <span className="stat-card__label">Available pool</span>
              <span className="stat-card__value">₹{formatInr(Number(report.availablePool))}</span>
            </div>
          </div>
          <h2 className="page__section-title">Donations by member</h2>
          <div className="panel" style={{ padding: 0, overflow: "hidden" }}>
            <div className="table-scroll">
              <table className="data-table">
                <thead>
                  <tr>
                    <th>Member</th>
                    <th>Email</th>
                    <th>Gifts</th>
                    <th>Total</th>
                  </tr>
                </thead>
                <tbody>
                  {report.donors.length === 0 ? (
                    <tr>
                      <td colSpan={4}>
                        <span className="empty-hint">No donations match this search.</span>
                      </td>
                    </tr>
                  ) : (
                    report.donors.map((row) => (
                      <tr key={row.donorId || row.donorEmail}>
                        <td>{row.donorName}</td>
                        <td>{row.donorEmail || "—"}</td>
                        <td>{row.donationCount}</td>
                        <td>₹{formatInr(Number(row.totalAmount))}</td>
                      </tr>
                    ))
                  )}
                </tbody>
              </table>
            </div>
          </div>
          <h2 className="page__section-title">Spending by purpose</h2>
          <div className="panel" style={{ padding: 0, overflow: "hidden" }}>
            <div className="table-scroll">
              <table className="data-table">
                <thead>
                  <tr>
                    <th>Purpose</th>
                    <th>Allocations</th>
                    <th>Total used</th>
                  </tr>
                </thead>
                <tbody>
                  {report.allocationsByPurpose.length === 0 ? (
                    <tr>
                      <td colSpan={3}>
                        <span className="empty-hint">No allocations match this search.</span>
                      </td>
                    </tr>
                  ) : (
                    report.allocationsByPurpose.map((row) => (
                      <tr key={row.purpose}>
                        <td>{row.purpose}</td>
                        <td>{row.allocationCount}</td>
                        <td>₹{formatInr(Number(row.totalAmount))}</td>
                      </tr>
                    ))
                  )}
                </tbody>
              </table>
            </div>
          </div>
        </>
      ) : null}
    </div>
  );
}
