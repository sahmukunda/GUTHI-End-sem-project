export type PoolPayload = {
  totalDonated: number;
  totalAllocated: number;
  availablePool: number;
  allocations: { purpose: string; amountUsed: number; date: string }[];
};

export type PersonalPayload = {
  myContributionsTotal: number;
  myPendingClaimsTotal: number;
  myUsedFromPoolTotal: number;
  myNetContribution: number;
};

export type Summary = {
  availablePool: number;
  myContributionsTotal?: number;
  myPendingClaimsTotal?: number;
  myUsedFromPoolTotal?: number;
  allocations: { purpose: string; amountUsed: number; date: string }[];
};

export function mergePoolIntoSummary(prev: Summary | null, pool: PoolPayload): Summary {
  const allocations = (pool.allocations ?? []).map((a) => ({
    purpose: a.purpose,
    amountUsed: Number(a.amountUsed),
    date: typeof a.date === "string" ? a.date : String(a.date),
  }));
  if (!prev) {
    return {
      availablePool: Number(pool.availablePool),
      myContributionsTotal: 0,
      myPendingClaimsTotal: 0,
      myUsedFromPoolTotal: 0,
      allocations,
    };
  }
  return {
    ...prev,
    availablePool: Number(pool.availablePool),
    allocations,
  };
}

export function mergePersonalIntoSummary(prev: Summary | null, p: PersonalPayload): Summary {
  const base =
    prev ??
    ({
      availablePool: 0,
      allocations: [],
    } as Summary);
  return {
    ...base,
    myContributionsTotal: Number(p.myContributionsTotal),
    myPendingClaimsTotal: Number(p.myPendingClaimsTotal),
    myUsedFromPoolTotal: Number(p.myUsedFromPoolTotal),
    myNetContribution: Number(p.myNetContribution),
  };
}
