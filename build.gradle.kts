import org.jasypt.encryption.pbe.PooledPBEStringEncryptor
import org.jasypt.encryption.pbe.config.SimpleStringPBEConfig

plugins {
    kotlin("jvm") version "2.2.21"
    id("org.springframework.boot") version "3.5.7"
    id("io.spring.dependency-management") version "1.1.6"
    kotlin("plugin.spring") version "2.2.21"
    kotlin("plugin.jpa") version "2.2.21"
    id("org.cthing.jasypt") version "1.0.0"
}

group = "GameMate"
version = "1.0"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

repositories {
    mavenCentral()
    maven { url = uri("https://jitpack.io") }
}

dependencies {
    // Spring Boot
    implementation("org.springframework.boot:spring-boot-starter")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.postgresql:postgresql:42.7.4")

    // Telegram Bots
    implementation("io.github.kotlin-telegram-bot.kotlin-telegram-bot:telegram:6.2.0")

    implementation("org.jsoup:jsoup:1.18.1")
    implementation("org.springframework.boot:spring-boot-starter-web")

    // API
    implementation("com.squareup.okhttp3:okhttp:5.1.0")
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")

    //crypt
    implementation("com.github.ulisesbocchio:jasypt-spring-boot-starter:3.0.5")
}

tasks.test {
    useJUnitPlatform()
}

tasks.register("encrypt") {
    doLast {
        val value = project.findProperty("value")?.toString()
            ?: error("Укажи значение для шифрования через -Pvalue=\"токен\"")
        val password = project.findProperty("password")?.toString()
            ?: error("Укажи пароль для шифрования через -Ppassword=\"ключ\"")

        val config = SimpleStringPBEConfig().apply {
            setPassword(password)
            algorithm = "PBEWITHHMACSHA512ANDAES_256"
            setKeyObtentionIterations("1000")
            setPoolSize("1")
            providerName = "SunJCE"
            setSaltGeneratorClassName("org.jasypt.salt.RandomSaltGenerator")
            setIvGeneratorClassName("org.jasypt.iv.RandomIvGenerator")
            stringOutputType = "base64"
        }

        val encryptor = PooledPBEStringEncryptor().apply {
            setConfig(config)
        }

        val encrypted = encryptor.encrypt(value)

        println("✅ Encrypted value:")
        println("ENC($encrypted)")
    }
}

kotlin {
    jvmToolchain(21)
}