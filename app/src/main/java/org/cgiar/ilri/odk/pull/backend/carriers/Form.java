package org.cgiar.ilri.odk.pull.backend.carriers;

import android.os.Environment;
import android.util.Log;

import java.util.Date;

/**
 * Created by Jason Rogena j.rogena@cgiar.org on 09/07/14.
 * This is just a carrier class for fom data
 */
public class Form {

    private static final String TAG = "Form";

    private static final String PULL_DONT = "dnt";
    private static final String PULL_5_MIN = "5_min";
    private static final String PULL_15_MIN = "15_min";
    private static final String PULL_30_MIN = "30_min";
    private static final String PULL_1_HR = "1_hr";
    private static final String PULL_24_HR = "24_hr";

    public static final String BASE_ODK_LOCATION = Environment.getExternalStorageDirectory() + "/odk/forms/";
    public static final String EXTERNAL_ITEM_SET_SUFFIX = "-media";
    public static final String CSV_FILE_NAME = "itemsets.csv";

    private String name;
    private String pullFrequency;
    private boolean pullWhenInternetOn;
    private boolean pullWhenODKLaunched;
    private long lastPull;

    public Form(String name, String pullFrequency, boolean pullWhenInternetOn, boolean pullWhenODKLaunched){
        this.name = name;
        this.pullFrequency = pullFrequency;
        this.pullWhenInternetOn = pullWhenInternetOn;
        this.pullWhenODKLaunched = pullWhenODKLaunched;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPullFrequency() {
        return pullFrequency;
    }

    public void setPullFrequency(String pullFrequency) {
        this.pullFrequency = pullFrequency;
    }

    public boolean isPullWhenInternetOn() {
        return pullWhenInternetOn;
    }

    public void setPullWhenInternetOn(boolean pullWhenInternetOn) {
        this.pullWhenInternetOn = pullWhenInternetOn;
    }

    public boolean isPullWhenODKLaunched() {
        return pullWhenODKLaunched;
    }

    public void setPullWhenODKLaunched(boolean pullWhenODKLaunched) {
        this.pullWhenODKLaunched = pullWhenODKLaunched;
    }

    public void setLastPull(String lastPull){
        this.lastPull = Long.parseLong(lastPull);
    }

    public void setLastPull(long lastPull){
        this.lastPull = lastPull;
    }

    public long getLastPull(){
        return this.lastPull;
    }

    /**
     * This method checks whether this form is due for an external data update
     *
     * @return True if the form is due for the update
     */
    public boolean isTimeForPull(){
        long currTime = new Date().getTime();

        long timeDiff = currTime - lastPull;

        Log.d(TAG, "Time difference between last update and current time is "+String.valueOf(timeDiff/1000)+" seconds");

        long maxTimePull = 60000;//number of milliseconds in a minute

        Log.d(TAG, "Pull frequency is "+pullFrequency);
        if(pullFrequency.equals(PULL_5_MIN)) {
            maxTimePull = maxTimePull * 5;
            Log.d(TAG, "MaxTimePull =  "+String.valueOf(maxTimePull/1000)+" seconds");
        }
        else if(pullFrequency.equals(PULL_15_MIN)) {
            maxTimePull = maxTimePull * 15;
            Log.d(TAG, "MaxTimePull =  "+String.valueOf(maxTimePull/1000)+" seconds");
        }
        else if(pullFrequency.equals(PULL_30_MIN)) {
            maxTimePull = maxTimePull * 30;
            Log.d(TAG, "MaxTimePull =  "+String.valueOf(maxTimePull/1000)+" seconds");
        }
        else if(pullFrequency.equals(PULL_1_HR)) {
            maxTimePull = maxTimePull * 60;
            Log.d(TAG, "MaxTimePull =  "+String.valueOf(maxTimePull/1000)+" seconds");
        }
        else if(pullFrequency.equals(PULL_24_HR)) {
            maxTimePull = maxTimePull * 60 * 24;
            Log.d(TAG, "MaxTimePull =  "+String.valueOf(maxTimePull/1000)+" seconds");
        }
        else if(pullFrequency.equals(PULL_DONT)){
            Log.i(TAG, "Not supposed to pull this form");
            return false;
        }
        else {
            Log.w(TAG, "Unable to determine the pull frequency.");
            return false;
        }

        if(maxTimePull < timeDiff) return true;
        else return false;
    }
}
