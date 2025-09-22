package io.github.juwonlee.kurbypdf;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.encryption.AccessPermission;
import org.apache.pdfbox.pdmodel.encryption.StandardProtectionPolicy;

public class PdfProtector {
    public static void applyUserPassword(PDDocument doc, String userPassword, String ownerPassword) throws Exception {
        AccessPermission ap = new AccessPermission();
        ap.setCanPrint(true);
        ap.setCanExtractContent(false);

        StandardProtectionPolicy spp = new StandardProtectionPolicy(ownerPassword, userPassword, ap);
        spp.setEncryptionKeyLength(256);
        spp.setPreferAES(true);
        doc.protect(spp);
    }
}
