plugins { java }

group   = "it.blackowlzz"
version = "1.0.0"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
    maven("https://oss.sonatype.org/content/repositories/snapshots/")
}

java { toolchain { languageVersion = JavaLanguageVersion.of(21) } }

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release  = 21
}
