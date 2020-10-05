plugins {
    java
    id("net.minecrell.licenser") version "0.4.1"
    id("com.github.johnrengelman.shadow") version "5.2.0"
}

group = project.group
version = "${project.properties["minecraftVersion"]}-r${project.properties["apiVersion"].toString().split("-")[0]}"

repositories {
    maven {
        name = "sponge v2"
        setUrl("https://repo-new.spongepowered.org/repository/maven-public/")
    }
}

dependencies {
    implementation("org.spongepowered:spongeapi:8.+")
    implementation("net.kyori:adventure-text-minimessage:4.0.0-SNAPSHOT")
    implementation("commons-io:commons-io:2.5")
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

tasks {
    compileJava {
        options.compilerArgs.addAll(listOf("-Xmaxerrs", "1000"))
        options.encoding = "UTF-8"
    }
    jar {
        manifest {
            attributes(mapOf(
                    "Implementation-Title" to project.name,
                    "Implementation-Version" to project.version,
                    "Implementation-Vendor" to "SpongePowered"
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
