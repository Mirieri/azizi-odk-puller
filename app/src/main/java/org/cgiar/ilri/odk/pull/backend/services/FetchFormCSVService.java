package org.cgiar.ilri.odk.pull.backend.services;

import android.app.IntentService;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import org.cgiar.ilri.odk.pull.R;
import org.cgiar.ilri.odk.pull.backend.DataHandler;
import org.cgiar.ilri.odk.pull.backend.carriers.Form;

/**
 * Created by Jason Rogena j.rogena@cgiar.org on 09/07/14.
 * This class fetches the external data sets for the form
 */
public class FetchFormCSVService extends IntentService {

    private static final String TAG = "FetchFormCSVService";
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
        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(this)
                        .setSmallIcon(R.drawable.ic_launcher)
                        .setContentTitle(title)
                        .setContentText(details);

        NotificationManager mNotificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

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
            updateNotification(formName, getString(R.string.fetching_csv));

            String csv = null;
            try {
                String jsonString = DataHandler.sendDataToServer(this, null, DataHandler.URI_FETCH_CSV + URLEncoder.encode(formName, "UTF-8"));
                if(jsonString !=null){
                    try {
                        JSONObject jsonObject = new JSONObject(jsonString);
                        csv = jsonObject.getString(DataHandler.DATA_CSV);
                        if(csv.length() == jsonObject.getInt(DataHandler.DATA_CSV_LENGTH)){
                            boolean result = saveCSVInFile(csv);
                            if(result){
                                updateNotification(formName, getString(R.string.successful_saved_csv));
                                Log.i(TAG, "Successfully gotten csv for "+formName);
                            }
                            else{
                                updateNotification(formName, getString(R.string.unable_save_csv));
                                Log.w(TAG, "Unable to get csv for "+formName);
                            }
                        }
                        else{
                            Log.e(TAG, "Actual CSV length not same specified length. Doing nothing");
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

    /**
     * This method saves the CSV item set for the form that is currently being handled
     *
     * @param csv The CSV String to be saved
     * @return True if the save was successful
     */
    private boolean saveCSVInFile(String csv){

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
                    String fileName = Form.BASE_ODK_LOCATION + formName + Form.EXTERNAL_ITEM_SET_SUFFIX + "/" + Form.CSV_FILE_NAME;
                    String adbFileName = Form.BASE_ODK_LOCATION + formName.replaceAll("[^A-Za-z0-9]", "_") + Form.EXTERNAL_ITEM_SET_SUFFIX + "/" + Form.CSV_FILE_NAME;

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
