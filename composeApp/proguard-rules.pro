# --- KENDI KODLARIMIZ ---
# Model siniflarini (JSON/DataStore vs. icin) koru
-keep class com.aliyasirnac.overridealarm.model.** { *; }
# Alarm mekanizmasi siniflarini (Receiver, Service) sistem bulabilsin diye koru
-keep class com.aliyasirnac.overridealarm.alarm.** { *; }

# --- KOTLIN SERIALIZATION ---
-dontwarn kotlinx.serialization.**
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Serialize edilen siniflarin bilesenlerini koru
-keep,includedescriptorclasses class com.aliyasirnac.overridealarm.**$$serializer { *; }
-keepclassmembers class com.aliyasirnac.overridealarm.** {
    *** Companion;
}
-keepclasseswithmembers class com.aliyasirnac.overridealarm.** {
    kotlinx.serialization.KSerializer serializer(...);
}