/*
 Note that this application has been custom made by and for use by the ILRI Azizi Biorepository team and does not follow any standards at all ;). Server side code used by this application is in another repository. You can however reverse engineer this app or simply contact me if you feel like you have to use this App. (C) 2015 Jason Rogena <j.rogena@cgiar.org>

 This program is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.cgiar.ilri.odk.pull.backend.services;

import android.app.IntentService;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import org.cgiar.ilri.odk.pull.R;
import org.cgiar.ilri.odk.pull.SettingsActivity;
import org.cgiar.ilri.odk.pull.backend.DataHandler;
import org.cgiar.ilri.odk.pull.backend.carriers.Form;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by Jason Rogena j.rogena@cgiar.org on 11th July 2014.
 * This service deletes external item sets and data from the SQLite database for the specified form
 */
public class DeleteFormDataService extends IntentService {

    private static final String TAG = "ODKPuller.DeleteFormDataService";
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
            List<String> pulledFiles = new ArrayList<String>();
            //get list of ODK Pull files for this form from the server
            try {
                String jsonString = DataHandler.sendDataToServer(this, null, DataHandler.URI_FETCH_FORM_FILE_NAMES + URLEncoder.encode(formName, "UTF-8"));
                JSONArray fileNames = new JSONObject(jsonString).getJSONArray("files");
                for(int index = 0; index < fileNames.length(); index++) {
                    if(fileNames.getString(index).equals(Form.DEFAULT_CSV_FILE_NAME)){
                        pulledFiles.add(fileNames.getString(index)+Form.SUFFIX_CSV);
                    }
                    else {
                        pulledFiles.add(fileNames.getString(index)+Form.SUFFIX_CSV);
                        pulledFiles.add(fileNames.getString(index)+Form.SUFFIX_CSV+Form.SUFFIX_IMPORTED);
                        pulledFiles.add(fileNames.getString(index)+Form.SUFFIX_DB);
                        pulledFiles.add(fileNames.getString(index)+Form.SUFFIX_DB+Form.SUFFIX_JOURNAL);
                    }
                }
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            } catch (JSONException e) {
                e.printStackTrace();
            }
            //delete files that might have been pulled using the normal way
            File formDir = new File(Form.BASE_ODK_LOCATION + formName + Form.EXTERNAL_ITEM_SET_SUFFIX);
            if(formDir.exists() && formDir.exists()){
                for(int index = 0; index < pulledFiles.size(); index++){
                    File file = new File(Form.BASE_ODK_LOCATION + formName + Form.EXTERNAL_ITEM_SET_SUFFIX+File.separator+pulledFiles.get(index));
                    if(file.exists()){
                        file.delete();
                        Log.i(TAG, "Deleted " + file.getPath());
                    }
                    else{
                        Log.w(TAG, file.getPath() + " does not exist. Not deleting anything");
                    }
                }
            }
            else{
                Log.w(TAG, formDir.getPath() + " does not exist. Not deleting anything");
            }
            //delete files that might have been pushed using adb
            File adbFormDir = new File(Form.BASE_ODK_LOCATION + formName.replaceAll("[^A-Za-z0-9]", "_") + Form.EXTERNAL_ITEM_SET_SUFFIX);
            if(adbFormDir.exists() && adbFormDir.exists()){
                for(int index = 0; index < pulledFiles.size(); index++){
                    File file = new File(Form.BASE_ODK_LOCATION + formName.replaceAll("[^A-Za-z0-9]", "_") + Form.EXTERNAL_ITEM_SET_SUFFIX+File.separator+pulledFiles.get(index));
                    if(file.exists()){
                        file.delete();
                        Log.i(TAG, "Deleted " + file.getPath());
                    }
                    else{
                        Log.w(TAG, file.getPath() + " does not exist. Not deleting anything");
                    }
                }
            }
            else{
                Log.w(TAG, adbFormDir.getPath() + " does not exist. Not deleting anything");
            }
            DataHandler.deleteFormData(this, formName);
            String deletedFilesString = "";
            for(int i = 0; i < pulledFiles.size(); i++) {
                deletedFilesString = deletedFilesString + "\n - " + pulledFiles.get(i);
            }
            updateNotification(formName, getString(R.string.deleted_external_data_for_form), getString(R.string.the_following_files_were_deleted) + deletedFilesString);
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
    private void updateNotification(String title, String details, String extraDetails){
        Intent intent = new Intent(this, SettingsActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, 0);
        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(this)
                        .setSmallIcon(R.drawable.ic_launcher)
                        .setContentTitle(title)
                        .setContentText(details)
                        .setStyle(new NotificationCompat.BigTextStyle().bigText(extraDetails))
                        .setContentIntent(pendingIntent)
                        .setAutoCancel(true);;
        //the_following_files_were_deleted
        NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        mNotificationManager.notify(NOTIFICATION_ID, mBuilder.build());
    }
}
