# KurbyPdf

> PDF 문서에 **안전한 워터마크 삽입 & 검증**을 지원하는 Java 라이브러리

---

## ✨ 주요 기능
- **문서 레벨 워터마크 삽입**  
  - PDF 내부에 눈에 보이지 않는 안전한 워터마크를 숨김
  - AES-GCM 암호화 + HMAC-SHA256 서명 적용
- **PDF 암호 보호**  
  - 사용자(User) 암호와 소유자(Owner) 암호 지원
  - Owner 암호는 자동 랜덤 생성 가능
- **검증 기능 제공**  
  - 삽입된 워터마크의 존재 여부, 무결성, 위변조 여부 확인 가능
- **유연한 클레임(Claims)**  
  - `uid`, `fileId`, `timestamp` 등 원하는 Key-Value 데이터 저장 가능

---

## 📦 설치

### Gradle

```gradle
dependencies {
    implementation files("libs/kurbypdf.jar")
}
```

### Maven
```Maven
<dependency>
    <groupId>io.github.juwonlee</groupId>
    <artifactId>kurbypdf</artifactId>
    <version>1.0.0</version>
    <scope>system</scope>
    <systemPath>${project.basedir}/libs/kurbypdf.jar</systemPath>
</dependency>
```

## 🚀 사용법
### 1) 워터마크 삽입
```
import io.github.juwonlee.kurbypdf.*;

Map<String,String> claims = new HashMap<>();
claims.put("uid", "user-123");
claims.put("fileId", UUID.randomUUID().toString());

WatermarkRequest req = new WatermarkRequest(
    Base64.getEncoder().encodeToString(hmacKey),
    Base64.getEncoder().encodeToString(aesKey),
    claims,
    "userPassword", // PDF 열람용 암호
    null            // Owner 암호 (null → 자동 생성)
);

WatermarkResult res = KurbyPdf.watermark(pdfBytes, req);
byte[] processed = res.getPdfBytes();
String ownerPwd = res.getOwnerPassword();
```

### 2) 워터마크 검증
```단순 검증
boolean isValid = PdfVerification.verify(processed, "userPassword", hmacKey, aesKey);
```
```검증 상세
PdfVerification.Result result = PdfVerification.verifyDetailed(processed, "userPassword", hmacKey, aesKey);

System.out.println("valid=" + result.isValid());
System.out.println("claims=" + result.getClaims());
System.out.println("timestamp=" + result.getTimestamp());
System.out.println("nonce=" + result.getNonce());
```

```
출력 예시:
valid=true
claims={uid=user-123, fileId=7e4e-...}
timestamp=1758531055870
nonce=13935049459731363327
```

## 📂 프로젝트 구조
```
src/main/java/io/github/juwonlee/kurbypdf/
 ├── KurbyPdf.java              # 라이브러리 진입점
 ├── PdfProtector.java          # PDF 암호화 유틸
 ├── PdfForensicEmbedder.java   # 워터마크 삽입기
 ├── PdfWatermarkInspector.java # 워터마크 추출기
 ├── PdfVerification.java       # 검증 유틸
 ├── WatermarkPayload.java      # 워터마크 데이터 구조
 ├── WatermarkRequest.java      # 삽입 요청
 ├── WatermarkResult.java       # 삽입 결과
 └── KeyUtil.java               # 랜덤 키/패스워드 유틸
```

## ✅ 장점
- 보안성: AES-GCM + HMAC-SHA256 적용, 위변조 방지
- 유연성: 클레임 데이터 자유 삽입 가능
- 호환성: JDK 1.8 이상, Fat JAR 제공 가능
- 확장성: 다른 DRM 또는 배포 추적 시스템과 연동 가능

## 📖 사용 사례(예시)
- 전자책 PDF 배포 시 불법 유출 추적
- 기업 내부 보고서/자료 무단 배포 방지
- PDF 기반 시험 문제지/교재 보호

## ⚠️ 주의사항
이 라이브러리는 워터마크 기반 추적 및 위변조 검출 목적에 적합합니다.
강력한 DRM 대체 수단은 아니므로, 필요시 DRM 솔루션과 병행 사용을 권장합니다.

## 📜 라이선스
MIT License
