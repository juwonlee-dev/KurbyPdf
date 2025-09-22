package io.github.juwonlee.kurbypdf;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.cos.COSStream;
import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.graphics.PDXObject;
import org.apache.pdfbox.pdmodel.graphics.form.PDFormXObject;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.security.MessageDigest;
import java.util.*;

public class PdfWatermarkInspector {
    private static final ObjectMapper OM = new ObjectMapper().configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);
    private static final COSName WM_KEY = COSName.getPDFName("_k1");
    private static final int IV_LEN = 12;

    public static DecodedWatermark extractFirst(byte[] pdf, byte[] hmacKey, byte[] aesKey) throws Exception {
        return extractFirst(pdf, hmacKey, aesKey, null);
    }

    public static DecodedWatermark extractFirst(byte[] pdf, byte[] hmacKey, byte[] aesKey, String password) throws Exception {
        List<DecodedWatermark> list = extractAll(pdf, hmacKey, aesKey, password);
        return list.isEmpty() ? null : list.get(0);
    }

    public static List<DecodedWatermark> extractAll(byte[] pdf, byte[] hmacKey, byte[] aesKey) throws Exception {
        return extractAll(pdf, hmacKey, aesKey, null);
    }

    public static List<DecodedWatermark> extractAll(byte[] pdf, byte[] hmacKey, byte[] aesKey, String password) throws Exception {
        if (pdf == null) throw new IllegalArgumentException("pdf == null");

        ByteArrayInputStream bin = new ByteArrayInputStream(pdf);
        PDDocument doc = null;
        try {
            doc = (password != null && !password.isEmpty()) ? PDDocument.load(bin, password) : PDDocument.load(bin);

            Set<COSStream> seen = new HashSet<COSStream>();
            List<DecodedWatermark> out = new ArrayList<DecodedWatermark>();

            for (PDPage page : doc.getPages()) {
                PDResources res = page.getResources();
                if (res == null) continue;

                for (COSName name : res.getXObjectNames()) {
                    PDXObject xo = res.getXObject(name);
                    if (xo instanceof PDFormXObject) {
                        PDFormXObject form = (PDFormXObject) xo;
                        Object wm = form.getCOSObject().getDictionaryObject(WM_KEY);
                        if (wm instanceof COSStream) {
                            COSStream stream = (COSStream) wm;
                            if (!seen.contains(stream)) {
                                seen.add(stream);
                                byte[] enc = streamToBytes(stream);
                                DecodedWatermark rec = decryptAndVerify(enc, hmacKey, aesKey);
                                out.add(rec);
                            }
                        }
                    }
                }
            }
            return out;

        } finally {
            if (doc != null) try { doc.close(); } catch (Exception ignore) {}
            try { bin.close(); } catch (Exception ignore) {}
        }
    }

    private static byte[] streamToBytes(COSStream s) throws Exception {
        InputStream in = null;
        try {
            in = s.createInputStream();
            byte[] buf = new byte[8192];
            int r;
            ByteArrayOutputStream bout = new ByteArrayOutputStream();
            while ((r = in.read(buf)) != -1) {
                bout.write(buf, 0, r);
            }
            return bout.toByteArray();
        } finally {
            if (in != null) try { in.close(); } catch (Exception ignore) {}
        }
    }

    private static DecodedWatermark decryptAndVerify(byte[] blob, byte[] hmacKey, byte[] aesKey) throws Exception {
        if (blob == null || blob.length < IV_LEN + 1)
            throw new IllegalArgumentException("invalid blob");

        byte[] iv = Arrays.copyOfRange(blob, 0, IV_LEN);
        byte[] ct = Arrays.copyOfRange(blob, IV_LEN, blob.length);

        Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
        c.init(Cipher.DECRYPT_MODE, new SecretKeySpec(aesKey, "AES"), new GCMParameterSpec(128, iv));
        byte[] json = c.doFinal(ct);

        JsonNode root = OM.readTree(json);

        // claims를 키순으로 재구성
        Map<String,String> claims = new TreeMap<String, String>();
        JsonNode claimsNode = root.get("claims");
        if (claimsNode != null && claimsNode.isObject()) {
            Iterator<String> it = claimsNode.fieldNames();
            while (it.hasNext()) {
                String k = it.next();
                JsonNode v = claimsNode.get(k);
                claims.put(k, (v == null || v.isNull()) ? null : v.asText());
            }
        }

        // body도 항상 같은 키 순서로
        Map<String,Object> body = new LinkedHashMap<String, Object>();
        body.put("v", root.path("v").asInt(1));
        body.put("claims", claims);
        body.put("ts", root.path("ts").asLong());
        body.put("nonce", root.path("nonce").asText(""));

        byte[] bodyJson = OM.writeValueAsBytes(body);

        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(hmacKey, "HmacSHA256"));
        byte[] expect = mac.doFinal(bodyJson);

        String sigB64 = root.path("sig").asText("");
        byte[] got = java.util.Base64.getDecoder().decode(sigB64);
        boolean valid = MessageDigest.isEqual(expect, got); // Arrays.equals(expect, got); // 타이밍 공격 방어

        return new DecodedWatermark(claims, root.path("ts").asLong(), root.path("nonce").asText(""), valid);
    }

    public static class DecodedWatermark {
        private final Map<String,String> claims;
        private final long timestampMillis;
        private final String nonce;
        private final boolean signatureValid;

        public DecodedWatermark(Map<String, String> claims, long timestampMillis, String nonce, boolean signatureValid) {
            this.claims = claims;
            this.timestampMillis = timestampMillis;
            this.nonce = nonce;
            this.signatureValid = signatureValid;
        }
        public Map<String, String> getClaims() { return claims; }
        public long getTimestampMillis() { return timestampMillis; }
        public String getNonce() { return nonce; }
        public boolean isSignatureValid() { return signatureValid; }
    }
}
