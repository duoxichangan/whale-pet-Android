# 泡泡鲸 2.3.0 验证记录

日期：2026-10-18。Java 17 / Android SDK 36；专用 Android 16 / API 36.1 模拟器。未连接 Redmi K90 Pro Max / Android 17 真机。

## 发布整理

清理未使用的动作调度成员、提醒时间戳、窗口辅助方法、矢量图标、旧版 UI 测试脚本以及不再展示的插画和重复图标；最新 UI 测试提取共用 ADB 辅助函数。保留角色原素材、当前原生测试与全部第三方许可。APK 不入 Git，仅通过 GitHub Release 分发。

干净构建 assembleDebug、assembleDebugAndroidTest、assembleRelease、lintDebug 成功。Lint 0 错误、19 警告。

219 个原角色素材与配置的 SHA256 一致，106 段人物注册和动作归类检查通过；Git 暂存区同样逐文件校验，防止换行转换破坏原素材。正式 APK 不含 DebugReceiver，也不再打包未使用的 branding / design 资产。

## 原生回归

NativeTests 共 32 项，覆盖全部 106 段动画解码、人物注册、状态结算、原有养成规则、并发去重、存档迁移、随机权重、方向、气泡冷却及手账聚合。最近结果见 [tests/results/instrumentation.txt](tests/results/instrumentation.txt)。

## 2.3.0 界面验证

[验证断言结果](tests/results/unified-verification.json)。本次代码清理未改动下列 UI 与交互逻辑：

- App / 桌面自发动作与筛选同排；深浅色工具栏及手账统一。
- 今日陪伴标题与三项统计居中，实际收益左对齐。
- 饱食 100、精力 0 时，食物、玩乐、自发动作均可直接回看；不确认、不跳转，关闭后存档原始 JSON 不变。
- App 确认页隐藏顶部两行，返回置顶、内容居中；桌面确认页隐藏头尾操作，返回列表后恢复。
- 活动通知列表仅有一条 id=7 常驻状态，渠道 sound=null、vibration=false、showBadge=false，通知包含 ONLY_ALERT_ONCE 与 FOREGROUND_SERVICE。
- 一轮关屏 / 解锁后保留原窗口与服务会话，关屏停止绘制，解锁继续。
- 2.2.0 到 2.3.0 同签名覆盖升级成功，保留休息状态。

当前展示截图在 [docs/screenshots](docs/screenshots)。系统或厂商后台限制下的真机表现需实际安装确认。

## 正式包

`release/泡泡鲸-2.3.0.apk`，191440043 字节；com.whale.pet / versionCode 11。

APK SHA256：`7b8cf32ee743a95d17ca14e2effb8e4d5bd5c98b2e901dd32891335541f5ed69`。

签名证书 SHA256：`ec838c38017e10983c9f0babdb2c7ecb791ecb014e9cfb01149bb6b1449cfdd5`，与旧版一致。密钥与密码仅保留在维护者本机，不入库。
