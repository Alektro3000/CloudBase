plugins {
    java
    id("io.spring.dependency-management") version "1.1.6"
    id("org.springframework.boot") version "3.3.5"
    id("info.solidsoft.pitest") version "1.19.0-rc.3"
    pmd
    jacoco
}

group = "com.al3000"
version = "1.0-SNAPSHOT"
java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21)) // Specify your desired Java version
    }
}
repositories {
    mavenCentral()
}

dependencies {

    // --- Spring Web (REST, MVC) ---
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")

    // --- Spring Security ---
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-validation")

    // --- Spring Session (with Redis) ---
    implementation("org.springframework.session:spring-session-data-redis")
    implementation("org.springframework.boot:spring-boot-starter-data-redis")
    implementation("org.springframework.boot:spring-boot-starter-actuator")

    implementation("org.springframework.data:spring-data-commons")

    compileOnly("org.projectlombok:lombok:1.18.42")
    annotationProcessor("org.projectlombok:lombok:1.18.42")

    //Minio
    implementation("io.minio:minio:8.6.0")


    runtimeOnly("org.postgresql:postgresql")

    //Testing
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.security:spring-security-test")
    testImplementation("org.mockito:mockito-core:5.21.0")
    testImplementation("org.junit.platform:junit-platform-suite")
    testImplementation("com.code-intelligence:jazzer-junit:0.29.1")

    //Cucumber
    testImplementation("io.cucumber:cucumber-java:7.20.1")
    testImplementation("io.cucumber:cucumber-spring:7.20.1")
    testRuntimeOnly("io.cucumber:cucumber-junit-platform-engine:7.20.1")

    testRuntimeOnly("com.h2database:h2")

    //Integration testing
    testImplementation(platform("org.testcontainers:testcontainers-bom:1.19.0"))
    testImplementation("org.testcontainers:junit-jupiter")
    testImplementation("org.testcontainers:testcontainers:2.0.2")
    testImplementation("io.minio:minio:8.6.0")

    pitest("org.pitest:pitest-junit5-plugin:1.2.3")

    //TestLauncher
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
    maxParallelForks = 1
    forkEvery = 1
    jvmArgs("-XX:+EnableDynamicAgentLoading")
    testLogging {
        showStandardStreams = true
    }
    finalizedBy(tasks.jacocoTestReport) // generate report after tests
}

jacoco {
    toolVersion = "0.8.12"
}

tasks.jacocoTestReport {
    dependsOn(tasks.test)

    reports {
        html.required.set(true)
        xml.required.set(true)
        csv.required.set(false)
    }
}

pmd {
    toolVersion = "7.18.0"
    isIgnoreFailures = true
    ruleSets = listOf<String>(
        "category/java/errorprone.xml",
        "category/java/bestpractices.xml",
        "category/java/design.xml",
        "category/java/security.xml",
        )
}

tasks.withType<Pmd>().configureEach {
    reports {
        html.required.set(true)
    }
}

pitest {
    junit5PluginVersion.set("1.2.3")

    targetClasses.set(listOf("com.al3000.cloudbase.*"))
    targetTests.set(listOf("com.al3000.cloudbase.*"))

    threads.set(1)
    outputFormats.set(listOf("HTML", "XML"))
    timestampedReports.set(false)
    jvmArgs.set(listOf("-XX:+EnableDynamicAgentLoading", "-Djdk.attach.allowAttachSelf=true"))

    // Helpful while debugging:
    failWhenNoMutations.set(false)
    exportLineCoverage.set(true)
    verbose.set(true)

    excludedClasses.set(listOf(
        "com.al3000.cloudbase.*Config*",
        "com.al3000.cloudbase.*Application*"
    ))
}


tasks.register<JavaExec>("benchmarkSearchCsv") {
    group = "verification"
    description = "Exports string-search benchmark results to CSV files."
    classpath = sourceSets.test.get().runtimeClasspath
    mainClass.set("com.al3000.cloudbase.service.search.StringSearchBenchmarkCsvExporter")
    dependsOn(tasks.testClasses)
}
