# ---------------------------------------------------------------------------
# 1. 核心压缩与优化设置 (核心性能提升)
# ---------------------------------------------------------------------------
# 迭代优化次数 (R8 默认会忽略这个，但 ProGuard 会用到)
-optimizationpasses 5
# 混淆时不使用大小写混合类名 (Windows 兼容性)
-dontusemixedcaseclassnames
# 不跳过非公共库类 (确保全局优化)
-dontskipnonpubliclibraryclasses
# 记录详细日志
-verbose

# ---------------------------------------------------------------------------
# 2. 开源调试友好设置 (保留行号与源文件信息)
# ---------------------------------------------------------------------------
# 即使混淆了类名，也保留原始代码的行号。这样崩溃日志（Crash Log）依然能定位到具体行。
-keepattributes SourceFile,LineNumberTable
# 保留所有的注解 (很多框架如 Room, Retrofit 依赖注解)
-keepattributes *Annotation*
# 保留泛型信息 (防止某些 JSON 解析库失效)
-keepattributes Signature
# 保留抛出的异常信息
-keepattributes Exceptions

# ---------------------------------------------------------------------------
# 3. Android 基础组件保护
# ---------------------------------------------------------------------------
-keep public class * extends android.app.Activity
-keep public class * extends android.app.Application
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.content.ContentProvider
-keep public class * extends android.app.backup.BackupAgentHelper
-keep public class * extends android.preference.Preference

# 保持 View 的构造函数，确保 XML 布局能正确加载自定义 View
-keep public class * extends android.view.View {
    public <init>(android.content.Context);
    public <init>(android.content.Context, android.util.AttributeSet);
    public <init>(android.content.Context, android.util.AttributeSet, int);
}

# ---------------------------------------------------------------------------
# 4. 你的项目特有规则 (根据你的包名 com.lff.classschedule)
# ---------------------------------------------------------------------------
# 保持你的数据模型类 (非常重要：防止数据库/JSON字段名被改乱)
# 假设你的实体类都在 .data.model 包下
-keep class com.lff.classschedule.data.model.** { *; }

# 如果你使用了特定的自定义类（比如通过反射调用的）
# -keep class com.lff.classschedule.ui.custom.** { *; }

# ---------------------------------------------------------------------------
# 5. Kotlin 适配规则
# ---------------------------------------------------------------------------
-keep class kotlin.Metadata { *; }
-keepclassmembers class * {
    @kotlin.jvm.JvmField <fields>;
}

# ---------------------------------------------------------------------------
# 6. 移除日志输出 (生产环境提速)
# ---------------------------------------------------------------------------
# 移除所有的 Log.d, Log.v 代码，这样发布版 APK 更干净且安全
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
}

# ---------------------------------------------------------------------------
# 7. 第三方库通用保持 (如果后续添加了常用库)
# ---------------------------------------------------------------------------
# 保持 R 文件中的资源 ID
-keepclassmembers class **.R$* {
    public static <fields>;
}