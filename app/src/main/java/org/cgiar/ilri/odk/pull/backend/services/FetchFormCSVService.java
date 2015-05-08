package org.cgiar.ilri.odk.pull.backend.services;

import android.app.IntentService;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import org.cgiar.ilri.odk.pull.SettingsActivity;
import org.cgiar.ilri.odk.pull.backend.storage.DatabaseHelper;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.cgiar.ilri.odk.pull.R;
import org.cgiar.ilri.odk.pull.backend.DataHandler;
import org.cgiar.ilri.odk.pull.backend.carriers.Form;

/**
 * Created by Jason Rogena j.rogena@cgiar.org on 09/07/14.
 * This class fetches the external data sets for the form
 */
public class FetchFormCSVService extends IntentService {

    private static final String TAG = "ODKPuller.FetchFormCSVService";
    private static final int NOTIFICATION_ID = 322132;

    public static final String KEY_FORM_NAME = "formName";

    private String formName;

    /**
     * Default constructor for the class. Make sure this is there or else Android will be unable to
     *  call this service
     */
    public FetchFormCSVService() {
        super(TAG);
    }

    /**
     * This method updates/creates the notification for this service.
    * Note that the notification generated here is a compact notification
     *
     * @param title The title for the notification
     * @param details The details of the notification
     */
    private void updateNotification(String title, String details){
        Intent intent = new Intent(this, SettingsActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, 0);
        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(this)
                        .setSmallIcon(R.drawable.ic_launcher)
                        .setContentTitle(title)
                        .setContentText(details)
                        .setContentIntent(pendingIntent)
                        .setAutoCancel(true);

        NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.notify(NOTIFICATION_ID, mBuilder.build());
    }

