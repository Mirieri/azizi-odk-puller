package org.cgiar.ilri.odk.pull.backend;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;
import android.widget.Toast;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.cgiar.ilri.odk.pull.R;
import org.cgiar.ilri.odk.pull.backend.carriers.Form;
import org.cgiar.ilri.odk.pull.backend.storage.DatabaseHelper;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by j.rogena@cgiar.org on 09/07/14.
 */
public class DataHandler {
    private static final String TAG = "ODKPuller.DataHandler";
    private static final int HTTP_POST_TIMEOUT = 20000;
    private static final int HTTP_RESPONSE_TIMEOUT = 20000;

    private static final String BASE_URL = "http://azizi.ilri.cgiar.org/repository/mod_ajax.php?page=odk_puller";
    public static final String URI_FETCH_FORMS = "&do=get_list";
    public static final String URI_FETCH_FORM_DATA = "&do=get_data&complete=1&form=";
    public static final String URI_FETCH_FORM_FILE_NAMES = "&do=get_data&complete=0&form=";
    public static final String DATA_FORM_LIST = "forms";
    public static final String DATA_CSV_LENGTH = "csv_length";

    public static final String PREF_ODK_ALREADY_ON = "odkAlreadyLaunched";
    public static final String ODK_ALREADY_LAUCHED = "yes";
    public static final String ODK_NOT_LAUCHED = "no";


    public static String sendDataToServer(Context context, String jsonString, String appendedURL) {
        HttpParams httpParameters = new BasicHttpParams();
        HttpConnectionParams.setConnectionTimeout(httpParameters, HTTP_POST_TIMEOUT);
        HttpConnectionParams.setSoTimeout(httpParameters, HTTP_RESPONSE_TIMEOUT);
        HttpClient httpClient=new DefaultHttpClient(httpParameters);
        HttpGet httpGet=new HttpGet(BASE_URL+appendedURL);
        try{
            //List<NameValuePair> nameValuePairs=new ArrayList<NameValuePair>(1);
            //nameValuePairs.add(new BasicNameValuePair("json", jsonString));
            //httpGet.setEntity(new UrlEncodedFormEntity(nameValuePairs));

            HttpResponse httpResponse=httpClient.execute(httpGet);
            if(httpResponse.getStatusLine().getStatusCode()==200)
            {
                HttpEntity httpEntity=httpResponse.getEntity();
                if(httpEntity!=null)
                {
                    InputStream inputStream=httpEntity.getContent();
                    String responseString=convertStreamToString(inputStream);
                    return responseString.trim();
                }
            }
            else
            {
                Log.e(TAG, "Status Code " + String.valueOf(httpResponse.getStatusLine().getStatusCode()) + " passed");
            }
        }
        catch (Exception e){
            e.printStackTrace();
            setSharedPreference(context, "http_error", e.getMessage());
        }
        if(isConnectedToServer(HTTP_POST_TIMEOUT)){
            setSharedPreference(context, "http_error", "This application was unable to reach http://azizi.ilri.cgiar.org within "+String.valueOf(HTTP_POST_TIMEOUT/1000)+" seconds. Try resetting your network connection");
        }
        return  null;
    }

    private static boolean isConnectedToServer(int timeout) {
        try{
            URL myUrl = new URL("http://azizi.ilri.cgiar.org");
            URLConnection connection = myUrl.openConnection();
            connection.setConnectTimeout(timeout);
            connection.connect();
            return true;
        } catch (Exception e) {
            // Handle your exceptions
            return false;
        }
    }

    /**
     * This method sets a shared preference to the specified value. Note that shared preferences can only handle strings
     *
     * @param context The context from where you want to set the value
     * @param sharedPreferenceKey The key corresponding to the shared preference. All shared preferences accessible by this app are defined in
     *                            DataHandler e.g DataHandler.SP_KEY_LOCALE
     * @param value The value the sharedPreference is to be set to
     */
    public static void setSharedPreference(Context context, String sharedPreferenceKey, String value) {
        SharedPreferences.Editor editor = context.getSharedPreferences(context.getString(R.string.app_name),Context.MODE_PRIVATE).edit();
        editor.putString(sharedPreferenceKey,value);
        editor.commit();
        Log.d(TAG, sharedPreferenceKey+" shared preference saved as "+value);
    }

