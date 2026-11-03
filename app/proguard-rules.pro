# Room / Hilt generated code is already kept by their own consumer rules.

# Apache POI reflects heavily and references desktop-only classes.
-dontwarn org.apache.poi.**
-dontwarn org.apache.xmlbeans.**
-dontwarn javax.xml.stream.**
-dontwarn java.awt.**
-keep class org.apache.poi.** { *; }
-keep class org.openxmlformats.** { *; }
-keep class schemaorg_apache_xmlbeans.** { *; }

-dontwarn com.tom_roush.pdfbox.**
-keep class com.tom_roush.pdfbox.** { *; }

# kotlinx.serialization
-keepattributes *Annotation*, InnerClasses
-keepclassmembers class com.dlbc.sermonstudio.** {
    *** Companion;
}
-keepclasseswithmembers class com.dlbc.sermonstudio.** {
    kotlinx.serialization.KSerializer serializer(...);
}