    /**
     * This method is called whenever the service is called. Note that the service might have
     * already been running when it was called
     *
     * @param intent The intent used to call the service
     */
    @Override
    protected void onHandleIntent(Intent intent) {
        formName = intent.getStringExtra(KEY_FORM_NAME);
        if(formName != null){
            //fetch data on the form
            updateNotification(formName, getString(R.string.fetching_data));
            try {
                String jsonString = DataHandler.sendDataToServer(this, null, DataHandler.URI_FETCH_FORM_DATA + URLEncoder.encode(formName, "UTF-8"));
                if(jsonString !=null){
                    try {
                        JSONObject jsonObject = new JSONObject(jsonString);
                        JSONObject fileData = jsonObject.getJSONObject("files");
                        Iterator<String> keys = fileData.keys();
                        while(keys.hasNext()){
                            String currFileName = keys.next();
                            Log.d(TAG, "Processing "+currFileName);
                            JSONArray currFileData = fileData.getJSONArray(currFileName);
                            if(currFileName.equals(Form.DEFAULT_CSV_FILE_NAME)){
                                Log.d(TAG, "Treating "+currFileName+" like a itemset file");
                                String csv = getCSVString(currFileData);
                                saveCSVInFile(currFileName+Form.SUFFIX_CSV, csv);
                            }
                            else {
                                Log.d(TAG, "Treating "+currFileName+" like an external data file");
                                boolean dataDumped = saveDataInDb(currFileName, currFileData);
                                String csv = getCSVString(currFileData);
                                if(dataDumped) {
                                    saveCSVInFile(currFileName+Form.SUFFIX_CSV+Form.SUFFIX_IMPORTED, csv);
                                }
                                else {
                                    saveCSVInFile(currFileName+Form.SUFFIX_CSV, csv);
                                }
                            }
                        }
                    }
                    catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
                else{
                    Log.e(TAG, "Data from server null. Something happened while trying to fetch csv for "+formName);
                }
            }
            catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }
        else{
            Log.w(TAG, "Form name from intent bundle was null, doing nothing");
        }
    }

    private String getCSVString(JSONArray jsonArray) {
        String csv = null;
        if(jsonArray.length() > 0) {
            try {
                csv = "";
                List<String> keys = new ArrayList<String>();
                Iterator<String> iterator = jsonArray.getJSONObject(0).keys();
                while(iterator.hasNext()){
                    String currKey = iterator.next();
                    keys.add(currKey);
                    if(csv.length() == 0){
                        csv = currKey;
                    }
                    else {
                        csv = csv + "," + currKey;
                    }
                }
                csv = csv + "\n";
                for(int rowIndex = 0; rowIndex < jsonArray.length(); rowIndex++) {
                    JSONObject currRow = jsonArray.getJSONObject(rowIndex);
                    for(int keyIndex = 0; keyIndex < keys.size(); keyIndex++){
                        String currValue = currRow.getString(keys.get(keyIndex));
                        if(currValue != null){
                            csv = csv + currValue;
                        }
                        if(keyIndex < keys.size() - 1){//not the last item in row
                            csv = csv + ",";
                        }
                    }
                    csv = csv + "\n";//will potentially lead to having an empty last line in the csv
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        else {
            Log.w(TAG, "Provided jsonArray to be converted to CSV is empty returning null as csv");
        }
        return csv;
    }

    private boolean saveDataInDb(String fileName, JSONArray rows) {
        boolean result = false;
        //TODO: only do this if ODK Collect is not using this file
        String pathToFile = Form.BASE_ODK_LOCATION + formName + Form.EXTERNAL_ITEM_SET_SUFFIX;
        /*File existingDb = new File(pathToFile+File.separator+fileName+Form.SUFFIX_DB);
        existingDb.delete();*/
        final DatabaseHelper databaseHelper = new DatabaseHelper(this, fileName, 1, pathToFile);
        SQLiteDatabase db = databaseHelper.getWritableDatabase();
        if(rows.length() > 0) {
            try {
                List<String> columns = new ArrayList<String>();
                List<String> indexes = new ArrayList<String>();
                Iterator<String> iterator = rows.getJSONObject(0).keys();
                //recreate the tables
                db.execSQL("drop table if exists "+Form.DB_METADATA_TABLE);
                String createMetaTableString = "create table "+Form.DB_METADATA_TABLE + " ("+Form.DB_META_LOCALE_FIELD+" "+Form.DB_META_LOCALE_FIELD_TYPE+")";
                db.execSQL(createMetaTableString);
                databaseHelper.runInsertQuery(Form.DB_METADATA_TABLE, new String[]{Form.DB_META_LOCALE_FIELD}, new String[]{Form.DB_DEFAULT_LOCALE}, -1, db);
                db.execSQL("drop table if exists "+Form.DB_DATA_TABLE);
                String createTableString = "create table "+Form.DB_DATA_TABLE+" (";
                while(iterator.hasNext()){
                    String currKey = iterator.next();
                    if(columns.size() > 0) {//this is the first column
                        createTableString = createTableString + ", ";
                    }
                    createTableString = createTableString + Form.DB_DATA_COLUMN_PREFIX + currKey + " " + Form.DB_DATA_COLUMN_TYPE;
                    columns.add(currKey);
                    if(currKey.endsWith(Form.SUFFIX_INDEX_FIELD)){
                        Log.d(TAG, fileName+" has an index column "+currKey);
                        indexes.add(currKey);
                    }
                }
                //only continue if we have at least one column
                if(columns.size() > 0){
                    createTableString = createTableString + ", " + Form.DB_DATA_SORT_FIELD + " " + Form.DB_DATA_SORT_COLUMN_TYPE + ")";
                    db.execSQL(createTableString);
                    for(int index = 0; index < indexes.size(); index++) {
                        db.execSQL("create index "+indexes.get(index)+Form.SUFFIX_INDEX+" on "+Form.DB_DATA_TABLE+"("+Form.DB_DATA_COLUMN_PREFIX+indexes.get(index)+")");
                    }
                    for(int rowIndex = 0; rowIndex < rows.length(); rowIndex++) {
                        JSONObject currRow = rows.getJSONObject(rowIndex);
                        String[] currColumns = new String[columns.size() + 1];
                        String[] currValues = new String[columns.size() + 1];
                        for(int columnIndex = 0; columnIndex < columns.size(); columnIndex++){
                            currColumns[columnIndex] = Form.DB_DATA_COLUMN_PREFIX+columns.get(columnIndex);
                            currValues[columnIndex] = currRow.getString(columns.get(columnIndex));
                        }
                        currColumns[columns.size()] = Form.DB_DATA_SORT_FIELD;
                        currValues[columns.size()] = String.valueOf((double)rowIndex);//TODO: not sure if should be float or double
                        databaseHelper.runInsertQuery(Form.DB_DATA_TABLE, currColumns, currValues, -1, db);//do not add unique key field index in argument list. Will end up being an extra query
                    }
                    result = true;
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        else {
            Log.w(TAG, "Provided jsonArray to be dumped into a db is empty");
        }
        db.close();
        //copy db to the ADB push directory
        File adbFormDir = new File(Form.BASE_ODK_LOCATION + formName.replaceAll("[^A-Za-z0-9]", "_") + Form.EXTERNAL_ITEM_SET_SUFFIX);
        if(!adbFormDir.exists() || !adbFormDir.isDirectory()){
            adbFormDir.setWritable(true);
            adbFormDir.setReadable(true);
            Log.i(TAG, "Trying to create dir " + adbFormDir.getPath());
        }
        File sourceDbFile = new File(pathToFile+File.separator+fileName+Form.SUFFIX_DB);
        File destDbFile = new File(Form.BASE_ODK_LOCATION + formName.replaceAll("[^A-Za-z0-9]", "_") + Form.EXTERNAL_ITEM_SET_SUFFIX+File.separator+fileName+Form.SUFFIX_DB);
        InputStream in = null;
        OutputStream out = null;
        try {
            in = new FileInputStream(sourceDbFile);
            out = new FileOutputStream(destDbFile);
            // Copy the bits from instream to outstream
            byte[] buf = new byte[1024];
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
            in.close();
            out.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return result;
    }

    /**
     * This method saves the CSV item set for the form that is currently being handled
     *
     * @param csv The CSV String to be saved
     * @return True if the save was successful
     */
    private boolean saveCSVInFile(String fileName, String csv){

        File directory = new File(Form.BASE_ODK_LOCATION);
        boolean dirCreated = false;
        if(!directory.exists() || !directory.isDirectory()){//means that the base directory does not exist
            dirCreated = directory.mkdirs();
            directory.setReadable(true);
            directory.setWritable(true);
            Log.i(TAG, "Trying to create dir "+directory.getPath());
        }
        else{
            dirCreated = true;
            Log.i(TAG, directory.getPath() + " already existed");
        }

        if(dirCreated){
            //check if form specific directory is created

            /*
            One thing to note here is that Collect handles downloaded from aggregate and forms gotten from adb push differently
                - Forms downloaded from aggregate will expect the itemsets.csv file to be in a dir with a name that is exactly the form name
                - Forms gotten from adb push will expect the itemsets.csv file to be in a dir where non-alphanumeric characters in the form name are replaced with underscores

            Will put the itemsets.csv file in both these dirs
             */

            File formDir = new File(Form.BASE_ODK_LOCATION + formName + Form.EXTERNAL_ITEM_SET_SUFFIX);
            dirCreated = false;
            if(!formDir.exists() || !formDir.isDirectory()){
                dirCreated = formDir.mkdirs();
                formDir.setWritable(true);
                formDir.setReadable(true);
                Log.i(TAG, "Trying to create dir " + formDir.getPath());
            }
            else{
                Log.i(TAG, formDir.getPath() + " already existed");
                dirCreated = true;
            }

            File adbFormDir = new File(Form.BASE_ODK_LOCATION + formName.replaceAll("[^A-Za-z0-9]", "_") + Form.EXTERNAL_ITEM_SET_SUFFIX);
            if(dirCreated){
                if(!adbFormDir.exists() || !adbFormDir.isDirectory()){
                    dirCreated = adbFormDir.mkdirs();
                    adbFormDir.setWritable(true);
                    adbFormDir.setReadable(true);
                    Log.i(TAG, "Trying to create dir " + adbFormDir.getPath());
                }
                else{
                    Log.i(TAG, adbFormDir.getPath() + " already existed");
                    dirCreated = true;
                }
            }
            else{
                Log.e(TAG, "Unable to create "+formDir.getPath()+" not attempting to create "+adbFormDir.getPath());
            }



            if(dirCreated){
                try {
                    String adbFileName = Form.BASE_ODK_LOCATION + formName.replaceAll("[^A-Za-z0-9]", "_") + Form.EXTERNAL_ITEM_SET_SUFFIX + "/" + fileName;
                    fileName = Form.BASE_ODK_LOCATION + formName + Form.EXTERNAL_ITEM_SET_SUFFIX + "/" + fileName;

                    File csvFile = new File(fileName);
                    File adbCSVFile = new File(adbFileName);
                    boolean fileCreated = false;

                    if(!csvFile.exists()){
                        fileCreated = csvFile.createNewFile();
                        Log.i(TAG, "Trying to create dir " + csvFile.getPath());
                    }
                    else{
                        csvFile.delete();
                        fileCreated = csvFile.createNewFile();
                        Log.i(TAG, csvFile.getPath() + " already existed");
                    }

                    if(fileCreated){
                        if(!adbCSVFile.exists()){
                            fileCreated = adbCSVFile.createNewFile();
                            Log.i(TAG, "Trying to create dir " + adbCSVFile.getPath());
                        }
                        else{
                            adbCSVFile.delete();
                            fileCreated = adbCSVFile.createNewFile();
                            Log.i(TAG, adbCSVFile.getPath() + " already existed");
                        }

                        if(fileCreated){
                            csvFile.setReadable(true);
                            csvFile.setWritable(true);

                            adbCSVFile.setReadable(true);
                            adbCSVFile.setWritable(true);

                            //OutputStreamWriter outputStreamWriter1 = new OutputStreamWriter(openFileOutput(fileName, Context.MODE_PRIVATE));
                            OutputStreamWriter outputStreamWriter1 = new OutputStreamWriter(new FileOutputStream(csvFile));
                            OutputStreamWriter outputStreamWriter2 = new OutputStreamWriter(new FileOutputStream(adbCSVFile));

                            outputStreamWriter1.write(csv);
                            outputStreamWriter2.write(csv);

                            outputStreamWriter1.close();
                            outputStreamWriter2.close();

                            DataHandler.updateFormLastUpdateTime(FetchFormCSVService.this, formName);

                            return true;
                        }
                        else{
                            Log.e(TAG, "Unable to create " + adbCSVFile.getPath());
                        }
                    }
                    else{
                        Log.e(TAG, "Unable to create " + csvFile.getPath());
                    }
                }
                catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
                catch (IOException e) {
                    e.printStackTrace();
                }
            }
            else{
                Log.e(TAG, "Unable to create " + formDir.getPath());
            }
        }
        else{
            Log.e(TAG, "Unable to create " + directory.getPath());
        }
        return false;
    }
}
