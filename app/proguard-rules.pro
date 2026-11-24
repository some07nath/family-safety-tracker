# Proguard rules for Family Safety Tracker

# Keep Room generated classes
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

# Keep Google Play Services and Maps
-keep class com.google.android.gms.maps.** { *; }
-keep interface com.google.android.gms.maps.** { *; }
-keep class com.google.android.gms.location.** { *; }
-keep interface com.google.android.gms.location.** { *; }

# Keep data models
-keepclassmembers class com.familysafety.tracker.data.model.** { *; }
-keepclassmembers class com.familysafety.tracker.data.local.** { *; }
