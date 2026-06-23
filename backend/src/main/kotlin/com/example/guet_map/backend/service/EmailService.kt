package com.example.guet_map.backend.service

import com.example.guet_map.backend.config.ConfigLoader
import javax.mail.*
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeMessage
import java.util.Properties
import kotlin.random.Random

class EmailService {
    
    private val config = ConfigLoader.get().mail
    private var session: Session? = null
    
    init {
        if (config.enabled) {
            initSession()
        }
    }
    
    private fun initSession() {
        val props = Properties().apply {
            put("mail.smtp.host", config.host)
            put("mail.smtp.port", config.port.toString())
            put("mail.smtp.auth", "true")
            if (config.useSsl) {
                put("mail.smtp.ssl.enable", "true")
                put("mail.smtp.socketFactory.port", config.port.toString())
                put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory")
                put("mail.smtp.socketFactory.fallback", "false")
            } else if (config.useTls) {
                put("mail.smtp.starttls.enable", "true")
                put("mail.smtp.starttls.required", "true")
            }
            put("mail.smtp.ssl.trust", config.host)
        }

        session = Session.getInstance(props, object : Authenticator() {
            override fun getPasswordAuthentication(): PasswordAuthentication {
                return PasswordAuthentication(config.username, config.password)
            }
        })

        println("Email service initialized with host: ${config.host}:${config.port}, ssl: ${config.useSsl}, tls: ${config.useTls}")
    }
    
    fun isEnabled(): Boolean = config.enabled
    
    fun sendVerificationCode(toEmail: String, code: String): Result<Unit> {
        if (!config.enabled) {
            println("[DEV MODE] Verification code for $toEmail: $code")
            return Result.success(Unit)
        }
        
        return try {
            val message = MimeMessage(session!!).apply {
                setFrom(InternetAddress(config.from))
                setRecipient(Message.RecipientType.TO, InternetAddress(toEmail))
                subject = "【GUET Map】您的注册验证码"
                setContent(buildEmailContent(code), "text/html; charset=UTF-8")
            }
            
            Transport.send(message)
            println("Verification code sent to $toEmail")
            Result.success(Unit)
        } catch (e: Exception) {
            println("Failed to send email: ${e.message}")
            Result.failure(e)
        }
    }
    
    private fun buildEmailContent(code: String): String {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <style>
                    body { font-family: Arial, sans-serif; background-color: #f5f5f5; margin: 0; padding: 20px; }
                    .container { max-width: 500px; margin: 0 auto; background: white; border-radius: 8px; overflow: hidden; box-shadow: 0 2px 10px rgba(0,0,0,0.1); }
                    .header { background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white; padding: 30px; text-align: center; }
                    .header h1 { margin: 0; font-size: 24px; }
                    .content { padding: 30px; text-align: center; }
                    .code-box { background: #f8f9fa; border: 2px dashed #dee2e6; border-radius: 8px; padding: 20px 40px; display: inline-block; margin: 20px 0; }
                    .code { font-size: 32px; font-weight: bold; color: #667eea; letter-spacing: 8px; }
                    .tips { color: #6c757d; font-size: 14px; line-height: 1.8; }
                    .footer { background: #f8f9fa; padding: 20px; text-align: center; color: #6c757d; font-size: 12px; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>GUET Map</h1>
                    </div>
                    <div class="content">
                        <h2 style="color: #333; margin-bottom: 20px;">邮箱验证码</h2>
                        <div class="code-box">
                            <div class="code">$code</div>
                        </div>
                        <div class="tips">
                            <p>您的验证码有效期为 <strong>10 分钟</strong></p>
                            <p>请勿将验证码告知他人</p>
                            <p>如果是您本人操作，请忽略此邮件</p>
                        </div>
                    </div>
                    <div class="footer">
                        <p>桂林电子科技大学校园导航</p>
                        <p>此邮件由系统自动发送，请勿回复</p>
                    </div>
                </div>
            </body>
            </html>
        """.trimIndent()
    }
}

object CodeGenerator {
    
    fun generate(): String {
        return Random.nextInt(100000, 999999).toString()
    }
    
    fun generate6Digit(): String {
        return String.format("%06d", Random.nextInt(999999))
    }
}
