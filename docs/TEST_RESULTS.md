# 测试与交付结果

执行日期：2026-09-18

## 构建环境

- JDK：`D:\codex-tools\class-schedule\jdk17`
- Gradle：8.13，本机使用 `D:\codex-tools\class-schedule\gradle-8.13\bin\gradle.bat`
- Android SDK：Platform 36、Build Tools 36.1.0、Platform Tools 37.0.1
- 应用包：`com.example.classschedule`
- APK 元数据：`versionCode 4`、`versionName 1.2.2`、`minSdk 26`、`targetSdk 36`、`compileSdk 36`
- 设备：雷电模拟器 `emulator-5554`，Android 9 / API 28，分辨率 `1920x1080`

## 自动化结果

| 命令或测试 | 结果 | 证据 |
| --- | --- | --- |
| `assembleDebug` | 通过 | v1.2.2 APK 已生成。 |
| `assembleDebugAndroidTest` | 通过 | v1.2.2 instrumentation APK 已生成。 |
| `lintDebug` | 通过，0 errors、8 warnings | 警告为 Gradle/Kotlin 版本提示、图标资源和已有 Compose 图标弃用提示。 |
| 直接 JUnit | 通过，22/22 | 使用当前已编译 class，覆盖领域、XLS、黄历、设置兼容和跨节布局。 |
| `testDebugUnitTest` | 测试执行器未启动 | 本机 Gradle worker 报 `ClassNotFoundException: worker.org.gradle.process.internal.worker.GradleWorkerMain`；不是断言失败。 |
| `RoomMigrationTest` | 通过，1/1 | 在连接的 Android 9 设备执行 `1 -> 2` Room 迁移。 |
| `connectedDebugAndroidTest` | 通过，2/2 | 雷电模拟器日志：2 tests、0 failed；包含 Room 迁移和首启“不美鸡课表”检查。 |

直接 JUnit 的 22 项断言覆盖周次计算、每周/单双周/指定集合/范围/日期范围、阶段归类、通知文本、CSV 必填和时间校验、冲突警告、标准表字段映射、XLSX 共享字符串、传统 XLS 格状课表、地点/教师/来源单元格、节次时间更新、学期保存兼容、课程颜色、跨节布局和黄历宜忌。

## 真实附件与界面验证

- 导入用户附件 `网安24课表(20260904155930920).xls`：识别为“教务系统格状课表”，预览为 9 条有效记录，最终为 6 门课程。
- 根据用户提供的河池学院作息图确认节次：第 1-11 节依次为 `08:00-08:40`、`08:45-09:25`、`09:50-10:30`、`10:35-11:15`、`11:20-12:00`、`14:40-15:20`、`15:25-16:05`、`16:30-17:10`、`17:15-17:55`、`19:40-20:20`、`20:25-21:05`。
- 周一“网络空间安全法律法规与伦理”的第 3-5 节实际显示为 `09:50-12:00`，课程块跨 3 个节次格，不再只占一个格。
- 今日黄历开关和重启后的保存状态已实测；开启后显示农历、干支、生肖及详细“宜/忌”。lunar-java 的宜忌计算在 1900、2022、2026、2030、2099、2100、2101 年锚点均返回数据，因此未采用仅四年的本地静态表。
- 在课程编辑页手动选色并保存后，周课表颜色已更新；缩放按钮已实测 `75%`、`100%`、`200%`。
- 设置页连续保存相同学期后仍停留在设置页并提示“学期设置已更新”。

## v1.2.1 增量验证

- 首启页显示应用名称“不美鸡课表”。
- 设置页底部显示 `不美鸡课表 1.2.1` 和 `作者：一只不太美的鸡`。
- 设置页新增“打赏作者”入口；进入页面后支付宝和微信支付两项均默认只显示文字，不直接展示二维码。
- 点击“支付宝”后显示支付宝收款图，节点含“支付宝收款二维码”和可访问的关闭按钮；点击关闭后返回打赏页面。
- 点击“微信支付”后显示微信收款图，节点含“微信支付收款二维码”和可访问的关闭按钮。
- `aapt2 dump badging` 确认应用标签为“不美鸡课表”，图标资源为 `res/drawable/app_icon.png`。
- v1.2.1 APK 已在雷电模拟器覆盖安装并启动，未引入账号、网络或新的运行时服务。

## v1.2.2 增量验证

