===============================================================================
 LAN FILE SHARING - INSTALLATION & USAGE GUIDE
===============================================================================

-------------------------------------------------------------------------------
 1. PROJECT OVERVIEW
-------------------------------------------------------------------------------

"LAN File Sharing" is a native Android application that turns a phone
into a small file-sharing web server on the local network (Wi-Fi / LAN).

-------------------------------------------------------------------------------
 2. DEVELOPMENT ENVIRONMENT
-------------------------------------------------------------------------------

The project was built and tested with the following toolchain. Matching these
versions (or newer compatible ones) gives the most reproducible results.

  Operating system      : Windows / macOS / Linux (any OS that runs Android Studio)
  IDE (recommended)     : Android Studio (latest stable, "Otter"/2025.x or newer)
  JDK                   : JDK 17 (required by Android Gradle Plugin 9.x)
  Build system          : Gradle 9.2.1 (provided via the Gradle wrapper)
  Android Gradle Plugin : 9.0.1
  Language              : Java (source/target compatibility = Java 11)

  Android SDK settings:
    compileSdk          : 36
    targetSdk           : 36
    minSdk              : 24   (Android 7.0 Nougat and above)

  Required Android SDK components (install via Android Studio SDK Manager):
    - Android SDK Platform 36
    - Android SDK Build-Tools (latest)
    - Android SDK Platform-Tools (adb)
    - (Optional) An emulator system image, e.g. Android 14/15, if you do not
      use a physical device.

Third-party libraries (downloaded automatically by Gradle from Google's Maven
and Maven Central - no manual installation needed):
    - androidx.appcompat            1.7.1
    - com.google.android.material   1.13.0
    - androidx.activity             1.12.4
    - androidx.constraintlayout     2.2.1
    - org.nanohttpd:nanohttpd       2.3.1   (embedded HTTP server)
    - com.journeyapps:zxing-android-embedded 4.3.0  (QR code scan/generate)
    - junit 4.13.2, androidx.test ext-junit 1.3.0, espresso-core 3.7.0 (tests)

-------------------------------------------------------------------------------
 3. INSTALLATION / BUILD STEPS
-------------------------------------------------------------------------------

You can build the app either with Android Studio or from the command
line using the Gradle wrapper.

------------------------------------------
 OPTION A - Build with Android Studio (GUI)
------------------------------------------
  1. Open Android Studio.
  2. Choose "Open" and select the project root folder (the folder containing
     "settings.gradle").
  3. Wait for Gradle to sync. Android Studio will automatically download the
     correct Gradle distribution and all dependencies (an internet connection
     is required on the first build).
  4. If prompted, install any missing SDK components (Platform 36, Build-Tools).
  5. When the sync finishes with no errors, the project is ready to run.

------------------------------------------
 OPTION B - Build from the command line
------------------------------------------
  Open a terminal in the project root folder.

  On Windows (PowerShell / CMD):
      .\gradlew.bat assembleDebug

  On macOS / Linux:
      ./gradlew assembleDebug

  The first run downloads Gradle 9.2.1 and all dependencies automatically.

  Output APK location:
      app\build\outputs\apk\debug\app-debug.apk

  To build a release (unsigned) APK:
      .\gradlew.bat assembleRelease        (Windows)
      ./gradlew assembleRelease            (macOS / Linux)

  To clean the build:
      .\gradlew.bat clean                  (Windows)
      ./gradlew clean                      (macOS / Linux)

-------------------------------------------------------------------------------
 4. RUNNING THE PROGRAM
-------------------------------------------------------------------------------

------------------------------------------
 4.1 Run on an emulator
------------------------------------------
  1. Create an Android Virtual Device (AVD) in Android Studio (Device Manager).
  2. Start the emulator and press "Run".
  Note: file sharing between devices works best on real devices connected to
  the same Wi-Fi network; an emulator is fine for testing the UI.

------------------------------------------
 4.2 Using the app
------------------------------------------
  Note: Put the host phone and all client devices on the SAME Wi-Fi / LAN.

  As the HOST (the phone running the server):
    1. Open the app. The home screen offers "Start Server" (public/private)
       and "Join Server" options.
    2. Choose a mode:
         - Public : no password needed.
         - Private: enter a password. Clients must enter this password (or scan
           the one-time QR code) to connect.
    3. Tap start. The app picks a free port (starting at 8080) and shows the
       server address
    4. Tap the QR option to display a QR code that clients can scan to connect
       instantly.

  As a CLIENT (another phone/computer on the same network):
    - Open any web browser and go to the address shown by the host
      OR
    - Open the app and use "Join Server" -> enter the IP or scan the host's QR
      code.
    - For a PRIVATE server, enter the password when prompted.

  What you can do on the web page:
    - Upload files to the host (/upload)
    - Browse and download shared files (/files, /download/<name>)
    - The host (localhost/owner) can delete files (/delete)

-------------------------------------------------------------------------------
 5. PROJECT STRUCTURE
-------------------------------------------------------------------------------

  Server2/
  |- settings.gradle              Project + module declaration
  |- build.gradle                 Top-level Gradle config
  |- gradlew / gradlew.bat        Gradle wrapper (build without a local Gradle)
  |- gradle/
  |   |- wrapper/                 Pinned Gradle 9.2.1 distribution
  |   |- libs.versions.toml       Central version catalog (dependencies)
  |- app/
      |- build.gradle             App module config (SDK, deps)
      |- proguard-rules.pro       Release shrink/obfuscation rules
      |- src/main/
          |- AndroidManifest.xml  Permissions, activities
          |- java/com/test/server/
          |   |- MainActivity.java
          |   |- EmbeddedServer.java
          |   |- NetworkUtils.java
          |- assets/              Web UI (index/home/files/upload/login/server + css/js)
          |- res/                 Android resources (layout, themes, icons, strings)