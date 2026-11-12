# DBMS mini project notes — Guthi

This file is the short report for the database requirements: problem, users, document model, why references are used, the queries behind the contribution report, and a test-case table.

## Problem and users

A Guthi keeps one shared fund, allocates Guthi land, pays for rituals and the Guthi house, and organises festivals and member labour. Two roles use the same MongoDB database `guthi`:

| Role | Who | What they may do |
|---|---|---|
| `USER` | Community member | Register, donate, submit requests, volunteer, post or accept work, review a completed festival, read their own requests |
| `ADMIN` | Guthi member | Approve or reject claims and requests, create events and festivals, read the contribution report |

A community member who calls an admin URL receives 403 from the server. The donation summary requires a login.

## Document model

Collections reference each other by id. A donation does not embed the whole user, because one member has many donations, claims, and requests, and the member document changes independently (name, password).

| Collection | Identifier | Points at |
|---|---|---|
| `users` | `_id` | — |
| `donations` | `_id`, `donorId` | `users._id` |
| `donation_claims` | `_id`, unique `upiTransactionId` | `users._id` |
| `donation_allocations` | `_id` | approved by `users._id` |
| `community_requests` | `_id`, `requesterId` | `users._id` |
| `festivals` | `_id` | — |
| `reviews` | unique (`festivalId`, `reviewerId`) | festival and user |
| `volunteer_events` | `_id` | — |
| `volunteer_signups` | unique (`eventId`, `userId`) | event and user |
| `work_posts` | `_id`, `posterId` | `users._id` |
| `work_acceptances` | unique (`workPostId`, `acceptedById`) | post and user |

Example donation:

```json
{
  "donorId": "66f0c2a1b3e4d5a678901234",
  "amount": { "$numberDecimal": "5000" },
  "note": "Dashain contribution",
  "donatedAt": { "$date": "2026-09-10T00:00:00Z" }
}
```

Request documents share one collection because the fields differ by type: land rent stores `landName` and dates, ritual help stores people and/or a fund, infrastructure stores a fund. Empty fields are omitted instead of forcing every request into separate tables.

`volunteer_event_reviews` is not part of this model. Nothing in the application writes it.

## Integrity

- Unique indexes: email, UPI transaction id, one review per member per festival, one acceptance per member per post, one signup per member per event.
- JSON Schema validators on those collections reject a new document that is missing a required field or has a wrong type. Validation level is moderate, so older documents are not rewritten.
- Rules that span collections stay in the service layer because MongoDB cannot express them as a foreign key: a fund request cannot exceed the pool, a member cannot accept their own work post, and a review is allowed only after the festival is marked complete.
- Approving a donation claim writes `donations` and updates `donation_claims` in one transaction. Approving a funded request writes `donation_allocations` and updates `community_requests` in one transaction. If the second write fails, neither change remains. MongoDB only allows that on a replica set. This machine’s Windows service on port 27017 is a standalone and could not be reconfigured without administrator rights, so the app uses a single-node replica set named `rs0` on **port 27018**. In Compass, connect to `mongodb://localhost:27018` and open the database `guthi`.

## Report queries

Donor totals join donations to users, then group. `_id` is stored as an ObjectId and `donorId` as a string, so the join compares `$toString` of `_id`:

```javascript
db.donations.aggregate([
  { $lookup: {
      from: "users",
      let: { donorId: "$donorId" },
      pipeline: [
        { $match: { $expr: { $eq: [ { $toString: "$_id" }, "$$donorId" ] } } }
      ],
      as: "donor"
  }},
  { $unwind: { path: "$donor", preserveNullAndEmptyArrays: true } },
  { $group: {
      _id: "$donorId",
      donorName: { $first: "$donor.fullName" },
      donorEmail: { $first: "$donor.email" },
      totalAmount: { $sum: "$amount" },
      donationCount: { $count: {} }
  }},
  { $sort: { totalAmount: -1 } }
])
```

Spending is a plain group:

```javascript
db.donation_allocations.aggregate([
  { $group: {
      _id: "$purpose",
      totalAmount: { $sum: "$amountUsed" },
      allocationCount: { $count: {} }
  }},
  { $sort: { totalAmount: -1 } }
])
```

The Guthi workspace shows both under **Report**. The pool line is `total donated − total allocated`.

## Workflows to demonstrate

1. Register or sign in as a community member and submit a request or donation claim.
2. Restart the API. The same document is still in Compass and in the UI.
3. Submit a duplicate email or a duplicate UPI transaction id. The API rejects it and the stored count does not change.
4. Sign in as `priya@guthi.local` and open `/guthi`. The server refuses the admin pages.
5. As `admin@guthi.local`, open **Report** and explain the `$lookup` and `$group` pipelines above.
6. Approve a pending claim. Compass shows a new `donations` document and the claim status `APPROVED` together. Cancelling the confirm dialog writes nothing.

Sample logins after the dev seed: `admin@guthi.local` and `priya@guthi.local`, password `ChangeMe123!`.

## Test cases

| Case | Input | Expected | What to observe |
|---|---|---|---|
| Valid save | New community registration with a fresh email | 200, user document in `users` | Still present after restarting the API |
| Invalid input | Fund request larger than the pool, or land rent with no plot name | 400, message on screen | No new `community_requests` row |
| Duplicate | Same email again, or the same UPI transaction id | 409 or 400 | Original document unchanged |
| Wrong role | Community token calling `GET /api/admin/reports/contributions` | 403 | Report page is not available in the community portal |
| Confirm cancel | Click Reject, then Cancel | No request | Claim or request status stays `PENDING` |
| Update own pending request | Member edits a pending request and saves | 200, same document id, new fields | Approved or rejected rows have no Edit button; another member's request is refused |
| Delete own pending request | Member clicks Withdraw and confirms | 204, document gone | Cancel on the confirm dialog leaves the document |
| Linked report | Open `/guthi/report` | Donor names and purpose totals | Names come from `users` via `$lookup`, totals from `$sum` |

Screenshots for the submission: community home (pool), a request form, Compass showing the matching document, the Guthi report page, and an error for a duplicate or an over-large fund request.
