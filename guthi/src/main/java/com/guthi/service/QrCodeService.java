package com.guthi.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.guthi.config.DonationQrProperties;
import com.guthi.config.PhonePeProperties;
import com.guthi.exception.BadRequestException;
import com.guthi.exception.PhonePeIntegrationException;
import com.guthi.integration.phonepe.PhonePeDqrClient;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class QrCodeService {

    private static final int QR_SIZE = 300;

    private final PhonePeProperties phonePe;
    private final DonationQrProperties donationQr;
    private final PhonePeDqrClient phonePeDqrClient;

    /**
     * PNG QR image. When {@code guthi.phonepe.enabled} is true and merchant settings are present,
     * calls PhonePe Dynamic QR Init and encodes the returned UPI {@code qrString}. Otherwise, if
     * {@code guthi.donation.offline-payee-upi} is set, encodes a standard {@code upi://pay} intent with
     * {@code am} in rupees. Else encodes a legacy https://guthi.np/ demo link (not UPI; may redirect).
     *
     * @param amountPaise amount in paise for the dynamic QR; if null, uses {@code defaultAmountPaise}
     * @param message optional payer-visible message for PhonePe
     * @see <a href="https://developer.phonepe.com/offline-integration/dynamic-qr-solution/introduction">PhonePe Dynamic QR</a>
     */
    public byte[] generateDonationQr(Long amountPaise, String message) throws Exception {
        if (amountPaise != null && amountPaise < 1) {
            throw new BadRequestException("amountPaise must be at least 1 (amount in paise)");
        }
        long paise = amountPaise != null ? amountPaise : phonePe.getDefaultAmountPaise();
        String content;
        if (phonePe.isEnabled()) {
            if (!hasMerchantCredentials()) {
                throw new PhonePeIntegrationException(
                        "PhonePe is enabled: set merchantId, saltKey, saltIndex, and storeId (see guthi.phonepe.*)");
            }
            content = phonePeDqrClient.initQrString(paise, message);
        } else {
            if (StringUtils.hasText(donationQr.getOfflinePayeeUpi())) {
                content = buildUpiPayIntent(donationQr.getOfflinePayeeUpi(), paise, message);
            } else if (amountPaise != null) {
                content =
                        "https://guthi.np/donate?org=guthi-main&amountPaise="
                                + amountPaise
                                + (StringUtils.hasText(message)
                                        ? "&note=" + URLEncoder.encode(message, StandardCharsets.UTF_8)
                                        : "");
            } else {
                content = "https://guthi.np/donate?org=guthi-main";
            }
        }
        return encodeQrPng(content);
    }

    /**
     * Standard UPI collect URL so scanning opens a payment app with amount prefilled (no PhonePe
     * API required). {@code pa} must be a valid payee address format.
     */
    static String buildUpiPayIntent(String payeeAddress, long paise, String message) {
        String am =
                BigDecimal.valueOf(paise)
                        .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP)
                        .toPlainString();
        StringBuilder sb = new StringBuilder("upi://pay?pa=");
        sb.append(URLEncoder.encode(payeeAddress.trim(), StandardCharsets.UTF_8));
        sb.append("&pn=").append(URLEncoder.encode("Guthi donation", StandardCharsets.UTF_8));
        sb.append("&am=").append(URLEncoder.encode(am, StandardCharsets.UTF_8));
        sb.append("&cu=INR");
        if (StringUtils.hasText(message)) {
            String tn = message.length() > 80 ? message.substring(0, 80) : message;
            sb.append("&tn=").append(URLEncoder.encode(tn, StandardCharsets.UTF_8));
        }
        return sb.toString();
    }

    private boolean hasMerchantCredentials() {
        return StringUtils.hasText(phonePe.getMerchantId())
                && StringUtils.hasText(phonePe.getSaltKey())
                && StringUtils.hasText(phonePe.getSaltIndex())
                && StringUtils.hasText(phonePe.getStoreId());
    }

    private byte[] encodeQrPng(String content) throws Exception {
        QRCodeWriter writer = new QRCodeWriter();
        BitMatrix matrix = writer.encode(content, BarcodeFormat.QR_CODE, QR_SIZE, QR_SIZE);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        MatrixToImageWriter.writeToStream(matrix, "PNG", out);
        return out.toByteArray();
    }
}
