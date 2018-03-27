import groovy.util.Node
import org.gradle.api.tasks.testing.logging.TestExceptionFormat

plugins {
    `java-library`
    id("net.minecrell.licenser") version "0.3"
    `maven-publish`
}

group = "eu.mikroskeem"
version = "0.0.4-SNAPSHOT"

val gradleWrapperVersion = "4.6"

val jbAnnotationsVersion = "15.0"
val junitVersion = "5.1.0"

repositories {
    mavenLocal()
    mavenCentral()

    maven("https://repo.wut.ee/repository/mikroskeem-repo/")
}

dependencies {
    implementation("org.jetbrains:annotations:$jbAnnotationsVersion")

    testImplementation("org.junit.jupiter:junit-jupiter-api:$junitVersion")
    testRuntime("org.junit.jupiter:junit-jupiter-engine:$junitVersion")
}

license {
    header = rootProject.file("etc/HEADER")
    filter.include("**/*.java")
}

val sourcesJar by tasks.creating(Jar::class) {
    classifier = "sources"
    from(java.sourceSets["main"].allJava)
}

val javadoc by tasks.getting(Javadoc::class)

val javadocJar by tasks.creating(Jar::class) {
    dependsOn(javadoc)
    classifier = "javadoc"
    from(javadoc.destinationDir)
}

val wrapper by tasks.getting(Wrapper::class) {
    gradleVersion = gradleWrapperVersion
    distributionUrl = "https://services.gradle.org/distributions/gradle-$gradleVersion-all.zip"
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
    (publications) {
        "maven"(MavenPublication::class) {
            artifactId = "picomaven"

            from(components["java"])
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
        if(rootProject.hasProperty("wutRepoUsername") && rootProject.hasProperty("wutRepoPassword")) {
            maven("https://repo.wut.ee/repository/mikroskeem-repo").credentials {
                username = rootProject.properties["wutRepoUsername"]!! as String
                password = rootProject.properties["wutRepoPassword"]!! as String
            }
        }
    }
}

tasks["build"].dependsOn("licenseFormat")

fun XmlProvider.builder(builder: GroovyBuilderScope.() -> Unit) {
    (asNode().children().last() as Node).plus(delegateClosureOf<Any> {
        withGroovyBuilder(builder)
    })
}
