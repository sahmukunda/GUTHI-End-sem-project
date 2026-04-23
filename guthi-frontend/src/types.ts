export type Role = "USER" | "ADMIN";

export interface AuthResponse {
  token: string;
  email: string;
  fullName: string;
  role: Role;
}

export type RequestType =
  | "LAND_RENT"
  | "RITUAL_HELP_PEOPLE"
  | "RITUAL_HELP_FUND"
  | "INFRASTRUCTURE_FUND";

export type RequestStatus = "PENDING" | "APPROVED" | "REJECTED";
