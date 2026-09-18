# 不美鸡课表维护交接

本文档给后续更换窗口、模型或开发者使用。开始修改前先读本文件，再读项目根目录的 `README.md` 和 `docs/TEST_RESULTS.md`。本文记录的是截至 v1.2.2 的真实实现和验证边界；不要把“未实测”写成“已支持”，也不要为了实现新需求删除旧版 APK 或破坏本地数据兼容。

## 1. 项目身份与当前基线

- 产品名：不美鸡课表
- 作者显示名：一只不太美的鸡
- 包名：`com.example.classschedule`
- 根项目：`E:\codex-work\outputs\generated\2026-09-17\class-schedule-android`
- 当前版本：`versionCode 4`，`versionName 1.2.2`
- 最低系统：Android API 26
- compile/target SDK：36
- 技术栈：Kotlin、Jetpack Compose、Material 3、Room、DataStore Preferences、Navigation Compose、ViewModel、Coroutines/Flow、AlarmManager
- 运行方式：离线、本地存储、无账号、无后端、无网络依赖
- GitHub 公开仓库：`https://github.com/yzbtmdj/bumeiji-class-schedule`
- 主分支：`main`
- 本交接文件创建前的基线提交：`23051578856b1a6910e3073f67aced95cfb08990`。创建本文件后的最新提交以 `git log -1` 为准。

### 重要不变量

1. 保持包名 `com.example.classschedule`，否则覆盖升级和本地数据都会失效。
2. 保持离线能力。不要引入登录、后端、网络请求或在线黄历服务。
3. Room、DataStore 的旧数据必须继续可读。涉及实体字段时要同步 schema、迁移和迁移测试。
4. 导入只有在得到真实开始/结束时间后才能写入。不能用示例作息猜测学校钟点；`timeSlotsConfigured` 和节次确认页是刻意设计的保护逻辑。
5. v1.2.2 已取消页面进入、退出、返回动画和底部导航点击涟漪。后续 UI 改动不要重新加入拖影或过渡动画。
6. 历史 APK 只读保留，不覆盖、不删除；不要把调试 APK 当作 release 签名包宣传。

## 2. 已实现功能

### 今日页和黄历

- 今日页显示日期、下一节课，并按上午、下午、晚上分组。
- 日期下方可显示今日黄历，设置中由 `showAlmanac` 开关控制。
- `ui/Almanac.kt` 使用 `cn.6tail:lunar:1.7.7`（lunar-java）离线计算农历、干支、生肖、详细“宜”和“忌”。不是四年静态表，也不需要网络。
- 已用 1900、2022、2026、2030、2099、2100、2101 等锚点确认宜忌字段有数据；仍需留意第三方库对极端日期的边界行为。

### 周课表

- 星期横向、节次纵向滚动。
- 支持双指缩放和缩小/恢复/放大按钮，范围 `0.75x` 到 `2.0x`，值保存在 DataStore。
- 课程块按真实 `startMinutes`/`endMinutes` 计算高度，不再固定占一格。用户课表中的“网络空间安全法律法规与伦理”第 3-5 节显示为 `09:50-12:00` 并跨三节。
- 同一时间发生冲突时使用分栏布局，核心计算在 `domain/WeekLayout.kt`。
- 课程有稳定的自动颜色，也能在课程编辑页手动选择颜色；颜色存于 `CourseEntity.colorHex`。

### 课程管理

- 课程新增、编辑、删除；一门课程可以拥有多条 Meeting，分别维护星期、时间、周次、地点和教师。
- 删除课程有 Snackbar 撤销。
- `domain/CourseColors.kt` 负责颜色分配；`ScheduleRepository.ensureCourseColors()` 负责兼容旧数据并给旧课程补色。

### 导入和导出

