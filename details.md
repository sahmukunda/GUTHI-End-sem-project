# Guthi codebase — what the features mean

This project is a digital stand-in for a Nepali Guthi: a community trust that holds shared money and land, organizes rituals and festivals, and expects members to give both cash and labor. The app splits that into two portals and five community features.

## What a Guthi is, and how the app maps it

Historically a Guthi (especially in the Kathmandu Valley) is an endowed institution. Members, the guthiyars, maintain a temple or deity, run jatras, support ancestor rites, and use Guthi land whose income pays for those duties. Decisions sit with the Guthi, not with one person.

The software models that as two roles:

| In the app | Role | Guthi meaning |
|---|---|---|
| Community member (`USER`, `/u`) | Open registration | Someone who donates, asks for help, volunteers, and joins festivals |
| Guthi member (`ADMIN`, `/guthi`) | Invite-code registration | The caretaker who approves money, land, events, and festivals |

That matches the tradition of a closed managing body and a wider community that participates. It does not model lineage, clan membership, or the older types of Guthi (deity Guthi, death-rite Guthi, private Guthi, state Guthi). Anyone can join as a community member.

## The five features, in Guthi terms

### 1. Shared fund (donations)

Real Guthis run on pooled contributions, not individual wallets. Here that is one community pool:

- A member pays a preset amount (₹50, ₹1,000–₹5,000), submits a UPI transaction id, and waits.
- A Guthi member verifies the claim. Only then does it become a real donation and raise the pool.
- The home screen shows available pool, your verified gifts, claims still waiting, and how much you have already drawn from the pool.
- Updates arrive live over WebSocket, so the balance moves without a refresh.

Demo notes in the seed data are the cultural content: Dashain, Tihar, Indra Jatra, ancestor anniversary, temple upkeep, repair of the Guthi ghar.

### 2. Asking the Guthi (community requests)

Members do not spend the fund themselves. They petition, and the Guthi decides. Four request types:

- **Land rent** — Guthi land (guthi jagga). Needs a plot name and a lease period. Seed examples are a rice plot in Patan and a vegetable field in Bhaktapur. A rejection note says the land is already reserved for ritual use that year, which is how real Guthi land was allocated.
- **Ritual help** — people, money, or both. Seed cases are Pitri Tarpan at Pashupatinath (8 people) and a Shraddha (priest fees and offerings, ₹3,500).
- **Infrastructure fund** — the Guthi house. Seed case: monsoon cracks in the western wall of the Guthi ghar, ₹12,000.

If money is requested, the amount cannot exceed the pool, both when the member submits and again when the Guthi member approves. Approval writes a `DonationAllocation` (purpose, amount, who approved it) and lowers the shared balance. Rejection stores an admin note. Land and “people only” requests do not move money.

### 3. Labor, not only cash (volunteer events)

Guthi duty is also physical work: cleaning the courtyard, setting up a jatra, looking after elders. Guthi members create events with a headcount and a date. Community members sign up with a name and mobile number, once per event. After the date passes, the event closes itself and signups stop. The Guthi workspace sees the live signup list.

### 4. Festivals and jatras

Guthi members publish a festival (name, description, date) and later mark it complete. Reviews open only after that, and each member can review a festival once. Seed calendar: Dashain (tika and feast), Tihar (lights, Laxmi puja, Bhai Tika), Indra Jatra (Kumari procession at Basantapur), Holi in the Guthi courtyard. Completed festivals carry reviews; upcoming ones do not.

### 5. Skilled help inside the community (work posts)

Members post a job with a phone number; someone else accepts it, but not their own post. Seed posts are Newar-specific: restoring traditional wooden windows, a truck for Indra Jatra supplies, a photographer for a Bratabandha. This is mutual aid inside the community, separate from the shared fund.

## How the pieces fit together

```text
Community member                         Guthi member
─────────────────                        ────────────
donate (UPI claim)          ──────►      verify claim → pool grows
request land / ritual /    ──────►      approve → allocation, or reject
  house repair
sign up for work            ◄──────      create volunteer events
review a festival           ◄──────      publish festival, mark complete
post or accept local work   (peer to peer, no admin)
```

Money only moves on two admin actions: approving a donation claim (money in) and approving a funded request (money out). Everything else is coordination.

## What the code actually is

Spring Boot 3 and MongoDB on port 9090, React and TypeScript on port 5173. JWT sessions, two login paths (`/login/user` and `/login/guthi`). Live topics cover the pool, personal donation totals, claims, requests, volunteer lists, work posts, festivals, and reviews.

Dev seed (`DevDataLoader`) fills Mongo with those Valley examples so the screens are not empty: Patan and Bhaktapur land, Pashupatinath rites, Guthi ghar repair, Dashain through Holi.

## Where it stays a demo of the tradition

- **One pool, two roles.** A real Guthi often has separate funds per deity or festival, and offices (thakali, nayo, and so on), not a single admin flag.
- **Land is a form, not a register.** There is no plot inventory, rent collected back into the fund, or multi-year lease ledger.
- **Payments are Indian rails.** Amounts display as ₹, and the QR flow is UPI / PhonePe (`cu=INR`). A Nepal deployment would be NPR with Fonepay, eSewa, or Khalti.
- **Rituals are free text.** Pitri Tarpan and Shraddha appear in sample data, not as their own workflows. Death-rite Guthis (si guthi), priest assignment, and deity inventories are absent.
- **Volunteer reviews are unused.** `VolunteerEventReview` and its repository exist, and the dev reseed clears that collection, but no service writes reviews for volunteer events. Festival reviews are the ones that work.
- **Guthi members do not post work.** Work posts live only on the community side.

The strongest cultural fit is the governance loop: shared money, Guthi land, ritual and house spending that must be approved, festival calendar, and member labor. The weakest fit is identity and money rails — open signup instead of guthiyar lineage, and INR/UPI instead of Nepali currency and payments.
