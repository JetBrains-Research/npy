
import com.jfrog.bintray.gradle.BintrayExtension
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.tasks.bundling.Jar
import org.gradle.api.tasks.wrapper.Wrapper
import org.gradle.script.lang.kotlin.*
import org.jetbrains.dokka.gradle.DokkaTask
import org.jetbrains.dokka.gradle.LinkMapping

val kotlinVersion = "1.1.2-2"

plugins {
    id("maven-publish")
    id("com.jfrog.bintray") version "1.6"
    id("org.jetbrains.kotlin.jvm") version "1.1.2-2"
    id("org.jetbrains.dokka") version "0.9.14-eap-2"
}

repositories {
    jcenter()
    mavenCentral()
}

dependencies {
    compile(kotlinModule("stdlib-jre8", kotlinVersion))

    testCompile(kotlinModule("test-junit", kotlinVersion))
}

configure<PublishingExtension> {
    group = "org.jetbrains.bio"
    version = project.version

    publications {
        create("mavenJava", MavenPublication::class.java) {
            from(components["java"])
            artifact("sourcesJar")
        }
    }
}

configure<BintrayExtension> {
    user = (properties["bintrayUser"] as String?) ?: System.getenv("BINTRAY_USER")
    key = (properties["bintrayKey"] as String?) ?: System.getenv("BINTRAY_KEY")
    setPublications("mavenJava")  // See KT-17882

    pkg(delegateClosureOf<BintrayExtension.PackageConfig> {
        repo = "maven"
        name = "npy"
        userOrg = "jetbrains-research"
        setLicenses("MIT")

        version(closureOf<BintrayExtension.VersionConfig> {
            name = project.version.toString()
        })
    })
}

tasks {
    "sourcesJar"(Jar::class) {
        classifier = "sources"

        // TODO: add from(sourceSets.main.allSource)
    }

    "dokka"(DokkaTask::class) {
        moduleName = "$version"
        outputFormat = "gfm"
        outputDirectory = "$rootDir/docs"

        linkMapping(closureOf<LinkMapping> {
            dir = "src/main/kotlin"
            url = "https://github.com/JetBrains-Research/npy/blob/$version/src/main/kotlin"
            suffix = "#L"
        })
    }

    "wrapper"(Wrapper::class) {
        gradleVersion = "3.5"
    }
}