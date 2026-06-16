package com.example.guet_map.module.social.data.repository

import android.content.Context
import com.example.guet_map.module.location.data.local.dao.AnnouncementDao
import com.example.guet_map.module.location.data.local.entity.AnnouncementEntity
import com.example.guet_map.module.social.data.model.AnnouncementCategory
import com.example.guet_map.module.social.data.model.CampusAnnouncement
import com.example.guet_map.model.Resource
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CampusAnnouncementRepository @Inject constructor(
    private val announcementDao: AnnouncementDao,
    private val gson: Gson,
    @ApplicationContext private val context: Context
) {
    private val readPrefs = context.getSharedPreferences("campus_announcement_read", Context.MODE_PRIVATE)

    fun getAllAnnouncements(): Flow<List<CampusAnnouncement>> {
        return announcementDao.getAllAnnouncements().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    fun getAnnouncementsByCategory(category: AnnouncementCategory): Flow<List<CampusAnnouncement>> {
        return announcementDao.getAnnouncementsByCategory(category.name).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    suspend fun getAnnouncementById(id: String): CampusAnnouncement? {
        return announcementDao.getAnnouncementById(id)?.toDomain()
    }

    suspend fun markAsRead(id: String) {
        readPrefs.edit().putBoolean(id, true).apply()
        announcementDao.incrementViewCount(id)
    }

    fun isRead(id: String): Boolean {
        return readPrefs.getBoolean(id, false)
    }

    suspend fun seedMockIfEmpty() {
        val existing = announcementDao.getAnnouncementById("mock-1")
        if (existing != null) return

        val mockAnnouncements = listOf(
            CampusAnnouncement(
                id = "mock-1",
                title = "关于2026年端午节放假安排的通知",
                content = "根据学校工作安排，现将2026年端午节放假安排通知如下：\n\n" +
                        "一、放假时间\n" +
                        "6月19日（星期五）至6月21日（星期日）放假，共3天。\n\n" +
                        "二、教学安排\n" +
                        "6月19日（星期五）的课程调至6月14日（星期日）上课。\n\n" +
                        "三、注意事项\n" +
                        "1. 请各学院做好假期期间的教学安排和学生管理工作。\n" +
                        "2. 图书馆6月19日闭馆一天，20日、21日正常开放。\n" +
                        "3. 食堂各餐厅正常营业。\n" +
                        "4. 假期期间请注意人身财产安全。",
                category = AnnouncementCategory.GENERAL,
                priority = 8,
                publishTime = System.currentTimeMillis() - 2 * 24 * 60 * 60 * 1000,
                author = "校长办公室",
                viewCount = 1523,
                isPinned = true
            ),
            CampusAnnouncement(
                id = "mock-2",
                title = "学术报告：深度学习在遥感图像处理中的应用",
                content = "报告题目：深度学习在遥感图像处理中的应用\n\n" +
                        "报告人：张明远 教授（中国科学院遥感与数字地球研究所）\n\n" +
                        "时间：2026年6月16日（星期二）14:30-16:00\n" +
                        "地点：金鸡岭校区 第13教学楼 305会议室\n\n" +
                        "报告摘要：\n" +
                        "随着深度学习技术的快速发展，其在遥感图像目标检测、语义分割、变化检测等方面取得了突破性进展。" +
                        "本报告将系统介绍深度学习在遥感图像处理领域的最新研究进展，包括多模态遥感数据融合、" +
                        "小样本学习、自监督学习等前沿技术，并探讨未来发展方向。\n\n" +
                        "欢迎全校师生参加！",
                category = AnnouncementCategory.ACADEMIC,
                priority = 6,
                publishTime = System.currentTimeMillis() - 3 * 24 * 60 * 60 * 1000,
                author = "计算机与信息安全学院",
                viewCount = 437
            ),
            CampusAnnouncement(
                id = "mock-3",
                title = "2026届毕业生校园招聘会（第六场）通知",
                content = "为促进2026届毕业生高质量就业，学校定于近期举办专场校园招聘会。\n\n" +
                        "时间：2026年6月18日（星期四）9:00-16:00\n" +
                        "地点：花江校区 大学生活动中心一楼大厅\n\n" +
                        "参会企业（部分）：\n" +
                        "1. 华为技术有限公司 —— 软件开发工程师、测试工程师\n" +
                        "2. 腾讯科技（深圳）有限公司 —— 前端/后端开发、产品经理\n" +
                        "3. 中国电子科技集团公司第三十四研究所 —— 嵌入式工程师\n" +
                        "4. 上汽通用五菱汽车股份有限公司 —— 智能驾驶算法工程师\n" +
                        "5. 桂林飞宇科技股份有限公司 —— 无人机系统开发\n" +
                        "6. 本地企业专区 —— 涵盖IT、通信、制造等多个行业\n\n" +
                        "温馨提示：\n" +
                        "• 请携带个人简历（建议5份以上）\n" +
                        "• 着正装参加\n" +
                        "• 提前了解目标企业的招聘需求\n" +
                        "• 部分企业将安排现场笔试和面试",
                category = AnnouncementCategory.CAREER,
                priority = 7,
                publishTime = System.currentTimeMillis() - 4 * 24 * 60 * 60 * 1000,
                author = "就业指导服务中心",
                viewCount = 2341
            ),
            CampusAnnouncement(
                id = "mock-4",
                title = "关于花江校区校园网核心设备升级的通知",
                content = "各位师生：\n\n" +
                        "为提升校园网络服务质量，信息中心计划对花江校区核心网络设备进行升级改造。\n\n" +
                        "一、升级时间\n" +
                        "2026年6月21日（星期日）0:00-6:00\n\n" +
                        "二、影响范围\n" +
                        "升级期间，花江校区以下区域的校园网将中断：\n" +
                        "• 第1-8教学楼\n" +
                        "• 图书馆\n" +
                        "• 行政办公楼\n" +
                        "• 各学生宿舍楼\n\n" +
                        "三、注意事项\n" +
                        "1. 请提前保存好在线文档和作业。\n" +
                        "2. 手机移动数据不受影响，紧急情况可使用4G/5G网络。\n" +
                        "3. 如遇升级延迟，将另行通知。\n\n" +
                        "由此带来的不便，敬请谅解。",
                category = AnnouncementCategory.MAINTENANCE,
                priority = 5,
                publishTime = System.currentTimeMillis() - 5 * 24 * 60 * 60 * 1000,
                author = "信息中心",
                viewCount = 876
            ),
            CampusAnnouncement(
                id = "mock-5",
                title = "\"科创未来\"——2026年校园科技文化节活动通知",
                content = "为激发学生科技创新热情，丰富校园文化生活，学校将举办2026年校园科技文化节。\n\n" +
                        "活动主题：科创未来 · 智汇桂电\n" +
                        "活动时间：2026年6月22日-28日\n" +
                        "活动地点：花江校区科技广场及周边\n\n" +
                        "主要活动安排：\n\n" +
                        "【开幕式暨科技成果展】6月22日 9:00-12:00\n" +
                        "展示各学院最新科研成果和学生创新项目\n\n" +
                        "【程序设计竞赛】6月23日 14:00-18:00\n" +
                        "花江校区计算中心，三人一队，线上报名\n\n" +
                        "【智能车竞赛】6月24日 全天\n" +
                        "科技广场赛道，展示自动驾驶和机器人技术\n\n" +
                        "【创新创业路演】6月25日 14:00-17:00\n" +
                        "大学生活动中心，优秀项目路演展示\n\n" +
                        "【学术海报展览】6月22日-28日 全天\n" +
                        "图书馆一楼大厅\n\n" +
                        "【闭幕式暨颁奖典礼】6月28日 19:00-21:00\n" +
                        "大学生活动中心礼堂\n\n" +
                        "报名方式：关注\"桂电青年\"公众号在线报名\n" +
                        "主办单位：团委、科技处、创新创业学院",
                category = AnnouncementCategory.ACTIVITY,
                priority = 7,
                publishTime = System.currentTimeMillis() - 1 * 24 * 60 * 60 * 1000,
                author = "校团委",
                viewCount = 3201,
                isPinned = true
            ),
            CampusAnnouncement(
                id = "mock-6",
                title = "关于2026年秋季学期选课的通知",
                content = "根据学校教学工作安排，2026年秋季学期选课工作即将开始。\n\n" +
                        "一、选课时间安排\n" +
                        "第一轮（预选）：6月25日 9:00 - 6月28日 17:00\n" +
                        "第二轮（正选）：7月2日 9:00 - 7月5日 17:00\n" +
                        "第三轮（补退选）：9月7日 9:00 - 9月11日 17:00\n\n" +
                        "二、选课平台\n" +
                        "登录教务管理系统（http://jwgl.guet.edu.cn）进行选课\n\n" +
                        "三、选课说明\n" +
                        "1. 预选阶段不限选课人数，超出容量的课程将随机筛选。\n" +
                        "2. 正选阶段实行\"先选先得\"原则。\n" +
                        "3. 请各位同学务必在规定时间内完成选课。\n" +
                        "4. 通识选修课毕业要求修满8学分，请合理规划。\n" +
                        "5. 体育课为必修课程，每学期均需选课。\n\n" +
                        "如有疑问，请联系各学院教学秘书。",
                category = AnnouncementCategory.GENERAL,
                priority = 9,
                publishTime = System.currentTimeMillis() - 12 * 60 * 60 * 1000,
                author = "教务处",
                viewCount = 4500,
                isPinned = true
            ),
            CampusAnnouncement(
                id = "mock-7",
                title = "金鸡岭校区部分区域停电通知（紧急）",
                content = "因校外供电线路检修，金鸡岭校区部分区域将于以下时段停电：\n\n" +
                        "停电时间：2026年6月15日（星期一）23:00 - 6月16日 5:00\n\n" +
                        "影响区域：\n" +
                        "• 第1-5教学楼\n" +
                        "• 第1-8栋学生宿舍\n" +
                        "• 第一食堂\n\n" +
                        "温馨提示：\n" +
                        "1. 请提前关闭电脑等电子设备，防止数据丢失。\n" +
                        "2. 停电期间电梯将停止运行，请提前做好准备。\n" +
                        "3. 图书馆正常供电，有需要的同学可前往学习。\n" +
                        "4. 如遇恶劣天气，停电时间可能调整。\n\n" +
                        "后勤保障处值班电话：0773-2291111",
                category = AnnouncementCategory.EMERGENCY,
                priority = 10,
                publishTime = System.currentTimeMillis() - 6 * 60 * 60 * 1000,
                author = "后勤保障处",
                viewCount = 6780
            ),
            CampusAnnouncement(
                id = "mock-8",
                title = "\"人工智能时代的机遇与挑战\"——杰出校友讲座",
                content = "校友讲坛（第42期）\n\n" +
                        "题目：人工智能时代的机遇与挑战\n\n" +
                        "主讲人：刘建国\n" +
                        "• 我校2005级计算机科学与技术专业校友\n" +
                        "• 现任字节跳动AI Lab高级技术总监\n" +
                        "• 曾任微软亚洲研究院研究员\n" +
                        "• 在CVPR/ICCV/NeurIPS等顶级会议发表论文30余篇\n\n" +
                        "时间：2026年6月19日（星期五）19:00-21:00\n" +
                        "地点：花江校区 图书馆学术报告厅\n\n" +
                        "讲座内容：\n" +
                        "1. AI技术的演进：从深度学习到大模型时代\n" +
                        "2. 大语言模型的核心技术与应用场景\n" +
                        "3. AI对IT行业的重塑——程序员该何去何从\n" +
                        "4. 在校学生如何应对AI时代的职业挑战\n" +
                        "5. 互动交流环节\n\n" +
                        "本讲座纳入第二课堂学分认定，请携带校园卡签到。",
                category = AnnouncementCategory.ACADEMIC,
                priority = 6,
                publishTime = System.currentTimeMillis() - 2 * 24 * 60 * 60 * 1000,
                author = "校友工作办公室",
                viewCount = 1890
            )
        )

        announcementDao.insertAnnouncements(mockAnnouncements.map { it.toEntity() })
    }

    private fun AnnouncementEntity.toDomain() = CampusAnnouncement(
        id = id,
        title = title,
        content = content,
        category = try { AnnouncementCategory.valueOf(category) } catch (_: Exception) { AnnouncementCategory.GENERAL },
        priority = priority,
        publishTime = publishTime,
        author = author,
        viewCount = viewCount,
        isPinned = isPinned,
        isRead = isRead(id)
    )

    private fun CampusAnnouncement.toEntity() = AnnouncementEntity(
        id = id,
        title = title,
        content = content,
        category = category.name,
        priority = priority,
        publishTime = publishTime,
        author = author,
        images = gson.toJson(emptyList<String>()),
        attachments = gson.toJson(emptyList<String>()),
        viewCount = viewCount,
        isPinned = isPinned
    )
}
