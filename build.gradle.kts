import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import groovy.util.Node
import org.gradle.api.tasks.testing.logging.TestExceptionFormat

plugins {
    `java-library`
    id("net.minecrell.licenser") version "0.4.1"
    id("com.github.johnrengelman.shadow") version "5.0.0"
    `maven-publish`
}

group = "eu.mikroskeem"
version = "0.0.4-SNAPSHOT"

val okHttpVersion = "4.0.1"
val checkerQualVersion = "2.9.0"
val mavenMetaVersion = "3.6.1"

val junitVersion = "5.5.1"

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

repositories {
    mavenLocal()
    mavenCentral()

    maven("https://repo.wut.ee/repository/mikroskeem-repo/")
}

dependencies {
    api("com.squareup.okhttp3:okhttp:$okHttpVersion")
    implementation("org.apache.maven:maven-repository-metadata:$mavenMetaVersion")
    implementation("org.checkerframework:checker-qual:$checkerQualVersion")

    testImplementation("org.junit.jupiter:junit-jupiter-api:$junitVersion")
    testRuntime("org.junit.jupiter:junit-jupiter-engine:$junitVersion")
}

license {
    header = rootProject.file("etc/HEADER")
    filter.include("**/*.java")
}

val sourcesJar by tasks.creating(Jar::class) {
    classifier = "sources"
    from(sourceSets["main"].allJava)
}

val javadoc by tasks.getting(Javadoc::class)

val javadocJar by tasks.creating(Jar::class) {
    dependsOn(javadoc)
    classifier = "javadoc"
    from(javadoc.destinationDir)
}


val shadowJar by tasks.getting(ShadowJar::class) {
    classifier = "shaded"

    val targetPackage = "eu.mikroskeem.picomaven.shaded"
    val relocations = listOf(
            "org.apache.maven",
            "org.codehaus.plexus",
            "okhttp3",
            "okio"
    )

    relocations.forEach {
        relocate(it, "$targetPackage.$it")
    }
}

val test by tasks.getting(Test::class) {
    useJUnitPlatform()

    // Show output
    testLogging {
        showStandardStreams = true
        exceptionFormat = TestExceptionFormat.FULL
    }

    // Verbose
    beforeTest(closureOf<Any> { logger.lifecycle("Running test: $this") })

}

publishing {
    publications {
        create<MavenPublication>("maven") {
            artifactId = "picomaven"

            from(components["java"])
            artifact(shadowJar)
            artifact(sourcesJar)
            artifact(javadocJar)

            pom.withXml {
                builder {
                    "name"("PicoMaven")
                    "description"("Library to download libraries from Maven repository on app startup")

                    "repositories" {
                        "repository" {
                            "id"("mikroskeem-repo")
                            "url"("https://repo.wut.ee/repository/mikroskeem-repo")
                        }
                    }

                    "issueManagement" {
                        "system"("GitHub Issues")
                        "url"("https://github.com/mikroskeem/PicoMaven/issues")
                    }

                    "licenses" {
                        "license" {
                            "name"("MIT License")
                            "url"("https://opensource.org/licenses/MIT")
                        }
                    }

                    "developers" {
                        "developer" {
                            "id"("mikroskeem")
                            "name"("Mark Vainomaa")
                            "email"("mikroskeem@mikroskeem.eu")
                        }
                    }

                    "scm" {
                        "connection"("scm:git@github.com:mikroskeem/PicoMaven.git")
                        "developerConnection"("scm:git@github.com:mikroskeem/PicoMaven.git")
                        "url"("https://github.com/mikroskeem/PicoMaven")
                    }
                }
            }
        }
    }

    repositories {
        mavenLocal()
        if (rootProject.hasProperty("wutee.repository.deploy.username") && rootProject.hasProperty("wutee.repository.deploy.password")) {
            maven("https://repo.wut.ee/repository/mikroskeem-repo").credentials {
                username = rootProject.properties["wutee.repository.deploy.username"]!! as String
                password = rootProject.properties["wutee.repository.deploy.password"]!! as String
            }
        }
    }
}

tasks["build"].dependsOn("licenseFormat", shadowJar)

fun XmlProvider.builder(builder: GroovyBuilderScope.() -> Unit) {
    (asNode().children().last() as Node).plus(delegateClosureOf<Any> {
        withGroovyBuilder(builder)
    })
}
