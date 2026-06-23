// 诊断 Gradle/AGP 实际使用的 JDK 路径
// 运行: ./gradlew -p app printActualJdkHome

tasks.register("printActualJdkHome") {
    doLast {
        val logFile = java.io.File(project.rootDir, ".cursor/debug-d1f364.log")
        logFile.parentFile().mkdirs()
        val ts = System.currentTimeMillis()
        fun write(payload: Map<String, Any?>) {
            val line = org.json.JSONObject(mapOf("id" to "log_${ts}_${payload.hashCode()}") + payload).toString()
            logFile.appendText(line + System.lineSeparator(), Charsets.UTF_8)
        }
        write(mapOf("sessionId" to "d1f364", "runId" to "diagnose", "hypothesisId" to "JDK_RESOLVE", "location" to "jdk_info_task.gradle.kts:printActualJdkHome", "message" to "printActualJdkHome", "data" to mapOf(
            "gradleVersion" to org.gradle.util.GradleVersion.current().version,
            "javaHome" to System.getProperty("java.home"),
            "orgGradleJavaHome" to System.getProperty("org.gradle.java.home"),
            "jdkHome" to project.findProperty("org.gradle.java.home"),
            "envJdk1" to System.getenv("JDK17_HOME"),
            "envJdk2" to System.getenv("JAVA17_HOME"),
            "envJavaHome" to System.getenv("JAVA_HOME"),
            "envPath" to System.getenv("PATH"),
            "jlinkFromJavaHome" to listOf(System.getProperty("java.home"), "bin", "jlink").joinToString("/"),
            "jlinkFromGradleHome" to listOf(project.findProperty("org.gradle.java.home") as String?, "bin", "jlink").filterNotNull().joinToString("/")
        )))
    }
}
