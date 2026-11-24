# Proguard rules for Family Safety Tracker

# Keep Room generated classes
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

# Keep Google Play Services Location and OSMDroid
-keep class com.google.android.gms.location.** { *; }
-keep interface com.google.android.gms.location.** { *; }
-keep class org.osmdroid.** { *; }
-dontwarn org.osmdroid.**

# Keep data models
-keepclassmembers class com.familysafety.tracker.data.model.** { *; }
-keepclassmembers class com.familysafety.tracker.data.local.** { *; }
