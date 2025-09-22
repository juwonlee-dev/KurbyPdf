plugins {
    `java-library`
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

group = "io.github.juwonlee"
version = "1.0.0"

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
    withSourcesJar()
    withJavadocJar()
}

tasks.withType<JavaCompile>().configureEach {
    // JDK 11/17/21 같은 고버전 JDK로 빌드하더라도
    // 표준 라이브러리/바이트코드를 '정확히' Java 8에 맞춰줌
    options.release.set(8)
}

repositories {
    mavenCentral()
}

dependencies {
    api("org.apache.pdfbox:pdfbox:2.0.30")
    api("com.fasterxml.jackson.core:jackson-databind:2.17.1")

    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}

tasks.test {
    useJUnitPlatform()
}

tasks.jar {
    archiveBaseName.set("KurbyPdf")
}

tasks.shadowJar {
    // 결과 파일명: build/libs/KurbyPdf-1.0.0-all.jar
    archiveBaseName.set("KurbyPdf")
    archiveClassifier.set("all")

    // ★ Shadow가 처리할 설정을 런타임 클래스패스만으로 제한
    configurations = listOf(project.configurations.runtimeClasspath.get())

    // ★ Multi-Release JAR 내부의 고버전 클래스/모듈 메타데이터는 제외
    exclude("META-INF/versions/**")   // 자바 9+용 대체 클래스들(11/17/21 등)
    exclude("module-info.class")      // JPMS 메타데이터
    exclude("**/*.kotlin_metadata")   // (있다면) 코틀린 메타데이터
    // (테스트 의존성이 섞이는 걸 방지) 보수적으로 테스트 관련 패키지도 제외
    exclude("org/junit/**", "org/opentest4j/**", "org/apiguardian/**")

    // ====== Shading(충돌 방지용 relocate) 설정 ======
    // PDFBox(및 하위 모듈 FontBox, XmpBox) 패키지 충돌 방지
    relocate("org.apache.pdfbox", "io.github.juwonlee.shadow.pdfbox")
    relocate("org.apache.fontbox", "io.github.juwonlee.shadow.fontbox")
    relocate("org.apache.xmpbox",  "io.github.juwonlee.shadow.xmpbox")

    // Jackson 전체 충돌 방지 (core/annotations/databind 등)
    relocate("com.fasterxml.jackson", "io.github.juwonlee.shadow.jackson")

    // (선택) commons-logging 충돌도 방지하고 싶다면 주석 해제
    // relocate("org.apache.commons.logging", "io.github.juwonlee.shadow.commons.logging")

    // (선택) 서비스 파일 병합이 필요한 경우 사용
    // mergeServiceFiles()
    // minimize() // 의존성 축소(주의: 과도 축소 시 NoClassDefFoundError 위험)
}

tasks.register<Copy>("dist") {
    dependsOn("build", "shadowJar")
    from(layout.buildDirectory.file("libs/KurbyPdf-${project.version}.jar"))
    from(layout.buildDirectory.file("libs/KurbyPdf-${project.version}-sources.jar"))
    from(layout.buildDirectory.file("libs/KurbyPdf-${project.version}-javadoc.jar"))
    from(layout.buildDirectory.file("libs/KurbyPdf-${project.version}-all.jar"))
    into(layout.projectDirectory.dir("dist"))
}