package io.github.juwonlee.kurbypdf;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.util.*;

public class WatermarkPayload {
    private static final ObjectMapper OM = new ObjectMapper().configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);;
    private static final SecureRandom RND = new SecureRandom();
    private static final String HMAC_ALG = "HmacSHA256";
    private static final String AES_ALG  = "AES/GCM/NoPadding";
    private static final int GCM_TAG_BITS = 128;
    private static final int IV_LEN = 12;

    public static byte[] buildPayload(Map<String, String> claimsIn, byte[] hmacKey, byte[] aesKey) throws Exception {
        // 1) claims를 키순 정렬 (null 허용 시 null→"null"이 아닌 그대로 두고 싶으면 가공 금지)
        Map<String,String> claims = new TreeMap<String, String>();
        if (claimsIn != null) claims.putAll(claimsIn);

        // 2) body를 항상 같은 순서로 구성
        Map<String,Object> body = new LinkedHashMap<String, Object>();
        body.put("v", 1);
        body.put("claims", claims);
        body.put("ts", System.currentTimeMillis());
        body.put("nonce", Long.toUnsignedString(RND.nextLong()));

        byte[] bodyJson = OM.writeValueAsBytes(body);

        // 3) HMAC 서명
        Mac mac = Mac.getInstance(HMAC_ALG);
        mac.init(new SecretKeySpec(hmacKey, "HmacSHA256"));
        byte[] sig = mac.doFinal(bodyJson);

        // 4) wrap = body + sig (wrap의 키 순서도 고정)
        Map<String,Object> wrap = new LinkedHashMap<String, Object>();
        wrap.put("v", body.get("v"));
        wrap.put("claims", body.get("claims"));
        wrap.put("ts", body.get("ts"));
        wrap.put("nonce", body.get("nonce"));
        wrap.put("sig", Base64.getEncoder().encodeToString(sig));

        byte[] packed = OM.writeValueAsBytes(wrap);

        // 5) AES-GCM 암호화: out = IV(12) || CT
        byte[] iv = new byte[IV_LEN];
        RND.nextBytes(iv);

        Cipher c = Cipher.getInstance(AES_ALG);
        c.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(aesKey, "AES"), new GCMParameterSpec(GCM_TAG_BITS, iv));
        byte[] ct = c.doFinal(packed);

        byte[] out = new byte[IV_LEN + ct.length];
        System.arraycopy(iv, 0, out, 0, IV_LEN);
        System.arraycopy(ct, 0, out, IV_LEN, ct.length);
        return out;
    }
}
