# Preserve runtime annotations and generic signatures used by Android/Compose.
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod
-keep class com.marayti.unblock.MainActivity { *; }
-keep class com.marayti.unblock.AppSecurityGuard { *; }
-dontwarn org.tensorflow.**
-dontwarn com.googlecode.tesseract.android.**
