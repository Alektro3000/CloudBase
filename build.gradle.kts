plugins {
    java
    id("io.spring.dependency-management") version "1.1.6"
    id("org.springframework.boot") version "3.3.5"
    id("info.solidsoft.pitest") version "1.19.0-rc.3"
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

    pitest("org.pitest:pitest-junit5-plugin:1.2.3")

    //TestLauncher
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
    maxParallelForks = 1
    forkEvery = 1
    jvmArgs("-XX:+EnableDynamicAgentLoading")
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
        "com.al3000.cloudbase.*Application*",
        "com.al3000.cloudbase.controller.*"
    ))
}