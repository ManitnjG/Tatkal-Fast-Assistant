plugins {
 id("com.android.application")
 kotlin("android")
 kotlin("plugin.serialization")
 id("org.jetbrains.kotlin.plugin.compose")
 id("com.google.devtools.ksp")
 id("com.google.dagger.hilt.android")
}
android {
 namespace = "in.tatkalfast.assistant"
 compileSdk = 35
 defaultConfig {
  applicationId = "in.tatkalfast.assistant"
  minSdk = 26
  targetSdk = 35
  versionCode = providers.environmentVariable("VERSION_CODE").orElse("3").get().toInt()
  versionName = providers.environmentVariable("VERSION_NAME").orElse("0.3.0").get()
  manifestPlaceholders["appLabel"] = "@string/app_name"
  testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
 }
 signingConfigs {
  if (System.getenv("DEBUG_STORE_FILE") != null) getByName("debug") {
   storeFile = file(System.getenv("DEBUG_STORE_FILE"))
   storePassword = System.getenv("DEBUG_STORE_PASSWORD")
   keyAlias = System.getenv("DEBUG_KEY_ALIAS")
   keyPassword = System.getenv("DEBUG_KEY_PASSWORD")
  }
  if (System.getenv("SIGNING_STORE_FILE") != null) create("release") {
   storeFile = file(System.getenv("SIGNING_STORE_FILE"))
   storePassword = System.getenv("SIGNING_STORE_PASSWORD")
   keyAlias = System.getenv("SIGNING_KEY_ALIAS")
   keyPassword = System.getenv("SIGNING_KEY_PASSWORD")
  }
 }
 buildTypes {
  debug { applicationIdSuffix = ".preview"; versionNameSuffix = "-preview"; manifestPlaceholders["appLabel"] = "Tatkal Fast Assistant Preview" }
  release {
   isMinifyEnabled = true
   isShrinkResources = true
   signingConfig = signingConfigs.findByName("release")
   proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
  }
 }
 buildFeatures { compose = true }
 compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
 kotlinOptions { jvmTarget = "17" }
 packaging { resources.excludes += "/META-INF/{AL2.0,LGPL2.1}" }
 lint { abortOnError = true }
}
ksp { arg("room.schemaLocation", "$projectDir/schemas") }
dependencies {
 implementation(project(":domain"))
 implementation(platform("androidx.compose:compose-bom:2025.04.01"))
 implementation("androidx.activity:activity-compose:1.10.1")
 implementation("androidx.compose.material3:material3")
 implementation("androidx.compose.material:material-icons-extended")
 implementation("androidx.compose.ui:ui-tooling-preview")
 debugImplementation("androidx.compose.ui:ui-tooling")
 implementation("androidx.lifecycle:lifecycle-runtime-compose:2.9.0")
 implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.9.0")
 implementation("androidx.fragment:fragment-ktx:1.8.6")
 implementation("androidx.appcompat:appcompat:1.7.0")
 implementation("androidx.biometric:biometric:1.1.0")
 implementation("androidx.browser:browser:1.8.0")
 implementation("androidx.core:core-ktx:1.16.0")
 implementation("androidx.datastore:datastore-preferences:1.1.4")
 implementation("androidx.room:room-runtime:2.7.1")
 implementation("androidx.room:room-ktx:2.7.1")
 ksp("androidx.room:room-compiler:2.7.1")
 implementation("com.google.dagger:hilt-android:2.55")
 ksp("com.google.dagger:hilt-compiler:2.55")
 implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
 implementation("androidx.work:work-runtime-ktx:2.10.1")
 testImplementation("junit:junit:4.13.2")
 androidTestImplementation(platform("androidx.compose:compose-bom:2025.04.01"))
 androidTestImplementation("androidx.compose.ui:ui-test-junit4")
 debugImplementation("androidx.compose.ui:ui-test-manifest")
 androidTestImplementation("androidx.test.ext:junit:1.2.1")
 androidTestImplementation("androidx.test:runner:1.6.2")
 androidTestImplementation("androidx.room:room-testing:2.7.1")
}
