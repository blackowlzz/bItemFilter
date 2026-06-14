plugins {
    id("bitemfilter.java-conventions")
    id("com.gradleup.shadow")
}

dependencies {
    implementation(project(":core"))
    implementation("org.xerial:sqlite-jdbc:3.46.1.3")
    compileOnly("io.papermc.paper:paper-api:1.21.1-R0.1-SNAPSHOT")
    compileOnly("com.google.code.gson:gson:2.10.1")
}

tasks.shadowJar {
    archiveClassifier = ""
    archiveFileName  = "bItemFilter-${project.version}.jar"
    mergeServiceFiles()
}

tasks.build { dependsOn(tasks.shadowJar) }
