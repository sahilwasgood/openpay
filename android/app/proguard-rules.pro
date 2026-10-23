# kotlinx.serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keep,includedescriptorclasses class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class in.openpay.app.data.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# zxing embedded
-keep class com.journeyapps.barcodescanner.** { *; }
-keep class com.google.zxing.** { *; }

# tink references compile-only annotations
-dontwarn com.google.errorprone.annotations.**
-dontwarn javax.annotation.**

# ed25519
-keep class net.i2p.crypto.eddsa.** { *; }

# eddsa references an optional JDK internal on verify paths never taken on android
-dontwarn sun.security.x509.**
