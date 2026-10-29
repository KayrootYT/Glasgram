# TDLib uses JNI/reflection in parts of its Java binding.
-keep class org.drinkless.tdlib.** { *; }
-keepclasseswithmembernames class * {
    native <methods>;
}
