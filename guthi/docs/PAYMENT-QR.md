# Donation payment QR (production vs local)

The donations page generates a **PNG QR** whose payload is either a **PhonePe Dynamic QR** UPI string or a **fallback** UPI / https link, depending on configuration.

When PhonePe is enabled with valid merchant fields, **every** `GET /api/donations/qr?amountPaise=…` uses **your** PhonePe account (from config) and the [DQR Init API](https://developer.phonepe.com/offline-integration/dynamic-qr-solution/dqr-init-api): the returned `qrString` is an `upi://…` intent with the **amount embedded in paise** as PhonePe defines it.

## Production: PhonePe Dynamic QR (DQR)

Use this when you have completed **PhonePe merchant onboarding** and want a real, amount-locked collect flow with PhonePe reconciliation and callbacks.

1. Set in `application.yml` (or environment-specific config / secrets — **do not commit production salt keys**):

   ```yaml
   guthi:
     phonepe:
       enabled: true
       # UAT: https://mercury-uat.phonepe.com/enterprise-sandbox
       # Prod: https://mercury-t2.phonepe.com
       base-url: https://mercury-t2.phonepe.com
       merchant-id: "<from PhonePe>"
       salt-key: "<from PhonePe dashboard>"
       salt-index: "<key index, e.g. 1>"
       store-id: "<unique store id, rules in PhonePe docs>"
       terminal-id: ""   # optional
       default-expires-seconds: 1800
       default-amount-paise: 100
       callback-url: "https://your-api.example.com/api/webhooks/phonepe"   # S2S callback URL
   ```

2. Implement and expose the **callback URL** on your server so paid donations can be confirmed and the pool updated (this repo may still need that webhook controller wired to your domain).

3. API reference: [Dynamic QR introduction](https://developer.phonepe.com/offline-integration/dynamic-qr-solution/introduction) and [DQR Init API](https://developer.phonepe.com/offline-integration/dynamic-qr-solution/dqr-init-api).

With `enabled: true` and valid merchant fields, `GET /api/donations/qr?amountPaise=...` calls **DQR Init**, receives **`qrString`** (`upi://…` with amount), and ZXing renders the PNG.

On app startup you should see a log line: **`PhonePe DQR ready for merchant id ending …`** — that confirms your credentials are bound. If you see a **warning** about missing fields, fix YAML or env vars and restart.

### Credentials via environment variables (recommended)

You can keep secrets out of files. Spring Boot maps these to `guthi.phonepe.*` (relaxed binding):

| Environment variable | Purpose |
| --- | --- |
| `GUTHI_PHONEPE_ENABLED` | `true` / `false` |
| `GUTHI_PHONEPE_BASE_URL` | UAT or prod base URL |
| `GUTHI_PHONEPE_MERCHANT_ID` | Merchant ID from PhonePe |
| `GUTHI_PHONEPE_SALT_KEY` | Salt key from dashboard |
| `GUTHI_PHONEPE_SALT_INDEX` | Key index, e.g. `1` |
| `GUTHI_PHONEPE_STORE_ID` | Store ID (per PhonePe rules) |
| `GUTHI_PHONEPE_TERMINAL_ID` | Optional |
| `GUTHI_PHONEPE_CALLBACK_URL` | Optional `X-CALLBACK-URL` for webhooks |

After setting env vars, restart Spring Boot. Open **Donations**, enter amount in ₹, click **Donate** — the UI requests the QR with `amountPaise` (rupees × 100), so the QR matches that amount on your merchant account.

## Local / staging without PhonePe: personal UPI (offline payee)

If PhonePe is **disabled**, you can still get a **real UPI QR** (amount prefilled in PhonePe / GPay / Paytm, etc.) by pointing the QR at **your own VPA** for testing:

```yaml
guthi:
  phonepe:
    enabled: false
  donation:
    offline-payee-upi: "yourname@oksbi"   # use your real test UPI id
```

The backend encodes a standard **`upi://pay?pa=…&am=…&cu=INR`** intent (amount in rupees derived from `amountPaise`). No `https://guthi.np/...` redirect.

Leave `offline-payee-upi` **empty** only if you intentionally want the legacy **https** demo link (not suitable for real UPI testing).

## Preset PNG QRs (`guthi/qra/`)

The community **Donations** UI can show fixed images (`50.png`, `1.png`–`5.png`) served from the **`guthi/qra`** folder (see `guthi/qra/README.md`). Run the API from the **`guthi`** directory so `file:./qra/` resolves correctly, or add files under `src/main/resources/static/qra/` as a fallback classpath location.

## Secrets and profiles

- Prefer **environment variables** or a **secrets manager** for `salt-key` and JWT secrets in production.
- Use a **dev** profile (`application-dev.yml`) for UAT PhonePe credentials; keep production values out of git.
