package io.github.juwonlee.kurbypdf;

public class WatermarkResult {
    private final byte[] pdfBytes;
    private final String ownerPassword;

    public WatermarkResult(byte[] pdfBytes, String ownerPassword) {
        this.pdfBytes = pdfBytes;
        this.ownerPassword = ownerPassword;
    }

    public byte[] getPdfBytes() { return pdfBytes; }
    public String getOwnerPassword() { return ownerPassword; }
}
