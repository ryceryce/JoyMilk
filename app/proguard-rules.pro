# Add project specific ProGuard rules here.
# Keep Firebase model classes
-keep class com.joymilk.app.data.model.** { *; }
# Keep Hilt generated code
-keepnames @dagger.hilt.android.lifecycle.HiltViewModel class * extends androidx.lifecycle.ViewModel

