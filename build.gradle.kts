plugins {
    id("java")
    id("io.spring.dependency-management") version "1.1.6"
    id("org.springframework.boot") version "3.3.5"
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
}