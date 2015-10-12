## Azizi ODK Puller [![Build Status](https://travis-ci.org/ilri/azizi-odk-puller.svg?branch=master)](https://travis-ci.org/ilri/azizi-odk-puller)

This application can be used to periodically fetch external data to be used by [ODK Collect](https://opendatakit.org/use/collect/) and possibly any other ODK application running on Android.
Refer to the [ODK External Itemsets page](https://opendatakit.org/help/form-design/external-itemsets/) and [ODK Data Preloading page](https://opendatakit.org/help/form-design/data-preloading/) for more information.

Note that this application has been custom made by and for use by the ILRI Azizi Biorepository team and has been tested for this purpose only.
Server side code used by this application is under [another GitHub repository](https://github.com/ilri/azizi-biorepository/).

### Building the project

To buid this [Gradle](https://gradle.org/) project, run the following commands:

    ./gradlew clean
    ./gradlew build --debug

### Signing the release APK

Although it is suffient to building the application in debug mode, it is recommended to build and sign the application in release mode. To do this, first make sure you have a release signing key:

```
cd ~/.android
keytool -genkey -v -keystore release.keystore -alias androidreleasekey -keyalg RSA -keysize 2048 -validity 10000
```

Then add the following lines in your local.properties file in the project's root directory:

```
STORE_FILE=/home/[username]/.android/release.keystore
STORE_PASSWORD=your_key_store_pw
KEY_ALIAS=androidreleasekey
KEY_PASSWORD=your_release_key_pw
```

You can now build and sign the application in release mode:

```
./gradlew clean
./gradlew aR
```

To install the signed application run:

```
adb install -r app/build/outputs/apk/app-release.apk
```

For your convenience, a signed APK for this project is available [here](https://raw.githubusercontent.com/ilri/azizi-odk-puller/master/app/build/outputs/apk/app-release.apk)

### License

This code is released under the [GNU General Public License v3](http://www.gnu.org/licenses/agpl-3.0.html). Please read LICENSE.txt for more details.