- `aapt2 dump badging` 确认 `versionCode 4`、`versionName 1.2.2`，应用标签仍为“不美鸡课表”。
- NavHost 的进入、退出、返回过渡均为 `EnterTransition.None`/`ExitTransition.None`；底部导航使用静态选中状态、无点击涟漪。
- v1.2.2 APK 已覆盖安装到雷电模拟器 Android 9 / API 28，首屏和今日/课表/课程/设置切换可正常显示，设置页底部显示 `不美鸡课表 1.2.2`；修复静态底部导航首次实现造成的内容区被撑满问题后重新验证通过。
- 本次改动未引入账号、网络或新的运行时服务；旧版本本地数据沿用原有兼容逻辑。

原 v1.2 真实截图已整理到 `E:\codex-work\outputs\tests\2026-09-18\class-schedule-v1.2\`：

- `01-first-launch.png`
- `02-xls-preview.png`
- `03-week-real-time-span.png`
- `04-today-almanac.png`
- `05-course-colors.png`
- `06-semester-save.png`
- `07-week-zoom-75.png`
- `08-week-zoom-200.png`

v1.2.1 增量截图已整理到 `E:\codex-work\outputs\tests\2026-09-18\class-schedule-v1.2.1\`：

- `01-first-launch.png`
- `02-settings-bottom.png`
- `03-donation-page.png`
- `04-donation-alipay.png`
- `05-donation-wechat.png`

v1.2.2 增量截图已整理到 `E:\codex-work\outputs\tests\2026-09-18\class-schedule-v1.2.2\`：

- `07-main-shell-fixed-after-wait.png`
- `09-week.png`
- `10-courses.png`
- `11-settings.png`
- `12-settings-bottom.png`
- `13-donation.png`
- `14-final-install.png`

## APK 校验

- `bmjclassv1.2.2.apk`：21,915,042 bytes
- SHA-256：`9454BC8F1F51219B3B4931C535CF0229383096EF2E42157D17BF014B26EEEDDE`
- `aapt2 dump badging`：`com.example.classschedule`、`versionCode 4`、`versionName 1.2.2`、标签“不美鸡课表”、图标 `res/drawable/app_icon.png`
- `apksigner verify --verbose`：APK Signature Scheme v2 verified，debug 签名。
- `class-schedule-v1.2.2-debug-androidTest.apk`：1,175,865 bytes，SHA-256 `99E73EC21C12186732A27232AE9F2002CF4F7903CF46CEB7F775737869B7278A`，与 v1.2.2 debug APK 配套。

- `bmjclassv1.2.1.apk`：21,642,441 bytes
- SHA-256：`7D35D20EB82891A669FA75E0C551519C0BDE9A6AB4894063AB0B1E964C74A255`
- `aapt2 dump badging`：`com.example.classschedule`、`versionCode 3`、`versionName 1.2.1`、标签“不美鸡课表”、图标 `res/drawable/app_icon.png`
- `apksigner verify --verbose`：APK Signature Scheme v2 verified，debug 签名。
- `class-schedule-v1.2.1-debug-androidTest.apk`：1,175,622 bytes，SHA-256 `4FE11D190F2F5DAE0DA5023E7A8721FB5A3ADD8838CCA6AFF5C129AA10574FCC`，与 v1.2.1 debug APK 配套。
- 保留的 `bmjclassv1.1.apk` 未被覆盖；其历史 SHA-256 为 `0261249B9EF34F15DE2AE4BF1E4CDAA5BF91E6761FEC519C6BD2C607E45AFE09`。

## 界面审查

- AI 模板感：2/10。采用学业日程本的时间轴和节次网格，使用低饱和青绿与中性灰白，不使用渐变、营销 Hero 或紫色 AI 风格。
- 产品辨识度：8/10。今日三阶段、真实时间纵轴、课程色条、宜忌信息和设置控制构成明确的课表工具流程。
- 可访问性基础项：中文表单均有可见标签，课程颜色选择有 content description；图标命令提供辅助说明或常用语义。

## 剩余风险

1. 当前真实设备是 Android 9；Android 8、Android 13 和 Android 16 的 UI 及兼容性尚未实测。
2. Android 13 通知权限、Android 12+ 精确闹钟授权、实际通知触发和厂商后台限制尚未在相应设备验证。
3. Gradle `testDebugUnitTest` 仍受本机 worker 类路径环境影响；22 个断言已通过直接 JUnit 覆盖，但应在另一套正常 Gradle 环境再运行该任务。
