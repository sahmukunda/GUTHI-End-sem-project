import type { RequestStatus } from "../types";

export function RequestStatusChip({ status }: { status: RequestStatus }) {
  const cls =
    status === "PENDING"
      ? "chip chip--pending"
      : status === "APPROVED"
        ? "chip chip--approved"
        : "chip chip--rejected";
  return <span className={cls}>{status}</span>;
}
