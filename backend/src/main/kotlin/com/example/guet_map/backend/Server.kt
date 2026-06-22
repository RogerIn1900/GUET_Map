package com.example.guet_map.backend

import com.example.guet_map.backend.config.ConfigLoader
import com.example.guet_map.backend.db.Database
import com.example.guet_map.backend.routes.authRoutes
import com.example.guet_map.backend.routes.notificationRoutes
import com.example.guet_map.backend.service.AuthService
import com.example.guet_map.backend.service.JwtService
import com.example.guet_map.backend.service.NotificationService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.serialization.gson.*
import io.ktor.server.routing.*
import org.slf4j.LoggerFactory
import java.io.File

fun main() {
    // 加载配置
    val config = ConfigLoader.load()
    
    // 初始化数据库
    Database.init()
    
    // 确保日志目录存在
    File("logs").mkdirs()
    
    // 创建服务实例
    val authService = AuthService()
    val jwtService = JwtService()
    val notificationService = NotificationService()
    
    // 启动服务器
    embeddedServer(Netty, port = config.server.port, host = config.server.host) {
        
        // 安装插件
        install(ContentNegotiation) {
            gson {
                setPrettyPrinting()
                setLenient()
            }
        }
        
        install(CORS) {
            allowMethod(HttpMethod.Get)
            allowMethod(HttpMethod.Post)
            allowMethod(HttpMethod.Put)
            allowMethod(HttpMethod.Delete)
            allowMethod(HttpMethod.Options)
            allowHeader(HttpHeaders.ContentType)
            allowHeader(HttpHeaders.Authorization)
            allowHeader(HttpHeaders.AccessControlAllowOrigin)
            allowCredentials = true
            
            val allowedOrigins = config.cors.allowedOrigins
            if (allowedOrigins.contains("*")) {
                anyHost()
            } else {
                allowedOrigins.forEach { origin ->
                    val host = origin.removePrefix("http://").removePrefix("https://").substringBefore(":")
                    allowHost(host, listOf(HttpHeaders.AccessControlAllowOrigin))
                }
            }
        }
        
        // 打印请求日志
        install(io.ktor.server.plugins.callloging.CallLogging) {
            logger = LoggerFactory.getLogger("ktor.call")
        }
        
        // 路由
        routing {
            // 健康检查
            get("/health") {
                call.respond("OK")
            }

            get("/api/v1/health") {
                call.respond(successResponse(mapOf(
                    "status" to "running",
                    "version" to "1.0.0",
                    "timestamp" to System.currentTimeMillis()
                )))
            }
            
            // 认证相关路由
            authRoutes(authService, jwtService, notificationService)
            
            // 通知相关路由
            notificationRoutes(notificationService, jwtService)
        }
        
    }.start(wait = true)
}

private fun successResponse(data: Map<String, Any>) = mapOf(
    "success" to true,
    "data" to data
)
