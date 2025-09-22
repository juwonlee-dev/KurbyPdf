package io.github.juwonlee.kurbypdf;

import io.github.juwonlee.kurbypdf.util.PdfVerificationUtil;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.graphics.PDXObject;
import org.apache.pdfbox.pdmodel.graphics.form.PDFormXObject;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.util.*;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class KurbyPdfTest {
    // 테스트용 키 (운영에서는 안전한 키 관리 필요)
    private static final byte[] HMAC_KEY = new byte[32];
    private static final byte[] AES_KEY  = new byte[32];

    static {
        Arrays.fill(HMAC_KEY, (byte)0x11);
        Arrays.fill(AES_KEY,  (byte)0x22);
    }

    @Test
    public void testWatermarkPdf() throws Exception {
        // 입력 PDF 로드 (src/test/resources/input/sample.pdf)
        File inFile = new File(getClass().getClassLoader().getResource("input/sample.pdf").toURI());
        byte[] originalPdf = Files.readAllBytes(inFile.toPath());

        // 원본 PDF의 래스터 이미지 개수 확인
        int originalImageCount;
        try (PDDocument tmp = PDDocument.load(new ByteArrayInputStream(originalPdf))) {
            originalImageCount = countImages(tmp);
        }
        System.out.println("[INFO] original image count = " + originalImageCount);

        // 암호화 출력 디렉토리
        File outDir = new File("build/test-out");
        if (!outDir.exists()) outDir.mkdirs();

        // 추적 클레임
        Map<String,String> claims = new LinkedHashMap<String, String>();
        claims.put("uid", "tester-001");
        claims.put("fileId", UUID.randomUUID().toString());
        claims.put("ts", Long.toString(System.currentTimeMillis()));

        // PDF 사용자 암호
        String userPwd = "testpw123!";

        WatermarkRequest req = new WatermarkRequest(
                Base64.getEncoder().encodeToString(HMAC_KEY),
                Base64.getEncoder().encodeToString(AES_KEY),
                claims,
                userPwd,            // userPassword (열람 비밀번호)
                null                // ownerPassword (null이면 내부 생성)
        );

        // watermark 호출 → 결과 DTO
        WatermarkResult result = KurbyPdf.watermark(originalPdf, req);
        byte[] processed = result.getPdfBytes();
        String ownerPwd = result.getOwnerPassword(); // 필요시 별도 로깅

        // 암호화된 결과 파일 저장
        String outName = "secured_" + System.currentTimeMillis() + ".pdf";
        File outFile = new File(outDir, outName);
        try (FileOutputStream fos = new FileOutputStream(outFile)) {
            fos.write(processed);
        }

        System.out.println("\n==== Encrypted Result ====");
        System.out.println("[OK] Encrypted PDF Generated: " + outFile.getAbsolutePath());
        System.out.println("ownerPwd=" + ownerPwd);
        System.out.println("userPwd=" + userPwd);
        System.out.println("claims=" + req.getClaims());

        // =========================
        // 1) 문서 레벨 워터마크 검증
        // =========================
        boolean isValid = PdfVerificationUtil.verify(processed, userPwd, HMAC_KEY, AES_KEY);
        assertTrue(isValid, "PDF watermark verification failed");

        // 상세 검증(옵션)
        PdfVerificationUtil.Result vr = PdfVerificationUtil.verifyDetailed(processed, userPwd, HMAC_KEY, AES_KEY);
        assertTrue(vr.isValid(), "Verification invalid: " + vr.getReason());
        assertNotNull(vr.getClaims());
        System.out.println("\n==== Verification ====");
        System.out.println("valid=" + vr.isValid());
        System.out.println("claims=" + vr.getClaims());
        System.out.println("timestamp=" + vr.getTimestampMillis());
        System.out.println("nonce=" + vr.getNonce());
    }

    // === PDF 내 래스터 이미지 개수 세기 (Form XObject 재귀 포함) ===
    private int countImages(PDDocument doc) throws Exception {
        int[] count = new int[] {0};
        for (PDPage page : doc.getPages()) {
            PDResources res = page.getResources();
            if (res != null) countImagesInResources(res, count);
        }
        return count[0];
    }

    private void countImagesInResources(PDResources res, int[] count) throws Exception {
        for (COSName name : res.getXObjectNames()) {
            PDXObject xo = res.getXObject(name);
            if (xo instanceof PDImageXObject) {
                count[0]++;
            } else if (xo instanceof PDFormXObject) {
                PDFormXObject form = (PDFormXObject) xo;
                if (form.getResources() != null) countImagesInResources(form.getResources(), count);
            }
        }
    }
}
