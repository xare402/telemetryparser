import org.gradle.api.tasks.bundling.Zip

plugins {
    id("java")
    id("application")
    id("edu.sc.seis.launch4j") version "3.0.6"
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

group = "com.telemetryparser"
version = "1.0-SNAPSHOT"

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
}

application {
    mainClass.set("com.telemetryparser.StreamParser")
}

launch4j {
    mainClassName = "com.telemetryparser.StreamParser"
    icon = "${projectDir}/icons/TelemetryParser.ico"
}

tasks.register<edu.sc.seis.launch4j.tasks.Launch4jLibraryTask>("createFastStart") {
    dependsOn("shadowJar")
    outfile.set("TelemetryParser.exe")
    mainClassName.set("com.telemetryparser.StreamParser")
    icon.set("$projectDir/icons/TelemetryParser.ico")
    fileDescription.set("The lightning fast implementation")

    doLast {
        println("The EXE is located at: ${outfile.get()}")
        println("Its parent directory is: ${file(outfile.get()).parentFile}")
        copy {
            from("$projectDir/yt-dlp.exe")
            into("$projectDir/build/launch4j")
        }
    }
}

tasks.register<Zip>("buildExe") {
    dependsOn("createFastStart")
    from("$projectDir/build/launch4j")

    destinationDirectory.set(file("$projectDir/distributions"))

    archiveFileName.set("TelemetryParser-$version.zip")
}

tasks.test {
    useJUnitPlatform()
}