package com.example.guet_map.backend.service

import com.example.guet_map.backend.db.NotificationRepository
import com.example.guet_map.backend.db.UserRepository
import com.example.guet_map.backend.db.VerificationCodeRepository
import com.example.guet_map.backend.model.CodeType
import com.example.guet_map.backend.model.LoginResponse
import com.example.guet_map.backend.model.Notification
import com.example.guet_map.backend.model.User
import org.mindrot.jbcrypt.BCrypt
import java.time.LocalDateTime

class AuthService(
    private val userRepository: UserRepository = UserRepository(),
    private val verificationCodeRepository: VerificationCodeRepository = VerificationCodeRepository(),
    private val emailService: EmailService = EmailService(),
    private val jwtService: JwtService = JwtService(),
    private val redisService: RedisService = RedisService.service,
    private val notificationRepository: NotificationRepository = NotificationRepository()
) {
    
    fun sendVerificationCode(email: String, type: String): Result<Unit> {
        // 验证邮箱格式
        if (!isValidEmail(email)) {
            return Result.failure(IllegalArgumentException("无效的邮箱格式"))
        }
        
        // 检查邮箱是否已注册（注册类型）
        val isRegister = type == "register"
        if (isRegister && userRepository.emailExists(email)) {
            return Result.failure(IllegalArgumentException("该邮箱已注册"))
        }
        
        // 检查邮箱是否未注册（登录类型）
        if (!isRegister && !userRepository.emailExists(email)) {
            return Result.failure(IllegalArgumentException("该邮箱未注册，请先注册"))
        }
        
        // 生成验证码
        val code = CodeGenerator.generate6Digit()
        
        // 存入数据库
        val codeType = if (isRegister) CodeType.REGISTER else CodeType.LOGIN
        val verification = com.example.guet_map.backend.model.VerificationCode(
            email = email,
            code = code,
            type = codeType,
            expiresAt = LocalDateTime.now().plusMinutes(10)
        )
        verificationCodeRepository.save(verification)
        
        // 发送邮件
        return emailService.sendVerificationCode(email, code)
    }
    
    fun register(email: String, code: String, password: String, nickname: String): Result<LoginResponse> {
        // 验证邮箱格式
        if (!isValidEmail(email)) {
            return Result.failure(IllegalArgumentException("无效的邮箱格式"))
        }
        
        // 验证密码长度
        if (password.length < 6) {
            return Result.failure(IllegalArgumentException("密码至少6位"))
        }
        
        // 验证昵称
        if (nickname.isBlank() || nickname.length > 50) {
            return Result.failure(IllegalArgumentException("昵称不能为空且最多50字符"))
        }
        
        // 验证验证码
        val validCode = verificationCodeRepository.findValidCodeByEmail(email, CodeType.REGISTER)
        if (validCode == null || validCode.code != code) {
            return Result.failure(IllegalArgumentException("验证码无效或已过期"))
        }
        
        // 检查邮箱是否已注册
        if (userRepository.emailExists(email)) {
            return Result.failure(IllegalArgumentException("该邮箱已注册"))
        }
        
        // 创建用户
        val passwordHash = BCrypt.hashpw(password, BCrypt.gensalt())
        val user = userRepository.create(email, passwordHash, nickname)
        
        // 标记验证码已使用
        verificationCodeRepository.markAsUsed(validCode.id)
        
        // 创建欢迎通知
        notificationRepository.createNotification(
            userId = user.id,
            type = "system",
            title = "欢迎使用 GUET Map",
            body = "感谢注册！校园实景导航已上线，欢迎贡献指路获取积分！"
        )
        
        // 生成JWT token
        val token = jwtService.generateToken(user.id, user.email)
        
        return Result.success(
            LoginResponse(
                token = token,
                nickname = user.nickname,
                points = user.points,
                contributionCount = user.contributionCount,
                userId = user.id
            )
        )
    }
    
    fun login(email: String, password: String): Result<LoginResponse> {
        // 验证邮箱格式
        if (!isValidEmail(email)) {
            return Result.failure(IllegalArgumentException("无效的邮箱格式"))
        }

        // 查找用户
        val user = userRepository.findByEmail(email)
        if (user == null) {
            return Result.failure(IllegalArgumentException("用户不存在，请先注册"))
        }

        // 验证密码
        if (!BCrypt.checkpw(password, user.passwordHash)) {
            return Result.failure(IllegalArgumentException("密码错误"))
        }

        // 生成JWT token
        val token = jwtService.generateToken(user.id, user.email)

        return Result.success(
            LoginResponse(
                token = token,
                nickname = user.nickname,
                points = user.points,
                contributionCount = user.contributionCount,
                userId = user.id
            )
        )
    }

    fun resetPassword(email: String, code: String, newPassword: String): Result<Unit> {
        if (!isValidEmail(email)) {
            return Result.failure(IllegalArgumentException("无效的邮箱格式"))
        }
        if (newPassword.length < 6) {
            return Result.failure(IllegalArgumentException("密码至少6位"))
        }
        val validCode = verificationCodeRepository.findValidCodeByEmail(email, CodeType.RESET_PASSWORD)
        if (validCode == null || validCode.code != code) {
            return Result.failure(IllegalArgumentException("验证码无效或已过期"))
        }
        val user = userRepository.findByEmail(email)
            ?: return Result.failure(IllegalArgumentException("用户不存在"))
        val passwordHash = BCrypt.hashpw(newPassword, BCrypt.gensalt())
        userRepository.updatePassword(user.id, passwordHash)
        verificationCodeRepository.markAsUsed(validCode.id)
        return Result.success(Unit)
    }
    
    fun getUserById(userId: Long): User? {
        return userRepository.findById(userId)
    }
    
    fun getUserByEmail(email: String): User? {
        return userRepository.findByEmail(email)
    }
    
    fun addPoints(userId: Long, points: Int, reason: String) {
        userRepository.incrementPoints(userId, points)
        val user = userRepository.findById(userId)
        if (user != null) {
            notificationRepository.createNotification(
                userId = userId,
                type = "points",
                title = "积分到账",
                body = "$reason +$points 积分，当前积分：${user.points + points}"
            )
        }
    }
    
    private fun isValidEmail(email: String): Boolean {
        val emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$".toRegex()
        return email.matches(emailRegex)
    }
}
