import org.gradle.jvm.tasks.Jar
import java.io.ByteArrayOutputStream

val semantic_version: String by project

val jackson_version: String by project
val jsonpath_version: String by project
val logback_version: String by project
val apache_commons_version: String by project
val kafka_version: String by project
val slf4j_version: String by project
val confluent_version: String by project
val avro_version: String by project

val picocli_version: String by project

val junit_pioneer_version: String by project
val junit_version: String by project
val testcontainer_kafka_version: String by project


plugins {
    id("java")
}

// this allows for subprojects to use java plugin constructs
// without then also causing the parent to have an empty jar file
// generated.
tasks.withType<Jar> {
    onlyIf { !sourceSets["main"].allSource.isEmpty }
}

allprojects {
    repositories {
        mavenLocal()
        mavenCentral()
        maven(url = "https://packages.confluent.io/maven/")
    }

}

subprojects {
    version = "${semantic_version}"

    plugins.apply("java")
    plugins.apply("application")

    java {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    dependencies {
        implementation("info.picocli:picocli:${picocli_version}")
        implementation("org.apache.commons:commons-lang3:$apache_commons_version")
        implementation("org.apache.kafka:kafka-clients:$kafka_version") {
            version {
                strictly(kafka_version)
            }
        }
        implementation("org.slf4j:slf4j-api:$slf4j_version")

        implementation("io.confluent:kafka-avro-serializer:$confluent_version")
        implementation("io.confluent:kafka-json-schema-serializer:$confluent_version")
        implementation("org.apache.avro:avro:$avro_version")

        //
        implementation("com.fasterxml.jackson.core:jackson-core:$jackson_version")
        implementation("com.fasterxml.jackson.core:jackson-databind:$jackson_version")
        implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:$jackson_version")
        implementation("com.fasterxml.jackson.datatype:jackson-datatype-jdk8:$jackson_version")

        implementation("com.jayway.jsonpath:json-path:$jsonpath_version")

        runtimeOnly("ch.qos.logback:logback-classic:$logback_version")

        //
        testImplementation("org.junit-pioneer:junit-pioneer:$junit_pioneer_version")
        testImplementation("org.junit.jupiter:junit-jupiter-api:$junit_version")
        testImplementation("org.junit.jupiter:junit-jupiter-params:$junit_version")
        testImplementation("org.testcontainers:kafka:$testcontainer_kafka_version")
        testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:$junit_version")
    }

    tasks.test {
        useJUnitPlatform()
    }

    // no reason to build both .tar and .zip application distributions, disable zip
    tasks.getByName<Zip>("distZip").isEnabled = false

    tasks.register("getTagVersion") {
        group = "Versioning"
        description = "get the branch name or tag name"
        doLast {
            val stdout = ByteArrayOutputStream()
            exec {
                commandLine("git", "describe", "--tags", "--always", "--first-parent", "--abbrev=1", "--match=v*", "HEAD")
                standardOutput = stdout
            }
            var branchOrTag = stdout.toString().trim()

            when {
                branchOrTag.matches(Regex("v[0-9]+\\.[0-9]+\\.[0-9]+")) -> {
                    println("tag matches: $branchOrTag")
                }
                else -> {
                    stdout.reset() // clear stdout
                    exec {
                        commandLine("git", "symbolic-ref", "--short", "HEAD")
                        standardOutput = stdout
                    }
                    branchOrTag = stdout.toString().trim()
                }
            }

            project.version = branchOrTag
            println("project version " + project.version)
        }
    }

    tasks.getByName<Tar>("distTar") {
        dependsOn("getTagVersion")
    }
}


subprojects {

    if (file("${project.projectDir}/run.sh").exists()) {

        val createIntegrationClasspath: (String) -> Unit = { scriptName ->
            val cp = extensions.getByName<JavaPluginExtension>("java").sourceSets["main"].runtimeClasspath.files.joinToString("\n") {
                """export CP="${'$'}{CP}:$it""""
            }

            val file = file(scriptName)
            file.writeText("export CP=\"\"\n$cp\n")

            file.setExecutable(true)
        }

        val postBuildScript by tasks.registering {
            doLast {
                createIntegrationClasspath("./.classpath.sh")
            }
        }

        tasks.named("build").configure {
            finalizedBy(postBuildScript)
        }
    }
}
