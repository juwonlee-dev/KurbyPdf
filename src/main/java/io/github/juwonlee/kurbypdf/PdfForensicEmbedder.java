package io.github.juwonlee.kurbypdf;

import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.cos.COSStream;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.form.PDFormXObject;
import org.apache.pdfbox.util.Matrix;

import java.io.OutputStream;
import java.util.Random;

public class PdfForensicEmbedder {
    private static final COSName WM_KEY = COSName.getPDFName("_k1");
    private static final COSName WM_VER = COSName.getPDFName("_k1v");

    /**
     * 기본 embed: 페이지당 3개 삽입, 랜덤 위치
     */
    public static void embed(PDDocument doc, byte[] encryptedPayload) throws Exception {
        embed(doc, encryptedPayload, 3, true, System.currentTimeMillis());
    }

    /**
     * 커스터마이즈 가능한 embed
     *
     * @param doc                PDDocument (열려있는 상태)
     * @param encryptedPayload   암호화된 페이로드 바이트 (워터마크)
     * @param copiesPerPage      각 페이지당 삽입할 복제본 수 (>=1)
     * @param randomizePositions true이면 페이지 내에서 무작위 위치로 삽입(보통 더 안전)
     * @param seed               랜덤 위치 시드 (재현성 필요하면 고정 시드 사용)
     */
    public static void embed(PDDocument doc, byte[] encryptedPayload, int copiesPerPage, boolean randomizePositions, long seed) throws Exception {
        if (doc == null) throw new IllegalArgumentException("doc == null");
        if (encryptedPayload == null || encryptedPayload.length == 0) throw new IllegalArgumentException("payload empty");
        if (copiesPerPage < 1) copiesPerPage = 1;

        Random rnd = new Random(seed);

        // 1) payload를 담은 COSStream을 한 번만 생성
        COSStream payloadStream = doc.getDocument().createCOSStream();
        payloadStream.setItem(WM_VER, COSName.getPDFName("1"));
        OutputStream out = null;
        try {
            out = payloadStream.createOutputStream();
            out.write(encryptedPayload);
        } finally {
            if (out != null) try { out.close(); } catch (Exception ignore) {}
        }

        // 2) 각 페이지마다 FormXObject 생성 + payloadStream을 딕셔너리에 붙임
        for (PDPage page : doc.getPages()) {
            PDRectangle media = page.getMediaBox();
            float pageWidth = media.getWidth();
            float pageHeight = media.getHeight();

            try (PDPageContentStream cs = new PDPageContentStream(doc, page, PDPageContentStream.AppendMode.APPEND, true, true)) {

                for (int i = 0; i < copiesPerPage; i++) {
                    PDFormXObject form = new PDFormXObject(doc);
                    form.setBBox(new PDRectangle(1, 1));
                    form.getCOSObject().setItem(WM_KEY, payloadStream);

                    // 랜덤 위치 or 고정 위치
                    float tx, ty;
                    if (randomizePositions) {
                        float marginX = Math.max(1f, pageWidth * 0.05f);
                        float marginY = Math.max(1f, pageHeight * 0.05f);
                        tx = marginX + rnd.nextFloat() * (pageWidth - 2 * marginX);
                        ty = marginY + rnd.nextFloat() * (pageHeight - 2 * marginY);
                    } else {
                        float gap = Math.max(1f, pageHeight / (copiesPerPage + 1));
                        tx = 1f;
                        ty = gap * (i + 1);
                    }

                    // 참조 삽입 (클리핑으로 보이지 않게)
                    cs.saveGraphicsState();
                    cs.transform(Matrix.getTranslateInstance(tx, ty));
                    cs.addRect(0, 0, 0, 0);
                    cs.clip();
                    cs.closePath();
                    cs.drawForm(form);
                    cs.restoreGraphicsState();
                }
            }
        }
    }
}
