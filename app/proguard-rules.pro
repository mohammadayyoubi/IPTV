# R8 rules for release builds (isMinifyEnabled = true).

# Keep line numbers so crash reports (CrashReporter) stay readable
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Media3 creates the FFmpeg audio renderer by reflection (DefaultRenderersFactory)
-keep class androidx.media3.decoder.ffmpeg.FfmpegAudioRenderer { <init>(...); }
-keep class androidx.media3.decoder.ffmpeg.FfmpegLibrary { *; }
-keepclasseswithmembernames class androidx.media3.decoder.ffmpeg.** { native <methods>; }

# Glide's generated API
-keep public class * implements com.bumptech.glide.module.GlideModule
-keep class * extends com.bumptech.glide.module.AppGlideModule { <init>(...); }
-keep public enum com.bumptech.glide.load.ImageHeaderParser$** { **[] $VALUES; public *; }

# Channel / ChannelServer are passed between screens as Serializable
-keepclassmembers class com.example.iptv.OOP.** implements java.io.Serializable {
    private static final long serialVersionUID;
    <fields>;
}