- 支持 CSV、标准 XLSX、传统 OLE/BIFF XLS。
- 支持教务系统格状课表；预览包含来源单元格、原始内容、解析结果和错误/冲突。
- 支持字段映射；重复项可以合并、覆盖或取消。
- 可解析课程名、星期、周次、节次、地点、教师。
- 使用节次但没有配置作息时，必须进入节次确认页，用户填写具体时间后才能继续。
- 必填字段：课程名、星期、开始时间、结束时间。周次支持每周、单双周、指定集合、范围、日期范围。
- CSV 模板位于 `templates/class_schedule_template.csv`。
- 用户附件“网安24课表(20260904155930920).xls”已实测：识别为教务系统格状课表，预览 9 条有效记录，最终形成 6 门课程。
- 附件中的课程验证过地点、教师、来源单元格和第 6-9 节跨节数据；原始附件在用户微信临时目录，不是项目可移植资源。测试可通过环境变量 `CLASS_SCHEDULE_XLS_PATH` 指向本地文件。

### 提醒

- 上午、下午、晚上三阶段；默认边界为上午 `< 12:00`、下午 `12:00-18:00`、晚上 `>= 18:00`。
- 默认提醒时间 `07:00`、`12:00`、`18:00`。
- InboxStyle 通知显示时间、课程名和地点；默认无课不提醒，可在设置开启。
- 课程、提醒设置、重启、时区变化、系统时间变化、应用升级后重新排程。
- Android 13+ 申请 `POST_NOTIFICATIONS`。
- Android 12+ 精确闹钟权限被拒绝时降级为非精确 AlarmManager，并显示“可能延迟”。
- 点击通知回到今日页并携带阶段定位状态。

### 设置和品牌

- 设置：学期、节次时间、阶段边界、提醒时间、通知开关、无课提醒、主题、CSV 导入导出。
- 修复了 v1.1 中“在设置页保存有效学期后误回首次建立流程”的问题，并保留旧 DataStore 兼容逻辑。
- v1.2.1 加入“打赏作者”页，支付宝和微信二维码默认隐藏，点击对应项目后分别显示。
- 应用标签为“不美鸡课表”，设置底部显示版本和“作者：一只不太美的鸡”。
- 当前图标为 `app/src/main/res/drawable/app_icon.png`；二维码资源为 `donate_alipay.jpg` 和 `donate_wechat.jpg`。
- v1.2.2 的导航动画和底部导航涟漪已移除。

## 3. 关键源码位置

| 路径 | 责任 |
| --- | --- |
| `app/src/main/java/com/example/classschedule/ui/ScheduleRoot.kt` | 主 Compose 壳、底部导航、NavHost、今日/周课表/课程/设置/导入/打赏页面。导航的 `EnterTransition.None`、`ExitTransition.None` 在这里。 |
| `app/src/main/java/com/example/classschedule/ui/MainViewModel.kt` | 页面状态、课程 CRUD、导入确认、设置保存、重排提醒。 |
| `app/src/main/java/com/example/classschedule/ui/Almanac.kt` | 黄历数据模型和 lunar-java 离线计算。 |
| `app/src/main/java/com/example/classschedule/ui/theme/Theme.kt` | Material 3 主题。 |
| `app/src/main/java/com/example/classschedule/domain/ScheduleDomain.kt` | 学期、周次规则、阶段分类、时间解析、设置模型。 |
| `app/src/main/java/com/example/classschedule/domain/WeekLayout.kt` | 真实时间高度、跨节和冲突分栏。 |
| `app/src/main/java/com/example/classschedule/domain/CourseColors.kt` | 自动课程颜色。 |
| `app/src/main/java/com/example/classschedule/domain/ReminderText.kt` | 通知文本。 |
| `app/src/main/java/com/example/classschedule/data/Entities.kt` | Room 的 `CourseEntity`、`MeetingEntity`。课程颜色在 `colorHex`，Meeting 同时支持钟点和节次 ID。 |
| `app/src/main/java/com/example/classschedule/data/AppDatabase.kt` | Room 数据库版本 2 和 `MIGRATION_1_2`。 |
| `app/src/main/java/com/example/classschedule/data/Daos.kt` | Room DAO。 |
| `app/src/main/java/com/example/classschedule/data/ScheduleRepository.kt` | 课程/Meeting 持久化、重复导入、颜色补全。 |
| `app/src/main/java/com/example/classschedule/data/SettingsRepository.kt` | DataStore 偏好读取和写入。 |
| `app/src/main/java/com/example/classschedule/data/SettingsCompatibility.kt` | 旧版本学期保存和 onboarding 状态兼容。 |
| `app/src/main/java/com/example/classschedule/data/ImportParser.kt` | CSV/XLSX、字段映射、预览和校验。 |
| `app/src/main/java/com/example/classschedule/data/LegacyXlsParser.kt` | OLE/BIFF XLS 和教务格状课表解析。 |
| `app/src/main/java/com/example/classschedule/data/AlarmScheduler.kt` | AlarmManager 精确/降级排程。 |
| `app/src/main/java/com/example/classschedule/data/ReminderReceiver.kt` | 通知发送。 |
| `app/src/main/java/com/example/classschedule/data/RescheduleReceiver.kt` | 启动、重启、时区/时间变化、升级后的重排。 |
| `app/src/main/AndroidManifest.xml` | 通知和精确闹钟权限、Receiver 注册。 |
| `app/src/androidTest/java/com/example/classschedule/RoomMigrationTest.kt` | Room 1 -> 2 迁移测试。 |
| `app/src/androidTest/java/com/example/classschedule/UiSmokeTest.kt` | 首次启动中文 UI 烟测。 |

