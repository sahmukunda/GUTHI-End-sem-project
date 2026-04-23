print("--- sample claim post-migration ---");
printjson(db.donation_claims.findOne({}, { amount: 1, status: 1, donorId: 1 }));

print("--- totals across collections ---");
const td = db.donations.aggregate([{ $group: { _id: null, t: { $sum: "$amount" } } }]).toArray();
print("totalDonated:", td[0] ? td[0].t : 0);

const ta = db.donation_allocations.aggregate([{ $group: { _id: null, t: { $sum: "$amountUsed" } } }]).toArray();
print("totalAllocated:", ta[0] ? ta[0].t : 0);

const pp = db.donation_claims.aggregate([
  { $match: { donorId: "69ea66d232f3731c87866253", status: "PENDING" } },
  { $group: { _id: null, t: { $sum: "$amount" } } },
]).toArray();
print("Priya pending sum:", pp[0] ? pp[0].t : 0);

const pv = db.donations.aggregate([
  { $match: { donorId: "69ea66d232f3731c87866253" } },
  { $group: { _id: null, t: { $sum: "$amount" } } },
]).toArray();
print("Priya verified donations sum:", pv[0] ? pv[0].t : 0);
