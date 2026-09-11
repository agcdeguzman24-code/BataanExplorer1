plugins {
    alias(libs.plugins.android.application) apply false

    id("com.google.gms.google-services") version "4.5.0" apply false

    // Added Kotlin Serialization plugin definition for Supabase
    kotlin("plugin.serialization") version "1.9.22" apply false
}