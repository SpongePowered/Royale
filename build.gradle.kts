import org.spongepowered.gradle.plugin.config.PluginLoaders
import org.spongepowered.plugin.metadata.PluginDependency

plugins {
    `java-library`
    id("org.spongepowered.gradle.plugin") version "1.0.2"
    id("org.cadixdev.licenser") version "0.5.0"
    id("com.github.johnrengelman.shadow") version "6.1.0"

}

group = project.group
version = "${project.properties["minecraftVersion"]}-r${project.properties["apiVersion"].toString().split("-")[0]}"

repositories {
    mavenLocal()
    maven("https://repo.spongepowered.org/repository/maven-public/") {
        name = "sponge"
    }
}

dependencies {
    implementation("org.spongepowered:spongeapi:8.+")
    implementation("net.kyori:adventure-text-minimessage:4.1.0-SNAPSHOT") {
        exclude(group = "net.kyori", module = "adventure-api")
    }
}

sponge {
    apiVersion("${project.properties["apiVersion"]}")
    plugin("royale") {
        loader(PluginLoaders.JAVA_PLAIN)
        displayName("${project.properties["name"]}")
        mainClass("org.spongepowered.royale.Royale")
        description("Battle Royale, now on Sponge!")
        links {
            homepage("https://spongepowered.org")
            source("https://github.com/SpongePowered/Royale")
            issues("https://github.com/SpongePowered/Royale/issues")
        }
        contributor("Spongie") {
            description("Lead Developer")
        }
        dependency("spongeapi") {
            loadOrder(PluginDependency.LoadOrder.AFTER)
            optional(false)
        }
    }
}

val javaTarget = 8 // Sponge targets a minimum of Java 8
java {
    sourceCompatibility = JavaVersion.toVersion(javaTarget)
    targetCompatibility = JavaVersion.toVersion(javaTarget)
}

tasks.withType(JavaCompile::class).configureEach {
    options.apply {
        encoding = "utf-8" // Consistent source file encoding
        if (JavaVersion.current().isJava10Compatible) {
            release.set(javaTarget)
        }
    }
}

tasks {
    jar {
        manifest {
            attributes(mapOf(
                    "Implementation-Title" to project.name,
                    "Implementation-Version" to project.version,
                    "Implementation-Vendor" to project.properties["organization"]
            )
            )
        }
    }
    shadowJar {
        archiveClassifier.set("plugin")
        dependencies {
            include(dependency("net.kyori:adventure-text-minimessage"))
        }
    }
    build {
        dependsOn.add(shadowJar)
    }
}

license {
    (this as ExtensionAware).extra.apply {
        this["name"] = project.properties["name"]
        this["organization"] = project.properties["organization"]
        this["url"] = project.properties["url"]
    }
    header = project.file("HEADER.txt")

    include("**/*.java")
    newLine = false
}


// Make sure all tasks which produce archives (jar, sources jar, javadoc jar, etc) produce more consistent output
tasks.withType(AbstractArchiveTask::class).configureEach {
    isReproducibleFileOrder = true
    isPreserveFileTimestamps = false
}