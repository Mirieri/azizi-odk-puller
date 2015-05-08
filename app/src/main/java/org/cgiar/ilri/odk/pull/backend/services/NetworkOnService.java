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
 * Created by Jason Rogena j.rogena@cgiar.org on 11th July 2014.
 * This service is called whenever the network comes back on
 */
public class NetworkOnService extends IntentService {
    private static final String TAG = "ODKPuller.NetworkOnService";

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
        Intent intent = new Intent(NetworkOnService.this, FetchFormDataService.class);
        intent.putExtra(FetchFormDataService.KEY_FORM_NAME, form.getName());

        startService(intent);
    }
}
