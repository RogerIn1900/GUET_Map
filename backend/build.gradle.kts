import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("org.jetbrains.kotlin.jvm") version "1.9.22"
    id("application")
}

group = "com.example.guet_map"
version = "1.0.0"

repositories {
    mavenCentral()
}

val ktorVersion = "2.3.12"
val exposedVersion = "0.49.0"
val mailVersion = "1.6.2"

dependencies {
    // Ktor
    implementation("io.ktor:ktor-server-core:$ktorVersion")
    implementation("io.ktor:ktor-server-netty:$ktorVersion")
    implementation("io.ktor:ktor-server-cors:$ktorVersion")
    implementation("io.ktor:ktor-server-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-serialization-gson:$ktorVersion")
    implementation("io.ktor:ktor-server-call-logging:$ktorVersion")
    implementation("io.ktor:ktor-server-status-pages:$ktorVersion")
    implementation("io.ktor:ktor-server-auto-head-response:$ktorVersion")
    implementation("io.ktor:ktor-server-default-headers:$ktorVersion")
    
    // Exposed (SQL)
    implementation("org.jetbrains.exposed:exposed-core:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-jdbc:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-java-time:$exposedVersion")
    
    // SQLite
    implementation("org.xerial:sqlite-jdbc:3.45.1.0")
    
    // MySQL
    implementation("com.mysql:mysql-connector-j:8.3.0")
    
    // Redis
    implementation("redis.clients:jedis:5.1.0")
    
    // Email
    implementation("com.sun.mail:javax.mail:$mailVersion")
    
    // JWT
    implementation("io.ktor:ktor-server-auth-jwt:$ktorVersion")
    implementation("com.auth0:java-jwt:4.4.0")
    
    // YAML config
    implementation("org.yaml:snakeyaml:2.2")
    
    // Logging
    implementation("ch.qos.logback:logback-classic:1.4.14")
    
    // BCrypt for password hashing
    implementation("org.mindrot:jbcrypt:0.4")
}

kotlin {
    jvmToolchain(21)
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "21"
}

application {
    mainClass.set("com.example.guet_map.backend.ServerKt")
}
