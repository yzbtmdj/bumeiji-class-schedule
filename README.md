# 不美鸡课表 v1.2.2

中文原生 Android 课表应用，面向本地离线使用。源码使用 Kotlin、Jetpack Compose、Material 3、Room、DataStore、Navigation Compose、ViewModel 和 Coroutines/Flow；提醒使用 AlarmManager，不依赖账号、后端或网络服务。

## v1.2.2 更新

- 页面切换取消 NavHost 的进入、退出和返回过渡；底部导航改为静态选中状态并移除点击涟漪，切换不再产生拖影。
- 版本号更新为 `versionCode 4`、`versionName 1.2.2`，设置页版本自动读取构建版本。

## v1.2.1 更新

- 应用名称更新为“不美鸡课表”，设置页底部显示作者“一只不太美的鸡”。
- 设置页新增“打赏作者”页面，支付宝和微信收款码默认隐藏，点击对应项目后分别查看。
- 应用图标替换为新的手绘鸡和课表图标。

## v1.2 更新

- 今日页可在日期下方显示“今日黄历”；设置页可单独开关。黄历使用随 APK 离线打包的 lunar-java 计算每日农历、干支、生肖和详细宜忌，不依赖网络或四年静态表。
- 周课表按课程的真实起止钟点绘制高度，而不是只占一个节次格。连续的第 3-5 节会按实际时间显示为 `09:50-12:00`；同一时段的冲突课程会分栏显示。
- 每门课程拥有稳定的自动颜色，也可在课程编辑页手动选择颜色。
- 修复 v1.1 中在设置页保存有效学期后误回到首次建立流程的问题，并兼容已有本地 DataStore 数据。

## 交付内容

- `bmjclassv1.2.2.apk`：v1.2.2 debug APK，便于直接安装测试。
- `app/build/outputs/apk/debug/app-debug.apk`：同一 v1.2.2 构建产物。
- `class-schedule-v1.2.2-debug-androidTest.apk`：v1.2.2 instrumentation 测试 APK，需和 debug APK 一起由测试环境安装。
- `bmjclassv1.2.1.apk`、`bmjclassv1.1.apk`：保留的历史 APK，不会被 v1.2.2 覆盖。
- `app/src/main/res/drawable/app_icon.png`：当前应用图标。
- `app/src/main/res/drawable/donate_alipay.jpg`、`donate_wechat.jpg`：离线打赏二维码资源，仅在打赏页面点击后显示。
- `templates/class_schedule_template.csv`：可直接导入的 CSV 示例。
- `docs/TEST_RESULTS.md`：本次构建、测试和环境验证结果。
- `E:\codex-work\outputs\tests\2026-09-18\class-schedule-v1.2.2\`：本次雷电模拟器真实验证截图。

## 功能

- 首次启动建立学期，选择导入课表或手动添加；应用显示名称为“不美鸡课表”。
- 今日页按上午、下午、晚上分组，突出下一节课。
- 周课表支持横向星期和纵向节次滚动，支持 0.75x-2.0x 双指缩放和缩小、恢复、放大按钮，缩放值保存在本机。
- 课程可新增、编辑、删除，单门课程的多条上课安排可分别维护；删除课程后通过 Snackbar 撤销。
- 导入 CSV、标准 XLSX 和传统 XLS。标准字段表与教务系统格状课表分开解析；标准表预览提供可调整的字段映射，导入先解析、校验和预览，再选择合并、覆盖匹配项或取消，不会在选择文件时直接清空旧课表。
- 格状课表预览展示来源单元格、原始内容和解析结果；缺失节次会进入节次确认页，用户填写具体钟点后才允许写入。
- 必填字段：课程名、星期、开始时间、结束时间。可选字段：地点、教师、周次、颜色、备注。
- 周次支持每周、单双周、指定周集合、周次范围和日期范围。
- 设置页可修改学期、节次、阶段边界、提醒时间、无课提醒、主题和通知开关，并支持 CSV 导出。
- 设置页底部显示当前构建版本和作者名称；“打赏作者”页面分别提供支付宝、微信收款码的点击查看和关闭操作。
- 提醒使用三段 InboxStyle 通知。通知点击回到今日页并带有阶段定位状态。

## 构建

要求 JDK 17、Android SDK Platform 36、Build Tools 36.1.0。最低 Android API 26，target/compile SDK 36。

在 Android Studio 中打开本目录并等待 Gradle 同步即可。命令行构建示例：

```powershell
$env:JAVA_HOME = 'D:\codex-tools\class-schedule\jdk17'
./gradlew.bat assembleDebug
./gradlew.bat assembleDebugAndroidTest
./gradlew.bat lintDebug
```

本机 `testDebugUnitTest` 的 Gradle worker 环境会在启动测试执行器时找不到 `GradleWorkerMain`；详见测试报告。相同编译产物已通过直接 JUnit 验证。

`local.properties` 只记录本次验证机的 SDK 路径；在其他机器上请由 Android Studio 重新生成或修改该文件，不应把它当作可移植配置。

## 提醒权限

- Android 13+ 首次打开通知开关后请求 `POST_NOTIFICATIONS`。
- Android 12+ 如果没有精确闹钟访问权，应用仍使用非精确 AlarmManager，并显示“可能延迟”状态；不会承诺绝对准点。
- 默认阶段边界为上午 `< 12:00`、下午 `12:00-18:00`、晚上 `>= 18:00`；默认提醒时间为 `07:00`、`12:00`、`18:00`。
- 课程、设置、重启、时区变化、系统时间变化和应用升级会触发提醒重排。

## CSV 说明

星期可写 `周一`、`星期一`、`1` 或英文星期名。时间支持 `08:00`、`8:00`，XLSX 时间单元格也支持 Excel 的日分数形式。周次示例：`每周`、`单周`、`双周`、`1,3,5`、`1-16`、`2026-09-01 至 2026-12-31`。

传统 XLS 还兼容常见教务系统格状课表：识别“时间段/节次/星期一至星期日”表头，并从课程单元格提取课程名、周次、节次、地点和教师。首次导入按节次排列的课表时，应用必须由用户确认实际开始和结束时间，不会把示例节次直接当作学校作息。

时间冲突和重复课程默认作为警告展示，不会强制禁止导入；无效星期、时间、周次和缺少必填字段会阻止确认导入。

## 验证边界

v1.2.2 已在雷电模拟器 Android 9 / API 28 覆盖安装并完成页面切换和设置页版本显示验证；v1.2.1 的应用名称、作者信息、打赏页面、二维码、图标和首启验证继续适用，并通过 `connectedDebugAndroidTest` 的 2 个测试。v1.2 的课表、导入、黄历、提醒和 Room 验证继续适用。Android 8、Android 13 和 Android 16 仍未完成设备侧验证；Android 13 通知权限和 Android 12+ 精确闹钟授权也仍需在对应系统版本验证。