### 数据结构注意点

`CourseEntity` 的核心字段为 `name`、`teacher`、`location`、`colorHex`、`note`、`source`；`MeetingEntity` 的核心字段为 `courseId`、`weekday`、`startMinutes`、`endMinutes`、`startSlotId`、`endSlotId`、`weekRule` 和可选日期范围。数据库现在是 v2，v1 -> v2 只新增 `courses.source`，迁移不可删除。

## 4. 构建和验证

### 本机工具链

```powershell
$env:JAVA_HOME = 'D:\codex-tools\class-schedule\jdk17'
$gradle = 'D:\codex-tools\class-schedule\gradle-8.13\bin\gradle.bat'
$adb = 'D:\leidian\LDPlayer9\adb.exe'
```

项目使用 JDK 17、Gradle 8.13、Android SDK Platform 36、Build Tools 36.1.0。优先使用上面的本机 Gradle，不要因为网络问题重新下载 Wrapper 或 JDK。

### 推荐验证顺序

```powershell
Set-Location 'E:\codex-work\outputs\generated\2026-09-17\class-schedule-android'
$env:JAVA_HOME = 'D:\codex-tools\class-schedule\jdk17'
$gradle = 'D:\codex-tools\class-schedule\gradle-8.13\bin\gradle.bat'

& $gradle assembleDebug --no-daemon
& $gradle assembleDebugAndroidTest --no-daemon
& $gradle lintDebug --no-daemon
& $gradle connectedDebugAndroidTest --no-daemon
```

如果设备被旧 DataStore/Room 数据污染，先只清这个包的数据，再重跑设备测试：

```powershell
& 'D:\leidian\LDPlayer9\adb.exe' shell pm clear com.example.classschedule
```

不要用 `git clean`、递归删除或覆盖命令清理项目；历史 APK 和输出应保留。

### 已通过的验证

- `assembleDebug`：通过。
- `assembleDebugAndroidTest`：通过。
- `lintDebug`：通过，0 errors、8 warnings；警告是版本提示、图标资源和已有 Compose 图标弃用提示。
- 直接 JUnit：22/22 通过，覆盖周次、阶段、通知文本、CSV/XLSX、传统 XLS、冲突、节次时间、学期兼容、颜色、跨节布局、黄历。
- `RoomMigrationTest`：雷电模拟器 Android 9 / API 28 上 1/1 通过。
- `connectedDebugAndroidTest`：2/2 通过，包含 Room 迁移和首次启动“不美鸡课表”检查。
- 雷电模拟器覆盖安装 v1.2.2 后，页面切换、设置版本、今日/周课表/课程/设置页面均可显示；导航无拖影。
- 真实 XLS 附件解析 9 条记录通过；周一网络空间安全课程显示 `09:50-12:00`。

### 尚未完成的验证

