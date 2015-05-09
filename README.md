## Azizi ODK Puller [![Build Status](https://travis-ci.org/ilri/azizi-odk-puller.svg?branch=master)](https://travis-ci.org/ilri/azizi-odk-puller)

This application can be used to periodically fetch external data to be used by [ODK Collect](https://opendatakit.org/use/collect/) and possibly any other ODK application running on Android.
Refer to the [ODK External Itemsets page](https://opendatakit.org/help/form-design/external-itemsets/) and [ODK Data Preloading page](https://opendatakit.org/help/form-design/data-preloading/) for more information.

Note that this application has been custom made by and for use by the ILRI Azizi Biorepository team and has been tested for this purpose only.
Server side code used by this application is under [another GitHub repository](https://github.com/ilri/azizi-biorepository/).

### Building the project

To buid this [Gradle](https://gradle.org/) project, run the following commands:

    ./gradlew clean
    ./gradlew build --debug

### License

This code is released under the [GNU General Public License v3](http://www.gnu.org/licenses/agpl-3.0.html). Please read LICENSE.txt for more details.
