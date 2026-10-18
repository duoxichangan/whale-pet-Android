# 泡泡鲸 · 口袋里的海

一个完全离线的原生 Android 桌宠。把一小片海装进口袋，让「泡泡」在手机桌面陪你，也可以给她取一个自己的名字。

**当前版本：2.3.0 · Android 9 / API 28 及以上**

[下载最新安装包](https://github.com/duoxichangan/whale-pet-Android/releases/latest) · [更新说明](CHANGELOG.md) · [验证记录](TEST-REPORT.md)

<p>
  <img src="docs/screenshots/home-dark.png" width="30%" alt="深海夜色的小屋">
  <img src="docs/screenshots/interaction-light.png" width="30%" alt="互动与自发动作">
  <img src="docs/screenshots/journal-light.png" width="30%" alt="陪伴手账">
</p>

## 可以做什么

- 106 段透明角色动画，轻触回应、拖动定位、长按打开桌面菜单。
- 日常与休息两种模式，食物、玩乐、自发小动作各有清晰入口。
- 饱食、心情、精力三项状态；互动先确认结算，再播放动画。
- 陪伴手账记录最近 100 条互动，可直接回看动画，不重复消耗或结算。
- 蓝色日间与深海夜色主题，可调整人物大小和自动走动。
- 原生悬浮窗、关屏暂停绘制、解锁恢复；常驻通知仅静默显示状态。
- 不联网、不需要账号，存档保存在手机本地。

## 安装与使用

1. 在 [GitHub Releases](https://github.com/duoxichangan/whale-pet-Android/releases/latest) 下载最新 APK 并安装。
2. 打开小屋，点击「让泡泡来陪你」，允许悬浮窗和陪伴通知。
3. 回到手机桌面：轻触互动、拖动换位置、长按打开菜单。
4. 在设置中修改名字、大小、走动与主题。

小米 / Redmi 等设备可能还需要允许后台弹出界面、自启动，并放宽后台运行限制。用户强行停止应用或重启手机后，需要重新打开小屋。通知里的「回来陪我」可尝试恢复显示，「待会儿见」停止陪伴。

## 养成规则

饱食每 15 分钟下降 1，满 100 时不能继续吃；心情自然每小时下降 1.5。跑动每 10 分钟最多消耗 1 精力，休息每小时恢复 30。日常精力充足且允许走动时，自发动作有 60% 概率选择跑步，其余符合模式的动作等概率抽取；休息使用独立动作池。

食物和玩乐的效果沿用原配置。动作分类与触发范围见 [动作说明](docs/ACTION-CLASSIFICATION.md)。

## 从源码构建

需要 Java 17、Android SDK 36；仓库自带 Gradle Wrapper。Android Studio 可直接打开本目录。

```powershell
# Windows：自动寻找本机 Android Studio JDK 和 SDK
./build.ps1 -DebugApk

# 或直接使用 Gradle（macOS / Linux 使用 ./gradlew）
./gradlew.bat :app:assembleDebug :app:assembleDebugAndroidTest :app:lintDebug

# 校验原始角色素材、动画注册和动作分类，需要 Node.js
node tools/check-project.cjs
```

调试包输出到 `app/build/outputs/apk/debug/`。正式签名密钥不入库；发布版更新由维护者使用同一私有密钥签署。自行构建请用调试版，或创建自己的发布签名，不能覆盖官方签名版。

## 项目结构

```text
app/src/main/java/com/whale/pet/  原生界面、动画、悬浮窗和养成逻辑
app/src/main/assets/pet/         角色原素材、动作配置和原始许可
app/src/main/assets/native-*.json 动作边界、人物注册与触发分类
app/src/androidTest/             32 项原生回归测试
tests/                          素材校验基线、测试说明和验证结果
tools/                          素材校验、原生 UI 测试和维护工具
docs/                           动作说明与当前界面截图
release/                        本机最新 APK 的校验值和发布说明
```

APK 仅作为 Release 附件发布，不进入 Git 历史。运行基于 Java View、ImageDecoder、Choreographer 与前台服务，不依赖 WebView。测试入口仅存在于 debug 构建，正式包不包含。

## 许可

项目代码采用 [MIT License](LICENSE)。第三方素材和参考实现保留各自许可，详见 [素材与开源致谢](THIRD-PARTY-NOTICES.md)。

## 参考项目与致谢

- **[PC2005-cloud/dsh-pet](https://github.com/PC2005-cloud/dsh-pet)**：本项目角色数字资产的来源。沿用 106 段角色动画，并转换为保留透明度的 animated WebP；原始 MIT 许可随源码与安装包保留。
- **[LIN428924379/baifanyu](https://github.com/LIN428924379/baifanyu)**：原生 Android 重写的参考项目。参考其桌宠悬浮窗、原生绘制以及关屏 / 解锁生命周期处理思路，保留本项目自己的数字资产与养成逻辑；原 MIT 许可随项目保留。
