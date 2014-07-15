package org.cgiar.ilri.odk.pull.backend.services;

import android.app.IntentService;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import java.io.File;

import org.cgiar.ilri.odk.pull.R;
import org.cgiar.ilri.odk.pull.backend.DataHandler;
import org.cgiar.ilri.odk.pull.backend.carriers.Form;

/**
 * Created by Jason Rogena j.rogena@cgiar.org on 11/07/14.
 * This service deletes external item sets and data from the SQLite database for the specified form
 */
public class DeleteFormDataService extends IntentService {

    private static final String TAG = "DeleteFormDataService";
    public static final String KEY_FORM_NAME = "formName";
    private static final int NOTIFICATION_ID = 8732;

    /**
     * The default constructor. This method should be there if Android should call this service
     */
    public DeleteFormDataService() {
        super(TAG);
    }

    /**
     * This method is called whenever the service is called. The service might have already been up
     *  when called
     *
     * @param intent The intent used to call the service
     */
    @Override
    protected void onHandleIntent(Intent intent) {
        String formName = intent.getStringExtra(KEY_FORM_NAME);

        if(formName != null){
            File formDir = new File(Form.BASE_ODK_LOCATION + formName + Form.EXTERNAL_ITEM_SET_SUFFIX);
            if(formDir.exists() && formDir.exists()){
                File csvFile = new File(Form.BASE_ODK_LOCATION + formName + Form.EXTERNAL_ITEM_SET_SUFFIX+"/"+Form.CSV_FILE_NAME);
                if(csvFile.exists()){
                    csvFile.delete();
                    Log.i(TAG, "Deleted " + csvFile.getPath());
                }
                else{
                    Log.w(TAG, csvFile.getPath() + " does not exist. Not deleting anything");
                }

            }
            else{
                Log.w(TAG, formDir.getPath() + " does not exist. Not deleting anything");
            }

            File adbFormDir = new File(Form.BASE_ODK_LOCATION + formName.replaceAll("[^A-Za-z0-9]", "_") + Form.EXTERNAL_ITEM_SET_SUFFIX);
            if(adbFormDir.exists() && adbFormDir.exists()){
                File csvFile = new File(Form.BASE_ODK_LOCATION + formName.replaceAll("[^A-Za-z0-9]", "_") + Form.EXTERNAL_ITEM_SET_SUFFIX+"/"+Form.CSV_FILE_NAME);
                if(csvFile.exists()){
                    csvFile.delete();
                    Log.i(TAG, "Deleted " + csvFile.getPath());
                }
                else{
                    Log.w(TAG, csvFile.getPath() + " does not exist. Not deleting anything");
                }

            }
            else{
                Log.w(TAG, adbFormDir.getPath() + " does not exist. Not deleting anything");
            }

            DataHandler.deleteFormData(this, formName);
            updateNotification(formName, getString(R.string.deleted_external_data_for_form));
        }
        else{
            Log.w(TAG, "The provided form name is null");
        }
    }

    /**
     * This method creates/updates the notification for this service
     *
     * @param title
     * @param details
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
}
