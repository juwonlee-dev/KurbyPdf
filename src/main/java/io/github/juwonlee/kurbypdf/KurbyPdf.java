package io.github.juwonlee.kurbypdf;

import org.apache.pdfbox.pdmodel.PDDocument;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Base64;
import java.util.Map;

import io.github.juwonlee.kurbypdf.util.KeyUtil;

public class KurbyPdf {
    public static WatermarkResult watermark(byte[] inputPdf, WatermarkRequest req) throws Exception {
        if (inputPdf == null) throw new IllegalArgumentException("inputPdf == null");
        if (req == null) throw new IllegalArgumentException("req == null");
        if (req.getClaims() == null) throw new IllegalArgumentException("claims == null");

        ByteArrayInputStream bin = new ByteArrayInputStream(inputPdf);
        PDDocument doc = null;
        try {
            doc = PDDocument.load(bin);

            // 1) 페이로드 준비 (JSON 고정순서 + HMAC + AES-GCM)
            byte[] hmacKey = Base64.getDecoder().decode(req.getHmacKeyBase64());
            byte[] aesKey  = Base64.getDecoder().decode(req.getPayloadAesKeyBase64());
            Map<String, String> claims = req.getClaims();

            byte[] payload = WatermarkPayload.buildPayload(claims, hmacKey, aesKey);

            // 2) 문서 레벨 숨김 워터마크 (폼 XObject 참조, 페이지당 다중 삽입)
            PdfForensicEmbedder.embed(doc, payload); // 기본: 페이지당 2개, 랜덤 위치

            // 3) 암호/권한 (옵션)
            String ownerPwd = null;
            if (req.getUserPassword() != null && !req.getUserPassword().isEmpty()) {
                ownerPwd = req.getOwnerPassword();
                if (ownerPwd == null || ownerPwd.isEmpty()) {
                    ownerPwd = KeyUtil.randomOwnerPassword();
                }
                PdfProtector.applyUserPassword(doc, req.getUserPassword(), ownerPwd);
            }

            ByteArrayOutputStream bout = new ByteArrayOutputStream();
            doc.save(bout);
            return new WatermarkResult(bout.toByteArray(), ownerPwd);

        } finally {
            if (doc != null) try { doc.close(); } catch (Exception ignore) {}
            try { bin.close(); } catch (Exception ignore) {}
        }
    }
}