    /**
     * Gets the vaule of a shared preference accessible by the context
     *
     * @param context Context e.g activity that is requesting for the shared preference
     * @param sharedPreferenceKey The key corresponding to the shared preference. All shared preferences accessible by this app are defined in
     *                            DataHandler e.g DataHandler.SP_KEY_LOCALE
     * @param defaultValue What will be returned by this method if the sharedPreference is empty or unavailable
     *
     * @return The value of the sharedPreference or the default value specified if the sharedPreference is empty
     */
    public static String getSharedPreference(Context context, String sharedPreferenceKey, String defaultValue) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(context.getString(R.string.app_name), Context.MODE_PRIVATE);
        //Log.d(TAG, "value of " + sharedPreferenceKey + " is " + sharedPreferences.getString(sharedPreferenceKey, defaultValue));
        return sharedPreferences.getString(sharedPreferenceKey, defaultValue);
    }

    private static String convertStreamToString(InputStream inputStream) {
        BufferedReader bufferedReader=new BufferedReader(new InputStreamReader(inputStream));
        StringBuilder stringBuilder=new StringBuilder();
        String line=null;
        try
        {
            while((line=bufferedReader.readLine()) != null)
            {
                stringBuilder.append(line+"\n");
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        finally
        {
            try
            {
                inputStream.close();

            } catch (Exception e2)
            {
                e2.printStackTrace();
            }
        }
        return stringBuilder.toString();
    }

    public static boolean saveFormPreferences(Context context, Form form) {
        DatabaseHelper databaseHelper = new DatabaseHelper(context);
        SQLiteDatabase writableDB = databaseHelper.getWritableDatabase();

        if(writableDB.isOpen()){
            int pullInternetOnVal = 0;
            if(form.isPullWhenInternetOn()) pullInternetOnVal = 1;

            int pullODKLaunchesVal = 0;
            if(form.isPullWhenODKLaunched())  pullODKLaunchesVal = 1;

            String query = "name=?";
            String[] columns = new String[]{"id"};

            String[][] results = databaseHelper.runSelectQuery(writableDB, DatabaseHelper.TABLE_FORM, columns, query, new String[]{form.getName()}, null, null, null, null);


            if (results.length == 1){
                databaseHelper.runDeleteQuery(writableDB, DatabaseHelper.TABLE_FORM, "id", new String[]{String.valueOf(results[0][0])});
            }
            else if(results.length > 1){
                Log.e(TAG, "It appears like there exists more than one form in the database with the name "+form + ". Unable to update preference for any of them");
                return false;
            }

            //(id INTEGER PRIMARY KEY, name TEXT, pref_sync_freq TEXT, pref_pull_internet_on TEXT, pref_pull_odk_launches TEXT, last_time_fetched INT);");
            columns = new String[]{"name", "pref_pull_freq", "pref_pull_internet_on", "pref_pull_odk_launches", "last_time_fetched"};
            String[] values = new String[columns.length];

            Log.d(TAG, "New value for form name :"+form.getName());
            Log.d(TAG, "New value for form pull freq :"+form.getPullFrequency());
            Log.d(TAG, "New value for form pullwheninternetavailable :"+String.valueOf(pullInternetOnVal));
            Log.d(TAG, "New value for form pullwhenodklaunched :"+String.valueOf(pullODKLaunchesVal));

            values[0] = form.getName();
            values[1] = form.getPullFrequency();
            values[2] = String.valueOf(pullInternetOnVal);
            values[3] = String.valueOf(pullODKLaunchesVal);
            values[4] = String.valueOf(form.getLastPull());

            databaseHelper.runInsertQuery(DatabaseHelper.TABLE_FORM, columns, values, -1, writableDB);
            return true;
        }
        else{
            Log.e(TAG, "Was unable to open the writable database ");
        }

        return false;
    }

    public static Form getForm(Context context, String formName){
        DatabaseHelper databaseHelper = new DatabaseHelper(context);
        SQLiteDatabase readableDB = databaseHelper.getReadableDatabase();

        if(readableDB.isOpen()){
            if(formName != null){
                String query = "name=?";
                String[] columns = new String[]{"name", "pref_pull_freq", "pref_pull_internet_on", "pref_pull_odk_launches", "last_time_fetched"};
                String[][] results = databaseHelper.runSelectQuery(readableDB, DatabaseHelper.TABLE_FORM, columns, query, new String[]{formName}, null, null, null, null);

                if(results.length == 1){
                    boolean pullWhenInternetConnection = false;
                    if(results[0][2].equals("1")) pullWhenInternetConnection = true;

                    boolean pullWhenODKLaunched = false;
                    if(results[0][3].equals("1")) pullWhenODKLaunched = true;

                    Form form = new Form(results[0][0], results[0][1], pullWhenInternetConnection, pullWhenODKLaunched);
                    form.setLastPull(results[0][4]);

                    return form;
                }
                else if(results.length == 0){
                    Log.w(TAG, "No form with name "+formName+" found in database");
                }
                else{
                    Log.e(TAG, "More than one form with the name "+formName+" found in the database. Unable to fetch preferences for any");
                }
            }
            else{
                Log.e(TAG, "provided form name is null. Unable to get data on the form");
            }
        }
        else{
            Log.e(TAG, "Was unable to open a readable database to read form preferences");
        }
        return null;
    }

    public static boolean deleteFormData(Context context, String formName){
        DatabaseHelper databaseHelper = new DatabaseHelper(context);
        SQLiteDatabase writableDB = databaseHelper.getWritableDatabase();
        if(writableDB.isOpen()){
            databaseHelper.runDeleteQuery(writableDB, DatabaseHelper.TABLE_FORM, "name", new String[]{formName});
            Log.i(TAG, "Deleted data on "+formName);
            return true;
        }
        else{
            Log.e(TAG, "Was unable to open a writable database to read form preferences");
        }

        return false;
    }

    public static void updateFormLastUpdateTime(Context context, String formName){
        Form form = getForm(context, formName);
        if(form != null){
            form.setLastPull(new Date().getTime());

            saveFormPreferences(context, form);
        }
        else{
            Log.w(TAG, "Fetched form was null. might mean that the user is 'force' pulling a form that is not saved in the SQLite database");
        }
    }

    public static List<Form> getAllForms(Context context){
        DatabaseHelper databaseHelper = new DatabaseHelper(context);
        SQLiteDatabase readableDB = databaseHelper.getReadableDatabase();
        List<Form> allForms = new ArrayList<Form>();
        if(readableDB.isOpen()){
            //get all the form names
            String[] columns = new String[]{"name"};
            String[][] results = databaseHelper.runSelectQuery(readableDB, DatabaseHelper.TABLE_FORM, columns, null, null, null, null, null, null);

            if(results.length > 0){
                for(int formIndex = 0; formIndex< results.length; formIndex++){
                    Form currForm = getForm(context, results[formIndex][0]);
                    allForms.add(currForm);
                }
            }
            else{
                Log.w(TAG, "No forms in database");
            }
        }
        else{
            Log.e(TAG, "Was unable to open a readable database to read form preferences");
        }

        return allForms;
    }

    public static List<String> getAllFormsOnServer(Context context) {
        List<String> serverForms = new ArrayList<String>();
        String result = sendDataToServer(context, "", DataHandler.URI_FETCH_FORMS);
        if(result != null) {
            Log.d(TAG, result);
            try {
                //populate the list of forms
                JSONObject resJsonObject = new JSONObject(result);
                JSONArray jsonArray = resJsonObject.getJSONArray(DataHandler.DATA_FORM_LIST);

                for(int formIndex = 0; formIndex < jsonArray.length(); formIndex++){
                    serverForms.add(jsonArray.getString(formIndex));
                }
            }
            catch (JSONException e) {
                Log.e(TAG, "Was unable to convert string from server into json object");
                e.printStackTrace();
            }
        }
        else {
            Log.w(TAG, "DataHandler returned null. Something we");
        }
        return serverForms;
    }

    /**
     * This function returns a list of all ODK forms that have been downloaded on the device regardless
     * of whether they have pull data or not
     *
     * @return
     */
    public static List<String> getAllODKForms() {
        List<String> allForms = new ArrayList<String>();
        File baseODKDir = new File(Form.BASE_ODK_LOCATION);
        File[] fileList = baseODKDir.listFiles();
        for(File currFile : fileList){
            if(currFile.isFile()){
                //check if file is an xml file
                String name = currFile.getName();
                if(name.endsWith(".xml")){
                    //assuming that the file only has a .xml at the end
                    allForms.add(name.replace(".xml",""));
                }
            }
        }
        return allForms;
    }

}