- Android 8、Android 13、Android 16 真机/可用模拟器 UI 尚未实测。
- Android 13 `POST_NOTIFICATIONS` 的真实授权流程尚未实测。
- Android 12+ 精确闹钟授权被拒绝/允许、真实通知触发、厂商后台限制尚未完整实测。
- Gradle `testDebugUnitTest` 尚未正常启动：本机 Gradle worker 报 `ClassNotFoundException: worker.org.gradle.process.internal.worker.GradleWorkerMain`。这不是断言失败；当前只能引用同一编译产物的直接 JUnit 22/22 结果，换正常 Gradle worker 环境后应补跑正式任务。
- 不要声称已经生成 Android 16 截图；可用截图来自雷电模拟器和项目输出目录。

## 5. 产物和校验值

正式交付目录：`E:\codex-work\outputs\generated\2026-09-18\class-schedule-v1.2.2\`

| 文件 | 说明 | SHA-256 |
| --- | --- | --- |
| `bmjclassv1.2.2.apk` | v1.2.2 debug APK，21,915,042 bytes | `9454BC8F1F51219B3B4931C535CF0229383096EF2E42157D17BF014B26EEEDDE` |
| `class-schedule-v1.2.2-debug-androidTest.apk` | instrumentation APK，1,175,865 bytes | `99E73EC21C12186732A27232AE9F2002CF4F7903CF46CEB7F775737869B7278A` |

项目根目录仍保留同版本 APK 以及历史产物：

- `E:\codex-work\outputs\generated\2026-09-17\class-schedule-android\bmjclassv1.2.2.apk`：与交付目录 APK 内容和哈希一致。
- `bmjclassv1.2.1.apk`：历史 v1.2.1，SHA-256 `7D35D20EB82891A669FA75E0C551519C0BDE9A6AB4894063AB0B1E964C74A255`。
- `bmjclassv1.1.apk`：历史 v1.1，SHA-256 `0261249B9EF34F15DE2AE4BF1E4CDAA5BF91E6761FEC519C6BD2C607E45AFE09`。
- `class-schedule-debug.apk`：早期 debug 产物，和 v1.1 内容及哈希一致。

v1.2、v1.2.1、v1.2.2 的真实截图分别位于：

- `E:\codex-work\outputs\tests\2026-09-18\class-schedule-v1.2\`
- `E:\codex-work\outputs\tests\2026-09-18\class-schedule-v1.2.1\`
- `E:\codex-work\outputs\tests\2026-09-18\class-schedule-v1.2.2\`

APK 为 debug 签名；已用 `apksigner verify --verbose` 确认 v2 签名验证通过，不是应用商店 release 签名。

## 6. 历史改动和已踩过的坑

### v1.1 -> v1.2

1. 加入今日黄历。最初考虑本地静态宜忌表，但用户发现本地没有详细宜忌；最终采用 lunar-java 离线计算，覆盖范围比硬编码四年表更合适。
2. 修复周课表只占一个格的问题。课程块现在按真实钟点计算高度和跨节；不要退回“每个 Meeting 固定一个格子”的渲染。
3. 修复设置页保存学期后回到首次建立页的问题。`SettingsCompatibility.resolveOnboardingComplete()` 会根据旧 DataStore 中的有效学期字段恢复 onboarding 状态；修改时要保留这个兼容分支。
4. 加入每门课程的自动/手动颜色，并在 Room 中保存 `colorHex`。

### v1.2 -> v1.2.1

1. 应用名改为“不美鸡课表”。
2. 设置底部加入作者“一只不太美的鸡”。
3. 增加打赏作者页，支付宝和微信图片点击后才显示。
4. 替换应用图标。

### v1.2.1 -> v1.2.2

1. 取消 NavHost 的进入、退出和返回动画。
2. 移除 Material3 `NavigationBarItem` 的点击涟漪，改为静态自定义底部导航选中状态。
3. 版本读取改为 `BuildConfig.VERSION_NAME`，并在 `app/build.gradle.kts` 开启 `buildFeatures { buildConfig = true }`。

### 失败尝试和处理结果

1. **GitHub CLI/桌面客户端不可用**：本机没有 `gh`、GitHub Desktop，也没有 `GITHUB_TOKEN`。改用 Git Credential Manager 登录后执行普通 Git 推送。
2. **GitHub 网页登录黑屏**：改用 `D:\Git\mingw64\bin\git-credential-manager.exe github login --device --no-ui` 完成设备码登录。
3. **第一次 GitHub API 创建仓库返回 400**：PowerShell 请求体把 `$false` 写成了未引用的 `false`，修正布尔值后创建公开仓库成功。
4. **一次 `git ls-remote` 超时**：`github.com:443` 网络超时不等于推送失败；之后用 GitHub API 确认了 `main`、README 和提交存在。遇到相同情况先检查远程 API/网页状态，不要重复覆盖提交。
5. **第一次使用 `BuildConfig.VERSION_NAME` 编译失败**：没有开启 BuildConfig 生成功能；已在 `app/build.gradle.kts` 加入 `buildFeatures { buildConfig = true }`。
6. **第一次静态底部导航改造导致页面内容消失**：错误使用 `.fillMaxHeight()` 让导航栏撑满父布局；已删除该 modifier，重新构建和雷电验证通过。后续改底部栏要检查内容区约束。
7. **仪器测试第一次失败**：设备保留了旧 DataStore 数据，首启断言不成立。执行 `adb shell pm clear com.example.classschedule` 后 `connectedDebugAndroidTest` 通过。不要把数据污染误判为代码回归。
8. **`testDebugUnitTest` 无法启动**：Gradle worker 报 `GradleWorkerMain ClassNotFoundException`。直接 JUnit 22/22 通过只能说明编译产物和断言正常，不能把正式 Gradle 任务标为通过；应在正常 worker 环境补跑。
9. **API 36 模拟器无法启动**：Windows Hypervisor Driver 缺失；因此 Android 16 UI 不能声称已验证。当前设备验证以雷电 Android 9/API 28 为准。
10. **早期截图计划依赖不可用设备**：不要用静态图片伪造 Compose 页面截图。现有真实截图来自雷电模拟器，缺失系统版本要明确写“未实测”。

## 7. 后续维护流程

1. `git status --short --branch`、`git remote -v`、`git log -1 --oneline`，确认没有覆盖用户未提交修改。
2. 读 `README.md`、`docs/TEST_RESULTS.md` 和本文件；先定位现有实现，再决定最小改动。
3. 如需升级版本，同步修改 `app/build.gradle.kts` 的 `versionCode`、`versionName`，并更新 README、测试报告和 APK 输出目录。
4. 如改 Room 实体，更新 schema、迁移、`RoomMigrationTest` 和旧数据兼容逻辑。
5. 如改导入，至少覆盖 CSV、XLSX、传统 XLS、字段映射、重复项、冲突和缺失节次时间；不能绕过预览/确认页。
6. 如改 UI，优先在雷电模拟器安装当前 APK 验证；检查窄屏、滚动、返回、空状态、字体放大和无动画要求。
7. 运行构建、lint、直接 JUnit；设备可用时清数据后运行 Room/Compose 仪器测试。
8. 用 `Get-FileHash -Algorithm SHA256` 和 `apksigner verify --verbose` 核对产物；不要把 `local.properties`、APK、缓存、密钥或凭据加入 Git。
9. 更新 `docs/TEST_RESULTS.md` 的真实结果和未覆盖风险，再提交并推送：

```powershell
git add MODEL_HANDOFF.md README.md docs/TEST_RESULTS.md app
git commit -m "docs: add maintenance handoff for v1.2.2"
git push origin main
```

10. 最终向用户报告：改了哪些文件、构建/测试是否通过、APK 路径和哈希、设备型号/API、仍未覆盖的风险。不要只说“已完成”。

## 8. 给下一模型的最短指令

先阅读 `MODEL_HANDOFF.md`、`README.md`、`docs/TEST_RESULTS.md`，然后检查 `git status`。保持 `com.example.classschedule`、离线架构、Room/DataStore 兼容、导入节次确认和 v1.2.2 无动画导航；不要删除历史产物。任何新功能都要做最小范围修改，并用本文件中的本机 Gradle/雷电 ADB 命令完成实际验证，明确区分“已验证”和“未实测”。
