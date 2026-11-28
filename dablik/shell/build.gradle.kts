plugins { id("com.android.application") }

android {
    namespace = "iq.dablik.shell"
    compileSdk = 34
    defaultConfig {
        // قيم مؤقتة يستبدلها تطبيق دبليك عند إنشاء كل APK
        applicationId = "wbl.placeholder.appid"
        minSdk = 21
        targetSdk = 29
        versionCode = 77777777
        versionName = "WBLVERNAMEPLACEHOLDER"
    }
    buildTypes {
        release {
            isMinifyEnabled = false
            isCrunchPngs = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    lint { abortOnError = false; checkReleaseBuilds = false }
}
