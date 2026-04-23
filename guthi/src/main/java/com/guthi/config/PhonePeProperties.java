package com.guthi.config;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

/**
 * PhonePe Dynamic QR (DQR) — merchant credentials from onboarding.
 *
 * @see <a href="https://developer.phonepe.com/offline-integration/dynamic-qr-solution/introduction">Dynamic QR introduction</a>
 * @see <a href="https://developer.phonepe.com/offline-integration/dynamic-qr-solution/dqr-init-api">DQR Init API</a>
 */
@Slf4j
@Getter
@Setter
@ConfigurationProperties(prefix = "guthi.phonepe")
public class PhonePeProperties {

    /** When false, donation QR uses the static demo URL (no PhonePe call). */
    private boolean enabled = false;

    /**
     * UAT: https://mercury-uat.phonepe.com/enterprise-sandbox
     * Prod: https://mercury-t2.phonepe.com
     */
    private String baseUrl = "https://mercury-uat.phonepe.com/enterprise-sandbox";

    private String merchantId = "";
    private String saltKey = "";
    /** Salt key index from PhonePe dashboard, e.g. 1 */
    private String saltIndex = "1";

    private String storeId = "";
    private String terminalId = "";

    /** QR / payment validity in seconds (API field {@code expiresIn}). */
    private long defaultExpiresSeconds = 1800;

    /** Default amount in paise when the client does not pass {@code amountPaise}. */
    private long defaultAmountPaise = 100;

    /** Optional S2S callback URL ({@code X-CALLBACK-URL}). */
    private String callbackUrl = "";

    @PostConstruct
    void logPhonePeBinding() {
        if (!enabled) {
            log.debug(
                    "PhonePe DQR is off. Set guthi.phonepe.enabled=true (or env GUTHI_PHONEPE_ENABLED=true) and merchant"
                            + " fields to generate QRs against your PhonePe merchant account.");
            return;
        }
        boolean complete =
                StringUtils.hasText(merchantId)
                        && StringUtils.hasText(saltKey)
                        && StringUtils.hasText(saltIndex)
                        && StringUtils.hasText(storeId);
        if (!complete) {
            log.warn(
                    "PhonePe DQR is enabled but merchantId, saltKey, saltIndex, or storeId is missing —"
                            + " QR generation will fail until all are set.");
            return;
        }
        String tail =
                merchantId.length() <= 4
                        ? "(short id)"
                        : "…" + merchantId.substring(merchantId.length() - 4);
        log.info(
                "PhonePe DQR ready for merchant id ending {} — baseUrl={}, storeId={}, saltIndex={}, callbackUrl set={}",
                tail,
                baseUrl,
                storeId,
                saltIndex,
                StringUtils.hasText(callbackUrl));
    }
}
