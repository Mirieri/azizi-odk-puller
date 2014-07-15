package org.cgiar.ilri.odk.pull.backend.services;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import org.cgiar.ilri.odk.pull.backend.DataHandler;
import org.cgiar.ilri.odk.pull.backend.carriers.Form;

/**
 * Created by Jason Rogena j.rogena@cgiar.org on 11/07/14.
 * This service is called whenever the network comes back on
 */
public class NetworkOnService extends IntentService {
    private static final String TAG = "NetworkOnService";

    /**
     * Default constructor. Make sure this method is there otherwise Android will not be able to start the service
     */
    public NetworkOnService() {
        super(TAG);
    }

    /**
     * This method is called whenever this service is called on. Note that the service might have already been started
     * @param intent The intent used to call the service
     */
    @Override
    protected void onHandleIntent(Intent intent) {
        Log.d(TAG,"************************************************");
        //check if device is connected to the internet
        ConnectivityManager cm = (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        boolean isConnected = activeNetwork != null && activeNetwork.isConnectedOrConnecting();

        if(isConnected){
            List<Form> allDueForms = getAllDueForms();

            for(int index = 0; index<allDueForms.size(); index++){
                startPullService(allDueForms.get(index));
            }
        }
        else{
            Log.i(TAG, "The device seems to be disconnected from the internet. Not fetching any external itemsets");
        }
        Log.d(TAG,"************************************************");
    }

    /**
     * Get all forms that need their data fetched whenever network comes back on
     *
     * @return A list of all forms that need their data fetched whenever the network comes on
     */
    private List<Form> getAllDueForms(){
        List<Form> allForms = DataHandler.getAllForms(this);

        List<Form> dueForms = new ArrayList<Form>();

        for(int index = 0; index < allForms.size(); index++){
            Log.d(TAG, "Current form is: " + allForms.get(index).getName());
            if(!allForms.get(index).isPullWhenInternetOn()){
                Log.i(TAG, allForms.get(index).getName() + " does not need data pulled right now");
            }
            else{
                dueForms.add(allForms.get(index));
                Log.i(TAG, allForms.get(index).getName() + " needs data pulled like now");
            }
        }

        return dueForms;
    }

    /**
     * This method starts a service for fetching external item sets for the specified form
     *
     * @param form The form whose data is to be fetched
     */
    public void startPullService(Form form){
        Intent intent = new Intent(NetworkOnService.this, FetchFormCSVService.class);
        intent.putExtra(FetchFormCSVService.KEY_FORM_NAME, form.getName());

        startService(intent);
    }
}
