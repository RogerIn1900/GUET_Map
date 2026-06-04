# GUET Map 完善版 — 需求与功能大纲

> 本文档为产品完善后的需求与功能规格，与当前代码实现对齐。技术细节见 [PROJECT.md](PROJECT.md)。

## 一、项目定位

| 维度 | 说明 |
|------|------|
| 产品名称 | GUET Map（桂林电子科技大学校园微导航） |
| 目标校区 | 桂电花江校区（可扩展多校区） |
| 核心痛点 | 传统地图在校园内「最后 50 米」寻路困难 |
| 核心解法 | **实景图文指引**（「商家指路」式分步：文字 + 实景照片） |
| 运营模式 | 官方 POI 底图 + **UGC 共创**指引；审核通过后 **积分激励** |
| 客户端 | Android 原生（Kotlin），单 Activity + 5 Tab 底部导航 |
| 技术栈 | MVVM + Hilt + Room + Retrofit + Coil + 高德 3D 地图 SDK（GCJ-02） |

## 二、用户与场景

**目标用户**：桂电在校学生、教职工、访客。

**典型场景**：

1. 搜索地点并查看「商家指路」走到门口。
2. 按分类筛选 POI，点 Marker 查看详情与指引。
3. 上传分步 UGC 指引，赚取积分。
4. 收藏常去地点，接收审核与积分通知。
5. 弱网/离线浏览已缓存地点与指引。

## 三、功能需求（按模块）

### 3.1 地图主页

**已实现**：高德地图、POI Marker、分类筛选（含「全部」切换）、本地搜索、系统定位、BottomSheet 详情、商家指路、封面图、导航/收藏/分享、无指引时引导贡献。

### 3.2 探索

热门推荐、分类入口、最新 UGC 列表。

### 3.3 收藏

本地 + 服务端同步收藏列表，点击进入地图详情。

### 3.4 我来指路（UGC）

多步表单、相册/相机、图片压缩、草稿箱、提交与积分、我的提交状态。

### 3.5 通知

审核结果、积分变动、公告；未读标记；点击跳转地图或 UGC。

### 3.6 用户与账号

登录（Mock/真实 Token）、个人资料入口、OkHttp Token 注入。

## 四、API

### 已有

- `GET /api/v1/locations`
- `GET /api/v1/locations/{id}`
- `GET /api/v1/locations/{id}/guides`
- `POST /api/v1/guides/upload`

### 扩展

- `GET /api/v1/categories`
- `GET/POST/DELETE /api/v1/favorites`
- `GET /api/v1/guides/recent`
- `GET /api/v1/guides/mine`
- `GET /api/v1/notifications`
- `POST /api/v1/auth/login`

## 五、非功能需求

性能、离线缓存、权限合规、HTTPS + Token、GCJ-02 坐标、minSdk 24 / targetSdk 35。

## 六、实施阶段

| 阶段 | 范围 |
|------|------|
| Phase 1 | 地图核心 + UGC 上传 + Mock |
| Phase 2 | 收藏/导航/分享、BuildConfig 切换 Mock |
| Phase 3 | 探索/收藏/通知 Tab、登录、扩展 API |
| Phase 4 | 语音搜索、图片压缩、草稿、本地推送通道 |

## 七、验收标准（完善版 MVP）

1. 地图可搜索、筛选、定位并查看图文指引。
2. UGC 可提交，可查看我的提交状态与通知。
3. 探索、收藏、通知 Tab 功能完整。
4. 离线可访问已缓存数据。
5. Release 构建对接真实 API（`USE_MOCK_API=false`）。
