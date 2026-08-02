# kotlinx.serialization generates a companion `$$serializer` for every @Serializable class and
# looks it up reflectively. R8 understands this, but the generated members still have to survive.
-keepclassmembers class com.ftc.stopwatch.** {
    *** Companion;
}
-keepclasseswithmembers class com.ftc.stopwatch.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.ftc.stopwatch.**$$serializer { *; }

# Keep the line numbers needed to read a release stack trace, without leaking source file names.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
