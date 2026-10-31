@file:Suppress("UnstableApiUsage")

plugins {
    alias(libs.plugins.agp.library) apply false
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.kotlin.serialization) apply false
}

extra["androidMinSdkVersion"] = 26
extra["androidTargetSdkVersion"] = 37
extra["androidCompileSdkVersion"] = 37
extra["androidCompileSdkVersionMinor"] = 0
extra["androidBuildToolsVersion"] = "37.0.0"
extra["androidSourceCompatibility"] = JavaVersion.VERSION_21
extra["androidTargetCompatibility"] = JavaVersion.VERSION_21
