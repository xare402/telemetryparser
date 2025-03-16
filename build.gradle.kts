import org.gradle.api.tasks.bundling.Zip
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

plugins {
    id("java")
    id("application")
    id("edu.sc.seis.launch4j") version "3.0.6"
    id("com.github.johnrengelman.shadow") version "8.1.1"
    application
    id("org.beryx.jlink") version "2.26.0"
}

group = "com.telemetryparser"
version = "1.0"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.9.1"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    implementation("uk.co.caprica:vlcj:4.10.1")
    implementation("uk.co.caprica:vlcj-natives:4.8.3")
    implementation("com.formdev:flatlaf:3.5.4")
    implementation("com.formdev:flatlaf-intellij-themes:3.5.4")
    implementation("com.formdev:flatlaf-extras:3.5.4")
    implementation("net.sourceforge.tess4j:tess4j:5.13.0")
    implementation("org.slf4j:slf4j-simple:2.0.7")
}

application {
    mainClass.set("com.telemetryparser.Main")
}

launch4j {
    mainClassName = "com.telemetryparser.Main"
    icon = "${projectDir}/icons/TelemetryParser.ico"
    outfile = "TelemetryParser.exe"
    requires64Bit = true
    stayAlive = false
    headerType = "console"
    jvmOptions = listOf("--enable-preview")
    bundledJrePath = file("$projectDir/build/jre").absolutePath
}


val createFastStart by tasks.registering(edu.sc.seis.launch4j.tasks.Launch4jLibraryTask::class) {
    dependsOn(createJre)

    outfile.set("TelemetryParser.exe")
    mainClassName.set("com.telemetryparser.Main")
    icon.set("$projectDir/icons/TelemetryParser.ico")
    fileDescription.set("The lightning fast implementation")

    bundledJrePath = file("$projectDir/build/jre").absolutePath

    doLast {
        println("The EXE is located at: ${outfile.get()}")
        println("Its parent directory is: ${file(outfile.get()).parentFile}")
        copy {
            from("$projectDir/yt-dlp.exe")
            into("$projectDir/build/launch4j")
        }
    }
}


val createJre by tasks.registering(Exec::class) {
    group = "build"
    description = "Generates a custom Java runtime using jlink"

    val javaHome = System.getenv("JAVA_HOME") ?: error("JAVA_HOME is not set")

    doFirst {
        val jreDir = file("build/jre")
        if (jreDir.exists()) {
            println("Deleting existing JRE directory at: ${jreDir.absolutePath}")
            jreDir.deleteRecursively()
        }
    }

    commandLine(
            "$javaHome/bin/jlink",
            "--module-path", "$javaHome/jmods",
            "--add-modules", "java.base,java.desktop,java.logging,jdk.unsupported",
            "--output", "build/jre",
            "--no-header-files",
            "--no-man-pages",
            "--strip-debug",
            "--compress=2"
    )


    doLast {
        println("Custom JRE created at: build/jre")
    }
}

val buildDate: String = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"))

tasks.register<Zip>("buildExe") {
    dependsOn(createFastStart)

    from("$projectDir/build/launch4j")

    from("$projectDir/build/jre") {
        into("jre")
    }

    from("$projectDir/rois.properties") {
        into("")
    }

    from("$projectDir/tessdata") {
        into("tessdata")
    }

    from("$projectDir/fonts") {
        into("fonts")
    }

    destinationDirectory.set(file("$projectDir/releases"))
    archiveFileName.set("Win64-TelemetryParser-${version}-$buildDate.zip")}




tasks.withType<JavaCompile> {
    options.compilerArgs.add("--enable-preview")
}

tasks.withType<Test> {
    jvmArgs("--enable-preview")
}

tasks.withType<JavaExec> {
    jvmArgs("--enable-preview")
}

tasks.test {
    useJUnitPlatform()
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}
