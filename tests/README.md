# 验证

## 静态素材检查

`node tools/check-project.cjs` 校验 219 个原角色文件、106 段人物注册与动作分类。原始校验基线在 `fixtures/asset-sha256.json`。

## 原生回归

```powershell
./gradlew.bat :app:assembleDebug :app:assembleDebugAndroidTest :app:lintDebug
adb -s emulator-5554 install app/build/outputs/apk/debug/app-debug.apk
adb -s emulator-5554 install app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb -s emulator-5554 shell am instrument -w com.whale.pet.test/com.whale.pet.NativeTests
```

32 项覆盖状态结算、原动画解码、人物注册、随机权重、动作方向、气泡冷却、手账聚合、并发去重和存档迁移。

## 实际界面与生命周期

`python tools/check-unified-android.py` 使用专用 `emulator-5554`。可通过 `WHALE_ADB` 指定 adb 路径、`WHALE_DEVICE` 指定其他专用模拟器。脚本会清除该模拟器中的泡泡鲸数据；拒绝实体设备序列号。

覆盖横向筛选、确认页布局、三类动作只读回看、深浅色、桌面菜单、静默通知属性及关屏恢复。共享 ADB 辅助函数在 `tools/android_qa.py`，输出在被 Git 忽略的 `tests/output/`。

`DebugReceiver` 仅用于 debug 构建，并要求系统 DUMP 权限。正式 APK 不包含它。最近一次验证结果在 `tests/results/`，摘要见 [TEST-REPORT.md](../TEST-REPORT.md)。
