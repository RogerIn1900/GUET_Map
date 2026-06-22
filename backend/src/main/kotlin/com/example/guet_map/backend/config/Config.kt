package com.example.guet_map.backend.config

import org.yaml.snakeyaml.Yaml
import java.io.File
import java.io.InputStream

data class AppConfig(
    val server: ServerConfig,
    val database: DatabaseConfig,
    val redis: RedisConfig,
    val jwt: JwtConfig,
    val mail: MailConfig,
    val cors: CorsConfig
)

data class ServerConfig(
    val host: String,
    val port: Int
)

data class DatabaseConfig(
    val type: String,
    val path: String,
    val host: String,
    val port: Int,
    val username: String,
    val password: String,
    val name: String
)

data class RedisConfig(
    val enabled: Boolean,
    val host: String,
    val port: Int,
    val password: String,
    val database: Int,
    val codePrefix: String,
    val codeExpireSeconds: Int
)

data class JwtConfig(
    val secret: String,
    val expirationHours: Long
)

data class MailConfig(
    val enabled: Boolean,
    val host: String,
    val port: Int,
    val useTls: Boolean,
    val useSsl: Boolean,
    val username: String,
    val password: String,
    val from: String
)

data class CorsConfig(
    val allowedOrigins: List<String>,
    val allowedMethods: List<String>,
    val allowedHeaders: List<String>
)

object ConfigLoader {
    
    private var config: AppConfig? = null
    
    fun load(): AppConfig {
        if (config != null) return config!!
        
        val yaml = Yaml()
        val inputStream: InputStream = try {
            File("src/main/resources/config.yaml").inputStream()
        } catch (e: Exception) {
            try {
                File("config.yaml").inputStream()
            } catch (e2: Exception) {
                ClassLoader.getSystemResourceAsStream("config.yaml") 
                    ?: throw IllegalStateException("Cannot find config.yaml")
            }
        }
        
        val map = yaml.load<Map<String, Any>>(inputStream)
        
        val dbMap = map["database"] as? Map<String, Any> ?: emptyMap()
        val redisMap = map["redis"] as? Map<String, Any> ?: emptyMap()

        config = AppConfig(
            server = ServerConfig(
                host = (map["server"] as? Map<String, Any>)?.get("host") as? String ?: "0.0.0.0",
                port = ((map["server"] as? Map<String, Any>)?.get("port") as? Number)?.toInt() ?: 8080
            ),
            database = DatabaseConfig(
                type = dbMap["type"] as? String ?: "sqlite",
                path = dbMap["path"] as? String ?: "./data/guet_map.db",
                host = dbMap["host"] as? String ?: "localhost",
                port = (dbMap["port"] as? Number)?.toInt() ?: 3306,
                username = dbMap["username"] as? String ?: "root",
                password = dbMap["password"] as? String ?: "",
                name = dbMap["name"] as? String ?: "guet_map"
            ),
            redis = RedisConfig(
                enabled = redisMap["enabled"] as? Boolean ?: false,
                host = redisMap["host"] as? String ?: "localhost",
                port = (redisMap["port"] as? Number)?.toInt() ?: 6379,
                password = redisMap["password"] as? String ?: "",
                database = (redisMap["database"] as? Number)?.toInt() ?: 0,
                codePrefix = redisMap["code-prefix"] as? String ?: "guet_map:code:",
                codeExpireSeconds = (redisMap["code-expire-seconds"] as? Number)?.toInt() ?: 600
            ),
            jwt = JwtConfig(
                secret = (map["jwt"] as? Map<String, Any>)?.get("secret") as? String 
                    ?: "default-secret-change-in-production",
                expirationHours = ((map["jwt"] as? Map<String, Any>)?.get("expiration-hours") as? Number)?.toLong() ?: 720
            ),
            mail = MailConfig(
                enabled = (map["mail"] as? Map<String, Any>)?.get("enabled") as? Boolean ?: false,
                host = (map["mail"] as? Map<String, Any>)?.get("host") as? String ?: "smtp.qq.com",
                port = ((map["mail"] as? Map<String, Any>)?.get("port") as? Number)?.toInt() ?: 465,
                useTls = (map["mail"] as? Map<String, Any>)?.get("use-tls") as? Boolean ?: false,
                useSsl = (map["mail"] as? Map<String, Any>)?.get("use-ssl") as? Boolean ?: true,
                username = (map["mail"] as? Map<String, Any>)?.get("username") as? String ?: "",
                password = (map["mail"] as? Map<String, Any>)?.get("password") as? String ?: "",
                from = (map["mail"] as? Map<String, Any>)?.get("from") as? String ?: ""
            ),
            cors = CorsConfig(
                allowedOrigins = ((map["cors"] as? Map<String, Any>)?.get("allowed-origins") as? List<*>)?.mapNotNull { it as? String } ?: listOf("*"),
                allowedMethods = ((map["cors"] as? Map<String, Any>)?.get("allowed-methods") as? List<*>)?.mapNotNull { it as? String } ?: listOf("GET", "POST", "PUT", "DELETE", "OPTIONS"),
                allowedHeaders = ((map["cors"] as? Map<String, Any>)?.get("allowed-headers") as? List<*>)?.mapNotNull { it as? String } ?: listOf("*")
            )
        )
        
        return config!!
    }
    
    fun get(): AppConfig = config ?: load()
}
