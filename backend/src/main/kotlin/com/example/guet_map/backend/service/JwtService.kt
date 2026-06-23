package com.example.guet_map.backend.service

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.example.guet_map.backend.config.ConfigLoader
import java.util.Date

class JwtService {
    
    private val config = ConfigLoader.get().jwt
    private val algorithm = Algorithm.HMAC256(config.secret)
    
    fun generateToken(userId: Long, email: String): String {
        return JWT.create()
            .withSubject(userId.toString())
            .withClaim("email", email)
            .withExpiresAt(Date(System.currentTimeMillis() + config.expirationHours * 60 * 60 * 1000))
            .sign(algorithm)
    }
    
    fun verifyToken(token: String): TokenPayload? {
        return try {
            val verifier = JWT.require(algorithm).build()
            val decoded = verifier.verify(token)
            TokenPayload(
                userId = decoded.subject.toLong(),
                email = decoded.getClaim("email").asString()
            )
        } catch (e: Exception) {
            null
        }
    }
}

data class TokenPayload(
    val userId: Long,
    val email: String
)
