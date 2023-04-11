plugins {
    id("io.quarkus")
}

description = "Daemon HTTP server for LightSwallow"
version = "1.0.0-SNAPSHOT"

val quarkusPlatformVersion: String by project

dependencies {
    implementation(project(mapOf("path" to ":lightswallow-core")))

    // Base
    implementation(enforcedPlatform("io.quarkus.platform:quarkus-bom:$quarkusPlatformVersion"))
    implementation("io.quarkus:quarkus-kotlin")
    implementation("io.quarkus:quarkus-arc")

    // Resteasy
    implementation("io.quarkus:quarkus-resteasy-reactive-jackson")
    implementation("io.quarkus:quarkus-resteasy-reactive-kotlin")

    // RabbitMQ Message Queue
    implementation("io.quarkus:quarkus-smallrye-reactive-messaging-rabbitmq")

    // Config
    implementation("io.quarkus:quarkus-config-yaml")

    // Test
    testImplementation("io.quarkus:quarkus-junit5")
    testImplementation("io.rest-assured:rest-assured")
}

tasks.withType<Test> {
    systemProperty("java.util.logging.manager", "org.jboss.logmanager.LogManager")
}

allOpen {
    annotation("javax.ws.rs.Path")
    annotation("javax.enterprise.context.ApplicationScoped")
    annotation("io.quarkus.test.junit.QuarkusTest")
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions.jvmTarget = JavaVersion.VERSION_17.toString()
    kotlinOptions.javaParameters = true
}
