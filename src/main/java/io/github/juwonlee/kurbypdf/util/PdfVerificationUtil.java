package io.github.juwonlee.kurbypdf.util;

import io.github.juwonlee.kurbypdf.PdfWatermarkInspector;

import java.util.Base64;
import java.util.Collections;
import java.util.Map;

/**
 * 문서 레벨 히든 워터마크 검증 유틸.
 * - verify(...) : 단순 성공/실패(boolean)
 * - verifyDetailed(...) : 실패 사유/클레임/타임스탬프 등 상세 결과 반환
 */
public class PdfVerificationUtil {
    private PdfVerificationUtil() {}

    /** 단순 검증(Boolean) */
    public static boolean verify(byte[] pdfBytes, String userPassword, byte[] hmacKey, byte[] aesKey) {
        Result r = verifyDetailed(pdfBytes, userPassword, hmacKey, aesKey);
        return r.isValid();
    }

    /** Base64 키 입력 지원 오버로드 */
    public static boolean verifyBase64(byte[] pdfBytes, String userPassword, String hmacKeyBase64, String aesKeyBase64) {
        if (hmacKeyBase64 == null || aesKeyBase64 == null) return false;
        byte[] hmac = Base64.getDecoder().decode(hmacKeyBase64);
        byte[] aes  = Base64.getDecoder().decode(aesKeyBase64);
        return verify(pdfBytes, userPassword, hmac, aes);
    }

    /** 상세 검증: 실패사유/클레임/서명유효성 등 포함 */
    public static Result verifyDetailed(byte[] pdfBytes, String userPassword, byte[] hmacKey, byte[] aesKey) {
        if (pdfBytes == null || pdfBytes.length == 0) {
            return Result.fail("EMPTY_PDF");
        }
        if (hmacKey == null || aesKey == null) {
            return Result.fail("MISSING_KEYS");
        }
        try {
            PdfWatermarkInspector.DecodedWatermark dw = PdfWatermarkInspector.extractFirst(pdfBytes, hmacKey, aesKey, userPassword);

            if (dw == null) {
                return Result.fail("WATERMARK_NOT_FOUND");
            }
            if (!dw.isSignatureValid()) {
                return Result.fail("INVALID_SIGNATURE");
            }
            return Result.ok(
                    dw.getClaims() != null ? dw.getClaims() : Collections.<String,String>emptyMap(),
                    dw.getTimestampMillis(),
                    dw.getNonce()
            );
        } catch (Exception e) {
            return Result.fail("EXCEPTION:" + e.getClass().getSimpleName());
        }
    }

    /** 결과 DTO */
    public static final class Result {
        private final boolean valid;
        private final String reason;          // 실패 사유 코드(성공 시 null)
        private final Map<String,String> claims;
        private final long timestampMillis;
        private final String nonce;

        private Result(boolean valid, String reason, Map<String,String> claims, long ts, String nonce) {
            this.valid = valid;
            this.reason = reason;
            this.claims = claims;
            this.timestampMillis = ts;
            this.nonce = nonce;
        }

        static Result ok(Map<String,String> claims, long ts, String nonce) {
            return new Result(true, null, claims, ts, nonce);
        }

        static Result fail(String reason) {
            return new Result(false, reason, Collections.<String,String>emptyMap(), 0L, null);
        }

        public boolean isValid() { return valid; }
        public String getReason() { return reason; }
        public Map<String,String> getClaims() { return claims; }
        public long getTimestampMillis() { return timestampMillis; }
        public String getNonce() { return nonce; }
    }
}
