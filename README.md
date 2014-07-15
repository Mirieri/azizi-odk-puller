# Azizi ODK Pull
This application can be used to fetch external item sets for ODK Collect forms. 
Refer to [ODK External Itemsets page](http://opendatakit.org/help/form-design/external-itemsets/)
for more information.

Note that this application has been custom made by and for use by the ILRI Azizi Bio-Repository team
and does not follow any standards at all ;).
Server side code used by this application is in another repository. You can however reverse engineer this app or simply contact [JASON](www.google.com/+jasonrogena) if you feel
like you have to use this App.

## Building the project
 1. Create the local.properties file in the project's root directory and specify Android SDK location there like this 
    sdk.dir=/android/SDK/location

 2. Run the following command(s) in the project's root directory to build the project
    ./gradlew build 

 3. You can also import the Project as a local Android Studios project on your machine