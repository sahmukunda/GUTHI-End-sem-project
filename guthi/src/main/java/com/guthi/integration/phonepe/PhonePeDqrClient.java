package com.guthi.integration.phonepe;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.guthi.config.PhonePeProperties;
import com.guthi.exception.BadRequestException;
import com.guthi.exception.PhonePeIntegrationException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

/**
 * PhonePe Dynamic QR init — returns a per-transaction UPI payload string ({@code qrString}).
 *
 * @see <a href="https://developer.phonepe.com/offline-integration/dynamic-qr-solution/dqr-init-api">DQR Init API</a>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PhonePeDqrClient {

    private static final String INIT_PATH = "/v3/qr/init";

    private final PhonePeProperties phonePe;
    private final ObjectMapper objectMapper;
    private final RestClient restClient = RestClient.create();

    /**
     * Calls DQR Init and returns the {@code qrString} to render as a QR image.
     *
     * @param amountPaise amount in paise (must be positive)
     * @param message optional note shown to payer (may be truncated by PhonePe)
     */
    public String initQrString(long amountPaise, String message) {
        validateConfig();

        if (amountPaise <= 0) {
            throw new BadRequestException("amountPaise must be greater than zero");
        }

        String transactionId = newTransactionId();
        long expires = phonePe.getDefaultExpiresSeconds();

        PhonePeDqrInitInnerRequest inner =
                PhonePeDqrInitInnerRequest.builder()
                        .merchantId(phonePe.getMerchantId())
                        .transactionId(transactionId)
                        .merchantOrderId(transactionId)
                        .amount(amountPaise)
                        .storeId(phonePe.getStoreId())
                        .terminalId(StringUtils.hasText(phonePe.getTerminalId()) ? phonePe.getTerminalId() : null)
                        .expiresIn(expires)
                        .message(StringUtils.hasText(message) ? message : "Guthi donation")
                        .build();

        String innerJson;
        try {
            innerJson = objectMapper.writeValueAsString(inner);
        } catch (Exception e) {
            throw new PhonePeIntegrationException("Failed to serialize PhonePe request", e);
        }

        String base64Payload =
                java.util.Base64.getEncoder().encodeToString(innerJson.getBytes(StandardCharsets.UTF_8));
        String xVerify = buildXVerifyHeader(base64Payload);

        String url = trimTrailingSlash(phonePe.getBaseUrl()) + INIT_PATH;
        PhonePeDqrApiRequest body = new PhonePeDqrApiRequest(base64Payload);

        try {
            PhonePeDqrInitResponse response =
                    restClient
                            .post()
                            .uri(url)
                            .contentType(MediaType.APPLICATION_JSON)
                            .headers(
                                    h -> {
                                        h.set("X-VERIFY", xVerify);
                                        h.set("X-CALL-MODE", "POST");
                                        if (StringUtils.hasText(phonePe.getCallbackUrl())) {
                                            h.set("X-CALLBACK-URL", phonePe.getCallbackUrl());
                                        }
                                    })
                            .body(body)
                            .retrieve()
                            .body(PhonePeDqrInitResponse.class);

            if (response == null) {
                throw new PhonePeIntegrationException("Empty response from PhonePe");
            }
            if (!response.isSuccess()
                    || response.getData() == null
                    || !StringUtils.hasText(response.getData().getQrString())) {
                String detail =
                        String.format(
                                "code=%s message=%s",
                                response.getCode(), response.getMessage());
                throw new PhonePeIntegrationException("PhonePe DQR init failed: " + detail);
            }
            return response.getData().getQrString();
        } catch (RestClientResponseException e) {
            log.warn("PhonePe DQR HTTP error status={} body={}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new PhonePeIntegrationException("PhonePe DQR request failed: HTTP " + e.getStatusCode(), e);
        } catch (PhonePeIntegrationException e) {
            throw e;
        } catch (Exception e) {
            throw new PhonePeIntegrationException("PhonePe DQR request failed", e);
        }
    }

    private void validateConfig() {
        if (!StringUtils.hasText(phonePe.getMerchantId())
                || !StringUtils.hasText(phonePe.getSaltKey())
                || !StringUtils.hasText(phonePe.getSaltIndex())
                || !StringUtils.hasText(phonePe.getStoreId())) {
            throw new PhonePeIntegrationException(
                    "PhonePe DQR requires merchantId, saltKey, saltIndex, and storeId");
        }
    }

    /**
     * {@code SHA256(base64Payload + "/v3/qr/init" + saltKey) + "###" + saltIndex}
     */
    String buildXVerifyHeader(String base64Payload) {
        try {
            String toSign = base64Payload + INIT_PATH + phonePe.getSaltKey();
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(toSign.getBytes(StandardCharsets.UTF_8));
            String checksum = HexFormat.of().formatHex(hash);
            return checksum + "###" + phonePe.getSaltIndex();
        } catch (Exception e) {
            throw new PhonePeIntegrationException("Failed to build X-VERIFY checksum", e);
        }
    }

    static String newTransactionId() {
        // PhonePe: fewer than 35 chars; only '_' and '-' as special chars besides alphanumerics
        String hex = UUID.randomUUID().toString().replace("-", "");
        return "D" + hex.substring(0, Math.min(31, hex.length()));
    }

    static String trimTrailingSlash(String base) {
        if (base == null || base.isEmpty()) {
            return "";
        }
        return base.endsWith("/") ? base.substring(0, base.length() - 1) : base;
    }
}
