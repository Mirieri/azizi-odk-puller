package org.cgiar.ilri.odk.pull;

import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.util.Log;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;
import java.util.List;

import org.cgiar.ilri.odk.pull.backend.DataHandler;
import org.cgiar.ilri.odk.pull.backend.carriers.Form;
import org.cgiar.ilri.odk.pull.backend.services.DeleteFormDataService;
import org.cgiar.ilri.odk.pull.backend.services.FetchFormCSVService;
import org.cgiar.ilri.odk.pull.backend.services.PersistentService;

/**
 * Created by Jason Rogena j.rogena@cgiar.org
 * This is the where all configuration is done by the user for fetching of the forms
 */
public class SettingsActivity extends PreferenceActivity
                              implements Preference.OnPreferenceChangeListener,
                                            Preference.OnPreferenceClickListener{

    private static final String TAG = "ODKPuller.SettingsActivity";

    private ListPreference prefForm;
    private ListPreference prefPullFrequency;
    private CheckBoxPreference prefPullInternetOn;
    private CheckBoxPreference prefPullODKLaunches;
    private Preference prefPullNow;
    private Preference prefDeleteData;

    private PreferenceCategory categoryForm;
    private PreferenceCategory categoryPulling;
    private PreferenceCategory categoryHouseCleaning;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.settings);

        scheduleServiceStart();

        categoryForm = (PreferenceCategory)findPreference("category_form");
        categoryPulling = (PreferenceCategory)findPreference("category_pulling");
        categoryHouseCleaning = (PreferenceCategory)findPreference("category_house_cleaning");

        prefForm = (ListPreference)findPreference("pref_form");
        prefForm.setOnPreferenceChangeListener(this);
        prefPullFrequency = (ListPreference)findPreference("pref_pull_frequency");
        prefPullFrequency.setOnPreferenceChangeListener(this);
        prefPullInternetOn = (CheckBoxPreference)findPreference("pref_pull_internet_on");
        prefPullInternetOn.setOnPreferenceChangeListener(this);
        prefPullODKLaunches = (CheckBoxPreference)findPreference("pref_pull_odk_launches");
        prefPullODKLaunches.setOnPreferenceChangeListener(this);

        prefPullNow = (Preference)findPreference("pref_pull_now");
        prefPullNow.setOnPreferenceClickListener(this);
        prefDeleteData = (Preference)findPreference("pref_delete_data");
        prefDeleteData.setOnPreferenceClickListener(this);

        hideChildPreferences();

        FetchFormsThread fetchFormsThread = new FetchFormsThread();
        fetchFormsThread.execute(0);
    }

    /**
     * This method hides all child preferences in the :
     *      - pulling
     *      - houseCleaning
     *  preference categories.
     *
     * Hiding is done until the user selects a form
     */
    private void hideChildPreferences(){
        categoryPulling.removeAll();
        categoryHouseCleaning.removeAll();
    }

    /**
     * This method shows all preferences in the :
     *      - pulling
     *      - houseCleaning
     *  settings categories.
     *
     *  Showing of the preferences should be done after the use selects a form
     */
    private void showChildPreferences(){
        categoryPulling.addPreference(prefPullFrequency);
        categoryPulling.addPreference(prefPullInternetOn);
        categoryPulling.addPreference(prefPullODKLaunches);
        categoryPulling.addPreference(prefPullNow);

        categoryHouseCleaning.addPreference(prefDeleteData);
    }

    /**
     * This method will be called whenever a preference is changed.
     * Note that, in this method, when you want to use the value of the selected preference
     *  use the one parsed to this method (newValue) and not preference.getValue
     *
     * @param preference The preference that has just changed
     * @param newValue The new value of the preference
     *
     * @return True of you want to effect the new value
     */
    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if(preference == prefForm){
            formChanged(newValue.toString());
        }
        else{
            boolean pullWhenInternetOn = false;
            if(prefPullInternetOn.isChecked()) pullWhenInternetOn = true;
            boolean pullWhenODKLaunched = false;
            if(prefPullODKLaunches.isChecked()) pullWhenODKLaunched = true;

            Form form = new Form(prefForm.getValue(), prefPullFrequency.getValue(), pullWhenInternetOn, pullWhenODKLaunched);

            if(preference == prefForm) form.setName(newValue.toString());
            else if(preference == prefPullFrequency) form.setPullFrequency(newValue.toString());
            else if(preference == prefPullInternetOn){
                if(newValue instanceof Boolean){
                    form.setPullWhenInternetOn((Boolean)newValue);
                }
            }
            else if(preference == prefPullODKLaunches){
                if(newValue instanceof Boolean){
                    form.setPullWhenODKLaunched((Boolean) newValue);
                }
            }

            form.setLastPull(new Date().getTime());

            UpdatePreferenceThread updatePreferenceThread = new UpdatePreferenceThread(form);
            updatePreferenceThread.execute(0);
            if(preference == prefPullFrequency){
                prefPullFrequency.setSummary(newValue.toString());
            }
        }
        return true;
    }

    /**
     * This method should be called whenever the user selects another form
     *
     * @param formName The selected form's name
     */
    private void formChanged(String formName){
        prefForm.setSummary(formName);

        GetFormPreferencesThread getFormPreferencesThread = new GetFormPreferencesThread(formName);
        getFormPreferencesThread.execute(0);
    }

    /**
     * This method is called whenever a registered preference is clicked
     * For now, registered preferences are:
     *      - prefPullNow
     *      - prefDeleteData
     *
     * This method works the same way View.onClickListener does.
     * @param preference
     * @return return true if the click was handled
     */
    @Override
    public boolean onPreferenceClick(Preference preference) {
        if(preference == prefPullNow){
            //start the external data pulling service
            Toast.makeText(this, getString(R.string.fetching_data_for_)+" "+prefForm.getValue(), Toast.LENGTH_LONG).show();
            Intent intent = new Intent(SettingsActivity.this, FetchFormCSVService.class);
            intent.putExtra(FetchFormCSVService.KEY_FORM_NAME, prefForm.getValue());

            startService(intent);
        }
        else if(preference == prefDeleteData){

            //create an event listener for the dialog about to be created
            DialogInterface.OnClickListener onClickListener = new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    if(which==DialogInterface.BUTTON_POSITIVE){
                        dialog.dismiss();

                        Intent intent = new Intent(SettingsActivity.this, DeleteFormDataService.class);
                        intent.putExtra(DeleteFormDataService.KEY_FORM_NAME, prefForm.getValue());

                        startService(intent);

                        formChanged(prefForm.getValue());
                    }
                    else{
                        dialog.cancel();
                    }
                }
            };

            //create a dialog warning the user of the consequences of deleting form data
            AlertDialog.Builder alertDialogBuilder=new AlertDialog.Builder(this);
            alertDialogBuilder.setTitle(getString(R.string.warning));
            alertDialogBuilder
                    .setMessage(SettingsActivity.this.getString(R.string.delete_external_data_warning_) + " " + prefForm.getValue())
                    .setCancelable(false)
                    .setPositiveButton(SettingsActivity.this.getString(R.string.okay), onClickListener)
                    .setNegativeButton(SettingsActivity.this.getString(R.string.cancel), onClickListener);

            AlertDialog deleteDialog = alertDialogBuilder.create();;
            deleteDialog.show();
        }
        return true;
    }

    /**
     * This AsyncTask fetches a list of the forms with external data from the server.
     * Ordinarily, this should be called after all views have been initialized
     *  in onCreate or onResume
     *
     * Make sure you overlay over the interface while doInBackground runs. You don't
     *  want the user trying to select a form when there is none
     */
    private class FetchFormsThread extends AsyncTask<Integer, Integer, List<Form>> {

        private ProgressDialog progressDialog;

        /**
         * Called before asynchronous thread braches off
         */
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progressDialog = ProgressDialog.show(SettingsActivity.this, "", SettingsActivity.this.getString(R.string.fetching_forms_please_wait_));
        }

        /**
         * Code in this method is run asynchronously from the UI thread
         *
         * @param integers Not really used, something is required to be parsed to this method
         * @return String from the server, probably going to be json string
         */
        @Override
        protected List<Form> doInBackground(Integer... integers) {
            Log.d(TAG, "Fetching ODK Pull forms");
            List<Form> requestResult = DataHandler.getAllForms(SettingsActivity.this);
            return requestResult;
        }

        /**
         * This method is called after the the asynchronous thread is done executing
         *
         * @param result Data from the server, probably going to be json
         */
        @Override
        protected void onPostExecute(List<Form> result) {
            super.onPostExecute(result);
            Log.d(TAG, "Fetched "+String.valueOf(result.size())+" ODK Puller forms");
            progressDialog.dismiss();
            if(result != null) {
                String[] formNames =  new String[result.size()];
                for(int formIndex = 0; formIndex < result.size(); formIndex++){
                    formNames[formIndex] = result.get(formIndex).getName();
                }

                prefForm.setEntries(formNames);
                prefForm.setEntryValues(formNames);
            }
            else {
                Toast.makeText(SettingsActivity.this, "No ", Toast.LENGTH_LONG).show();
                Log.w(TAG, "DataHandler returned null. Something we");
            }
        }
    }

    /**
     * This AsyncTask updates preferences for a form in the app's SQLite database
     */
    private class UpdatePreferenceThread extends AsyncTask<Integer, Integer, Boolean>{

        private Form form;

        public UpdatePreferenceThread(Form form){
            this.form = form;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected Boolean doInBackground(Integer... integers) {
            Boolean result = false;

            result = DataHandler.saveFormPreferences(SettingsActivity.this, form);

            return result;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            super.onPostExecute(result);

            if(result){
                Toast.makeText(SettingsActivity.this, "Preferences saved successfully", Toast.LENGTH_LONG).show();
            }
            else{
                Toast.makeText(SettingsActivity.this, "Was unable to save the form's preferences", Toast.LENGTH_LONG).show();
            }
        }
    }

    /**
     * This method schedules the persistent service to start every now and then
     */
    private void scheduleServiceStart(){
        Intent intent = new Intent(SettingsActivity.this, PersistentService.class);
        PendingIntent pendingIntent = PendingIntent.getService(SettingsActivity.this, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);
        AlarmManager alarmManager = (AlarmManager)getSystemService(Context.ALARM_SERVICE);
        alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, new Date().getTime(), PersistentService.RESTART_INTERVAL, pendingIntent);
        Log.i(TAG, "Persistent service now scheduled to start every now and then");
    }

    /**
     * This AsyncTask gets saved preferences for a form from the app's SQLite database
     * This should be called whenever a form is selected from the list of forms
     */
    private class GetFormPreferencesThread extends AsyncTask<Integer, Integer, Form>{

        private String formName;
        private ProgressDialog progressDialog;

        /**
         * Constructor for the AsyncTask (duh)
         *
         * @param formName The name of the form you want to get references for
         */
        public GetFormPreferencesThread(String formName){
            this.formName = formName;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            progressDialog = ProgressDialog.show(SettingsActivity.this, null, SettingsActivity.this.getText(R.string.loading_please_wait_));
        }

        @Override
        protected Form doInBackground(Integer... integers) {
            return DataHandler.getForm(SettingsActivity.this, formName);
        }

        @Override
        protected void onPostExecute(Form form) {
            super.onPostExecute(form);
            showChildPreferences();
            progressDialog.dismiss();

            //populate the preference views with whatever was gotten from the database
            prefPullFrequency.setValue("");
            prefPullFrequency.setSummary(SettingsActivity.this.getString(R.string.pull_frequency_summery));
            prefPullInternetOn.setChecked(false);
            prefPullODKLaunches.setChecked(false);

            if(form != null){
                prefPullFrequency.setValue(form.getPullFrequency());
                prefPullFrequency.setSummary(form.getPullFrequency());
                prefPullInternetOn.setChecked(form.isPullWhenInternetOn());
                prefPullODKLaunches.setChecked(form.isPullWhenODKLaunched());
            }
        }
    }
}
