import type { RequestType } from "../types";

/** Single "Ritual help" label for the merged people+fund flow (stored as PEOPLE or legacy FUND). */
export function requestTypeLabel(t: RequestType): string {
  if (t === "RITUAL_HELP_PEOPLE" || t === "RITUAL_HELP_FUND") return "Ritual help";
  if (t === "LAND_RENT") return "Land rent";
  if (t === "INFRASTRUCTURE_FUND") return "Infrastructure fund";
  return t;
}
