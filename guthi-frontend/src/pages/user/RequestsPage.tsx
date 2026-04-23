import { useEffect, useState } from "react";
import { apiJson } from "../../api/client";
import { LIVE_TOPICS } from "../../live/topics";
import { useStompSubscription } from "../../live/StompContext";
import type { RequestType, RequestStatus } from "../../types";
import { RequestStatusChip } from "../../components/RequestStatusChip";
import { requestTypeLabel } from "../../lib/requestTypeLabel";

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
};

export function RequestsPage() {
  const [rows, setRows] = useState<Row[]>([]);
  const [type, setType] = useState<RequestFormType>("INFRASTRUCTURE_FUND");

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
  }

  useEffect(() => {
    load().catch((e) => setErr(e instanceof Error ? e.message : "Load failed"));
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

      await apiJson("/api/requests", {
        method: "POST",
        body: JSON.stringify(body),
      });
      setLandName("");
      setRentFrom("");
      setRentTo("");
      setDescription("");
      setFund("");
      setPeople("");
      setErr("");
      setMsg("Request submitted.");
      await load();
    } catch (e) {
      setMsg("");
      setErr(e instanceof Error ? e.message : "Submit failed");
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
          Submit a request for land rent, ritual help (optional people and fund), or infrastructure funds. Track status
          below.
        </p>
      </header>

      <div className="panel">
        <h2 className="panel__title">New request</h2>
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
              <label htmlFor="req-land">Name of land / parcel</label>
              <input
                id="req-land"
                value={landName}
                onChange={(e) => setLandName(e.target.value)}
                placeholder="e.g. Ward 4 community field"
                required
              />
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
                  <label htmlFor="req-ritual-people">Number of people (optional)</label>
                  <input
                    id="req-ritual-people"
                    type="number"
                    inputMode="numeric"
                    value={people}
                    onChange={(e) => setPeople(e.target.value)}
                    placeholder="Leave blank if not needed"
                  />
                </div>
              </div>
              <p className="empty-hint" style={{ margin: "0.35rem 0 0", fontSize: "0.85rem" }}>
                If you enter a fund amount, it cannot exceed the <strong>community donation pool</strong> available
                now (Donations / Home). Otherwise you will see an insufficient funds message and nothing is sent to
                admins.
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
            </>
          ) : null}

          <button type="submit">Submit</button>
        </form>
      </div>

      <h2 className="page__section-title">My requests</h2>
      <div className="panel" style={{ padding: 0, overflow: "hidden" }}>
        <div className="table-scroll">
          <table className="data-table">
            <thead>
              <tr>
                <th>Submitted</th>
                <th>Type</th>
                <th>Status</th>
                <th>Fund</th>
                <th>Summary</th>
              </tr>
            </thead>
            <tbody>
              {rows.map((r) => (
                <tr key={r.id}>
                  <td className="data-table__cell--narrow">{new Date(r.createdAt).toLocaleDateString()}</td>
                  <td>{requestTypeLabel(r.type)}</td>
                  <td>
                    <RequestStatusChip status={r.status} />
                  </td>
                  <td>{r.fundAmountRequested ?? "—"}</td>
                  <td className="data-table__cell--summary">{summaryCell(r)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
}
