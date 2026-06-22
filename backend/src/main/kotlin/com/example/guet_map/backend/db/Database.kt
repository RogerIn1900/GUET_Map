package com.example.guet_map.backend.db

import com.example.guet_map.backend.config.ConfigLoader
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory

object Database {
    
    private val logger = LoggerFactory.getLogger(Database::class.java)
    
    fun init() {
        val config = ConfigLoader.get()
        val dbConfig = config.database
        
        val url = when (dbConfig.type.lowercase()) {
            "mysql" -> "jdbc:mysql://${dbConfig.host}:${dbConfig.port}/${dbConfig.name}?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true"
            else -> "jdbc:sqlite:${dbConfig.path}"
        }
        
        logger.info("Connecting to database: ${dbConfig.type} at $url")
        
        Database.connect(url, driver = when (dbConfig.type.lowercase()) {
            "mysql" -> "com.mysql.cj.jdbc.Driver"
            else -> "org.sqlite.JDBC"
        }, user = dbConfig.username, password = dbConfig.password)
        
        // 创建表结构
        transaction {
            SchemaUtils.create(Users)
            SchemaUtils.create(VerificationCodes)
            SchemaUtils.create(UserNotifications)
            logger.info("Database tables created/verified successfully")
        }
    }
}
