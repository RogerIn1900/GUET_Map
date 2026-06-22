package com.example.guet_map.backend.service

import com.example.guet_map.backend.config.ConfigLoader
import redis.clients.jedis.Jedis

class RedisService private constructor() {
    
    private var jedis: Jedis? = null
    private val config = ConfigLoader.get().redis
    
    init {
        if (config.enabled) {
            try {
                jedis = Jedis(config.host, config.port, 5000)
                if (config.password.isNotBlank()) {
                    jedis?.auth(config.password)
                }
                jedis?.select(config.database)
                println("Redis connected successfully")
            } catch (e: Exception) {
                println("Failed to connect to Redis: ${e.message}")
                jedis = null
            }
        }
    }
    
    fun isEnabled(): Boolean = config.enabled && jedis != null
    
    fun setVerificationCode(email: String, code: String) {
        if (!isEnabled()) return
        try {
            val key = "${config.codePrefix}$email"
            jedis?.setex(key, config.codeExpireSeconds.toLong(), code)
        } catch (e: Exception) {
            println("Redis setVerificationCode error: ${e.message}")
        }
    }
    
    fun verifyCode(email: String, code: String): Boolean {
        if (!isEnabled()) return false
        try {
            val key = "${config.codePrefix}$email"
            val storedCode = jedis?.get(key)
            return storedCode == code
        } catch (e: Exception) {
            println("Redis verifyCode error: ${e.message}")
            return false
        }
    }
    
    companion object {
        val service: RedisService by lazy { RedisService() }
    }
}
