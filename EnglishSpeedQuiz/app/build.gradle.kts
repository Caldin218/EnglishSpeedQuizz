plugins {
    alias(libs.plugins.android.application)
    // [QUAN TRỌNG 1] Thêm plugin này để đọc file google-services.json
    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.englishspeedquiz"
    compileSdk = 34 // (Khuyên dùng 34 ổn định hơn 36 preview, nhưng nếu bạn muốn giữ 36 thì để nguyên)

    defaultConfig {
        applicationId = "com.example.englishspeedquiz"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)

    // [QUAN TRỌNG 2] Thêm Firebase BOM (Quản lý phiên bản tự động)
    // Phiên bản này giúp các thư viện con không bị đá nhau
    implementation(platform("com.google.firebase:firebase-bom:33.7.0"))

    // [QUAN TRỌNG 3] Thêm Auth và Firestore (Không cần ghi số version nữa)
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-firestore")

    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

    // [SỬA LỖI] Thêm dòng này để ép dùng phiên bản ổn định (thay vì bản 1.11.0 bị lỗi)
    implementation("androidx.activity:activity:1.9.3")
}