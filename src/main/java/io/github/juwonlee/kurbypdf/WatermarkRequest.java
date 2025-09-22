package io.github.juwonlee.kurbypdf;

import java.util.Map;

public class WatermarkRequest {
    private String hmacKeyBase64;
    private String payloadAesKeyBase64;
    private Map<String, String> claims;

    private String userPassword;   // optional
    private String ownerPassword;  // optional (null/"" -> 랜덤)

    public WatermarkRequest() {
    }

    public WatermarkRequest(String hmacKeyBase64,
                            String payloadAesKeyBase64,
                            Map<String, String> claims,
                            String userPassword,
                            String ownerPassword) {
        this.hmacKeyBase64 = hmacKeyBase64;
        this.payloadAesKeyBase64 = payloadAesKeyBase64;
        this.claims = claims;
        this.userPassword = userPassword;
        this.ownerPassword = ownerPassword;
    }

    public String getHmacKeyBase64() { return hmacKeyBase64; }
    public String getPayloadAesKeyBase64() { return payloadAesKeyBase64; }
    public Map<String, String> getClaims() { return claims; }
    public String getUserPassword() { return userPassword; }
    public String getOwnerPassword() { return ownerPassword; }
}
