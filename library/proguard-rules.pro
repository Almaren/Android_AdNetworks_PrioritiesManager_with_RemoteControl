# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile
#for receiving deobfuscated crash reports to Firebase, source & line also used in mediation IronSrc Unity
-keepattributes *Annotation*
-keepattributes SourceFile,LineNumberTable
-keep public class * extends java.lang.Exception
# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

#DEBUG: For faster builds with ProGuard, exclude Crashlytics
#-keep class com.crashlytics.** { *; }
#-dontwarn com.crashlytics.**

# -------------------------- For Unity Ads as alone SDK ------------------------------
#-keep class com.unity3d.ads.** { *; }
# Keep all classes in Unity Services package
#-keep class com.unity3d.services.** { *; }
#-dontwarn com.unity3d.ads.**
#-dontwarn com.unity3d.services.**

# -------------------------- For IronSource Mediation ------------------------------
#-keepclassmembers class com.ironsource.sdk.controller.IronSourceWebView$JSInterface {
#    public *;
#}
#-keepclassmembers class * implements android.os.Parcelable {
#    public static final android.os.Parcelable$Creator *;
#}
#-keep public class com.google.android.gms.ads.** {
#   public *;
#}
#-keep class com.ironsource.adapters.** { *;
#}
#-dontwarn com.ironsource.mediationsdk.**
#-dontwarn com.ironsource.adapters.**
#-keepattributes JavascriptInterface
#-keepclassmembers class * {
#    @android.webkit.JavascriptInterface <methods>;
#}
#-dontwarn com.ironsource.adapters.unityads.**

# ----------------------- mediated AppLovin: ---------------------------------
#-keepattributes Signature,InnerClasses,Exceptions,Annotation
#-keepattributes Exceptions
#-keep public class com.applovin.sdk.AppLovinSdk{ *; }
#-keep public class com.applovin.sdk.AppLovin* { public protected *; }
#-keep public class com.applovin.nativeAds.AppLovin* { public protected *; }
#-keep public class com.applovin.adview.* { public protected *; }
#-keep public class com.applovin.mediation.* { public protected *; }
#-keep public class com.applovin.mediation.ads.* { public protected *; }
#-keep public class com.applovin.impl.*.AppLovin { public protected *; }
#-keep public class com.applovin.impl.**.*Impl { public protected *; }
#-keepclassmembers class com.applovin.sdk.AppLovinSdkSettings { private java.util.Map localSettings; }
#-keep class com.applovin.mediation.adapters.** { *; }
#-keep class com.applovin.mediation.adapter.**{ *; }

# ----------------------- mediated unity ads: ---------------------------------
# Keep filenames and line numbers for stack traces
#-keepattributes SourceFile,LineNumberTable
# Keep JavascriptInterface for WebView bridge
#-keepattributes JavascriptInterface
# Sometimes keepattributes is not enough to keep annotations
#-keep class android.webkit.JavascriptInterface {
#   *;
#}

# Keep all classes in Unity Ads package
-keep class com.unity3d.ads.** {
   *;
}
# Keep all classes in Unity Services package
-keep class com.unity3d.services.** {
   *;
}
-dontwarn com.google.ar.core.**
-dontwarn com.unity3d.services.**
-dontwarn com.ironsource.adapters.unityads.**

# -------------------------- Mediated IronSource ------------------------------
#-keepclassmembers class com.ironsource.sdk.controller.IronSourceWebView$JSInterface {
#    public *;
#}
#-keepclassmembers class * implements android.os.Parcelable {
#    public static final android.os.Parcelable$Creator *;
#}