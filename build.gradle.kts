import java.net.URI
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.security.MessageDigest
import java.util.zip.ZipFile

plugins {
    java
}

group = "com.adbtool"
version = providers.gradleProperty("version").getOrElse("0.1.0-SNAPSHOT")
require(version.toString().matches(Regex("^[0-9]+\\.[0-9]+\\.[0-9]+([+-][0-9A-Za-z.-]+)?$"))) {
    "version must be semantic, for example 1.2.3 or 1.2.3-SNAPSHOT"
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

val androidPlatformUrl = "https://dl.google.com/android/repository/platform-35_r01.zip"
val androidPlatformSha256 = "ec20a0a65704e1b2554f8e3eebf588ef260569673c848c01aff2e29c28734cf4"
val androidJarSha256 = "4566663c3876e022b4fa4ced8c8697c4ab1688267f090114fd92d027b32e619b"
val r8Url = "https://dl.google.com/dl/android/maven2/com/android/tools/r8/9.1.31/r8-9.1.31.jar"
val r8Sha256 = "3b4de3053885da105e39c15212261d22653d6d1b5eb92323dd04ae913cc8286f"

val toolchainDirectory = providers.provider {
    layout.projectDirectory.dir(".gradle/adbtool/toolchains")
}
val androidPlatformZip = toolchainDirectory.map { it.file("platform-35_r01.zip") }
val androidJar = toolchainDirectory.map { it.file("android-35/android.jar") }
val r8Jar = toolchainDirectory.map { it.file("r8-9.1.31.jar") }

dependencies {
    testCompileOnly(files(androidJar))
    testRuntimeOnly(files(androidJar))
}

fun sha256(file: File): String {
    val digest = MessageDigest.getInstance("SHA-256")
    file.inputStream().buffered().use { input ->
        val buffer = ByteArray(8192)
        while (true) {
            val read = input.read(buffer)
            if (read < 0) break
            digest.update(buffer, 0, read)
        }
    }
    return digest.digest().joinToString("") { "%02x".format(it) }
}

fun downloadVerified(url: String, expectedSha256: String, destination: File) {
    if (destination.isFile && sha256(destination) == expectedSha256) return

    destination.parentFile.mkdirs()
    val temporary = File(destination.parentFile, "${destination.name}.part")
    URI(url).toURL().openStream().buffered().use { input ->
        temporary.outputStream().buffered().use { output -> input.copyTo(output) }
    }
    val actual = sha256(temporary)
    check(actual == expectedSha256) {
        "Checksum mismatch for $url: expected $expectedSha256, got $actual"
    }
    Files.move(
        temporary.toPath(),
        destination.toPath(),
        StandardCopyOption.ATOMIC_MOVE,
        StandardCopyOption.REPLACE_EXISTING,
    )
}

val provisionAndroidPlatform = tasks.register("provisionAndroidPlatform") {
    group = "build setup"
    description = "Downloads and verifies the Android 35 compile platform"
    outputs.file(androidJar)
    outputs.upToDateWhen {
        androidJar.get().asFile.let { it.isFile && sha256(it) == androidJarSha256 }
    }

    doLast {
        val archive = androidPlatformZip.get().asFile
        downloadVerified(androidPlatformUrl, androidPlatformSha256, archive)
        val output = androidJar.get().asFile
        if (!output.isFile || sha256(output) != androidJarSha256) {
            output.parentFile.mkdirs()
            ZipFile(archive).use { zip ->
                val entry = zip.getEntry("android-35/android.jar")
                    ?: error("android-35/android.jar is missing from $archive")
                zip.getInputStream(entry).use { input ->
                    output.outputStream().buffered().use { stream -> input.copyTo(stream) }
                }
            }
            check(sha256(output) == androidJarSha256) { "Extracted android.jar checksum mismatch" }
        }
    }
}

val provisionR8 = tasks.register("provisionR8") {
    group = "build setup"
    description = "Downloads and verifies the pinned R8/D8 compiler"
    outputs.file(r8Jar)
    outputs.upToDateWhen {
        r8Jar.get().asFile.let { it.isFile && sha256(it) == r8Sha256 }
    }

    doLast {
        downloadVerified(r8Url, r8Sha256, r8Jar.get().asFile)
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.release = 8
    options.encoding = "UTF-8"
    options.compilerArgs.add("-Xlint:-options")
}

tasks.named<JavaCompile>("compileJava") {
    dependsOn(provisionAndroidPlatform)
    classpath += files(androidJar)
}

val unitTest = tasks.register<JavaExec>("unitTest") {
    group = "verification"
    description = "Runs the zero-dependency JVM test suite"
    dependsOn(tasks.testClasses)
    classpath = sourceSets.test.get().runtimeClasspath
    mainClass = "com.adbtool.TestSuite"
}

tasks.test {
    dependsOn(unitTest)
    failOnNoDiscoveredTests = false
    testLogging {
        events("failed", "skipped")
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
    }
}

tasks.jar {
    enabled = false
}

val distributionDirectory = layout.buildDirectory.dir("dist")
val dexJar = distributionDirectory.map { it.file("adbtool-${project.version}.jar") }
val dexChecksum = distributionDirectory.map { it.file("adbtool-${project.version}.jar.sha256") }

val assembleDex = tasks.register<JavaExec>("assembleDex") {
    group = "build"
    description = "Compiles JVM classes into the device-side DEX JAR"
    dependsOn(tasks.classes, provisionAndroidPlatform, provisionR8)
    classpath = files(r8Jar)
    mainClass = "com.android.tools.r8.D8"
    inputs.files(sourceSets.main.get().output.classesDirs)
    inputs.file(androidJar)
    inputs.file(r8Jar)
    outputs.file(dexJar)

    doFirst {
        val output = dexJar.get().asFile
        output.parentFile.mkdirs()
        output.delete()
        val classFiles = sourceSets.main.get().output.classesDirs.files
            .flatMap { directory -> fileTree(directory).matching { include("**/*.class") }.files }
            .sortedBy { it.path }
        check(classFiles.isNotEmpty()) { "No compiled classes were found" }
        args(
            "--min-api", "30",
            "--release",
            "--lib", androidJar.get().asFile.absolutePath,
            "--output", output.absolutePath,
        )
        args(classFiles.map { it.absolutePath })
    }
}

val verifyDexJar = tasks.register("verifyDexJar") {
    group = "verification"
    description = "Verifies that the distribution JAR contains classes.dex"
    dependsOn(assembleDex)
    inputs.file(dexJar)
    inputs.files(sourceSets.main.get().output.classesDirs)

    doLast {
        val entries = ZipFile(dexJar.get().asFile).use { zip ->
            zip.entries().asSequence().map { it.name }.toList()
        }
        check(entries == listOf("classes.dex")) {
            "Distribution JAR must contain only classes.dex; found $entries"
        }

        sourceSets.main.get().output.classesDirs.asFileTree
            .matching { include("**/*.class") }
            .forEach { classFile ->
                classFile.inputStream().buffered().use { input ->
                    val header = ByteArray(8)
                    check(input.read(header) == header.size) { "Invalid class file: $classFile" }
                    val major = ((header[6].toInt() and 0xff) shl 8) or (header[7].toInt() and 0xff)
                    check(major == 52) { "$classFile has class major $major, expected 52 (Java 8)" }
                }
            }
    }
}

val writeDistributionChecksum = tasks.register("writeDistributionChecksum") {
    group = "build"
    description = "Writes the SHA-256 checksum beside the distribution JAR"
    dependsOn(verifyDexJar)
    inputs.file(dexJar)
    outputs.file(dexChecksum)

    doLast {
        val jar = dexJar.get().asFile
        dexChecksum.get().asFile.writeText("${sha256(jar)}  ${jar.name}\n")
    }
}

val shellTest = tasks.register<Exec>("shellTest") {
    group = "verification"
    description = "Runs host wrapper and installer tests with a fake adb"
    dependsOn(writeDistributionChecksum)
    commandLine("bash", "tests/shell/run.sh")
    inputs.files("bin/adbtool", "device/adbtool", "install.sh", "tests/shell/fake-adb", "tests/shell/run.sh")
    inputs.file(dexJar)
    inputs.file(dexChecksum)
}

tasks.check {
    dependsOn(verifyDexJar, shellTest)
}

tasks.assemble {
    dependsOn(writeDistributionChecksum)
}
