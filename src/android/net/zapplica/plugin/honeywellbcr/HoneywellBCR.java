/*
       Licensed to the Apache Software Foundation (ASF) under one
       or more contributor license agreements.  See the NOTICE file
       distributed with this work for additional information
       regarding copyright ownership.  The ASF licenses this file
       to you under the Apache License, Version 2.0 (the
       "License"); you may not use this file except in compliance
       with the License.  You may obtain a copy of the License at

         http://www.apache.org/licenses/LICENSE-2.0

       Unless required by applicable law or agreed to in writing,
       software distributed under the License is distributed on an
       "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
       KIND, either express or implied.  See the License for the
       specific language governing permissions and limitations
       under the License.
 */

/**
 * provides barcode reader for Honeywell devices using Data Collection Intent API
 */
package net.zapplica.plugin.honeywellbcr;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaWebView;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;
import android.os.Bundle;
import android.os.Build;

/**
 * @author francis.appels@yahoo.com
 *
 */
public class HoneywellBCR extends CordovaPlugin {

    // Debugging
    private static final String TAG = "HoneywellBCR";
    private static final boolean D = false;

    // BCR states
    // Constants that indicate the current connection state
    public static final int STATE_NONE = 0; // we're doing nothing
    public static final int STATE_READY = 1; // reading BCR reader
    public static final int STATE_READING = 2; // reading BCR reader
    public static final int STATE_READ = 3; // read received BCR reader
    public static final int STATE_ERROR = 4; // Error
    public static final int STATE_DESTROYED = 5; // BCR reader destroyed
    private int mState;

    // BCR actions
    private static final String ACTION_INIT = "init";
    private static final String ACTION_DESTROY = "destroy";
    private static final String ACTION_READ = "read";
    private static final String ACTION_GETSTATE = "getState";

    // Local BCR adapter
    private BCRBroadcastReceiver mCodeScanReceiver = null;
    private boolean bCodeScanReceiverRegistered = false;

    // Member fields
    private JSONObject szComData;
	private String szComResult;
    private static final String ACTION_BARCODE_DATA = "com.honeywell.action.MY_BARCODE_DATA";
    private static final String EXTRA_CONTROL = "com.honeywell.aidc.action.ACTION_CONTROL_SCANNER";
    private static final String EXTRA_SCAN = "com.honeywell.aidc.extra.EXTRA_SCAN";

    /**
     * Honeywell DataCollection Intent API
     * Claim scanner
     * Permissions:
     * "com.honeywell.decode.permission.DECODE"
     */
    public static final String ACTION_CLAIM_SCANNER = "com.honeywell.aidc.action.ACTION_CLAIM_SCANNER";

    /**
     * Honeywell DataCollection Intent API
     * Release scanner claim
     * Permissions:
     * "com.honeywell.decode.permission.DECODE"
     */
    public static final String ACTION_RELEASE_SCANNER = "com.honeywell.aidc.action.ACTION_RELEASE_SCANNER";

    /**
     * Honeywell DataCollection Intent API
     * Optional. Sets the scanner to claim. If scanner is not available or if extra is not used,
     * DataCollection will choose an available scanner.
     * Values : String
     * "dcs.scanner.imager" : Uses the internal scanner
     * "dcs.scanner.ring" : Uses the external ring scanner
     */
    public static final String EXTRA_SCANNER = "com.honeywell.aidc.extra.EXTRA_SCANNER";

    /**
     * Honeywell DataCollection Instent API
     * Optional. Sets the profile to use. If profile is not available or if extra is not used,
     * the scanner will use factory default properties (not "DEFAULT" profile properties).
     * Values : String
     */
    public static final String EXTRA_PROFILE = "com.honeywell.aidc.extra.EXTRA_PROFILE";

    /**
     * Honeywell DataCollection Intent API
     * Optional. Overrides the profile properties (non-persistend) until the next scanner claim.
     * Values : Bundle
     */
    public static final String EXTRA_PROPERTIES = "com.honeywell.aidc.extra.EXTRA_PROPERTIES";

    /**
     * Create a BCR reader
     */
    public HoneywellBCR() {
        this.setState(STATE_NONE);
    }

    /**
     * Sets the context of the Command. This can then be used to do things like get
     * file paths associated with the Activity.
     *
     * @param cordova The context of the main Activity.
     * @param webView The associated CordovaWebView.
     */
    @Override
    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
        super.initialize(cordova, webView);
    }

    @Override
    public void onDestroy() {
        this.setState(STATE_DESTROYED);
        if ((mCodeScanReceiver != null) && this.bCodeScanReceiverRegistered) {
            this.cordova.getActivity().unregisterReceiver(mCodeScanReceiver);
        }
        releaseScanner();
        if (D)
            Log.d(TAG, "Destroyed");
        super.onDestroy();
    }

    /**
     * Execute supported functions
     */
    @Override
    public boolean execute(String action, JSONArray args, final CallbackContext callbackContext) throws JSONException {

        if (D)
            Log.d(TAG, "Action: " + action);

        if (ACTION_INIT.equals(action)) {
            // init BroadcastReceiver
            if (mCodeScanReceiver == null && !this.bCodeScanReceiverRegistered) {
                mCodeScanReceiver = new BCRBroadcastReceiver();
                // Register for broadcasts to listen to scanner
                IntentFilter filter = new IntentFilter();
                filter.addAction(ACTION_BARCODE_DATA);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    cordova.getActivity().registerReceiver(mCodeScanReceiver, filter, Context.RECEIVER_EXPORTED);
                } else {
                    cordova.getActivity().registerReceiver(mCodeScanReceiver, filter);
                }
                this.bCodeScanReceiverRegistered = true;
            }
            claimScanner();
            this.setState(STATE_READY);
            callbackContext.success();
        } else if (ACTION_DESTROY.equals(action)) {
            if ((mCodeScanReceiver != null)) {
                this.cordova.getActivity().unregisterReceiver(mCodeScanReceiver);
                this.bCodeScanReceiverRegistered = false;
                mCodeScanReceiver = null;
            }
            callbackContext.success();
            this.onDestroy();
        } else if (ACTION_READ.equals(action) && mState == STATE_READY) {
            this.setState(STATE_READING);

            if (D)
                Log.d(TAG, "Reading...");

            cordova.getThreadPool().execute(new Runnable() {
                public void run() {
                    while (true) {
                        if (mState == STATE_READ) {
                            try {
                                PluginResult result = new PluginResult(PluginResult.Status.OK, szComData);
                                result.setKeepCallback(true);
                                callbackContext.sendPluginResult(result);
                                mState = STATE_READING;
                                if (D)
                                    Log.d(TAG, "Read timestamp = " + szComResult);
                                Thread.sleep(500);
                            } catch (InterruptedException e) {
                                mState = STATE_ERROR;
                                callbackContext.error(e.getMessage());
                                break;
                            }
                        } else if ((mState == STATE_DESTROYED) || (mState == STATE_ERROR)) {
                            if (mState == STATE_ERROR) callbackContext.error("Not Read");
                            break;
                        }
                        try {
                            Thread.sleep(50);
                        } catch (InterruptedException e) {
                            mState = STATE_ERROR;
                            callbackContext.error(e.getMessage());
                            break;
                        }
                    }
                }
            });
        } else if (ACTION_GETSTATE.equals(action)) {
            JSONObject stateJSON = new JSONObject();
            try {
                stateJSON.put("state", mState);
                callbackContext.success(stateJSON);
            } catch (JSONException e) {
                Log.e(TAG, e.getMessage());
                this.setState(STATE_ERROR);
                callbackContext.error(e.getMessage());
            }
        } else {
            callbackContext.error("Action '" + action + "' not supported (now) state = " + mState);
        }

        return true;
    }

    private void setState(int state) {
        this.mState = state;
    }

    // The BroadcastReceiver that listens BCR feedback and trigger
    private class BCRBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (action.equals(ACTION_BARCODE_DATA) && mState == STATE_READING) {
                int version = intent.getIntExtra("version", 0);
                if (version >= 1) {
                    Bundle bundle = new Bundle();
                    bundle = intent.getExtras();
                    assert bundle != null;
                    JSONObject obj = new JSONObject();
                    try {
                        obj.put("text", bundle.getString("data"));
                        obj.put("format", bundle.getString("codeId"));
                    } catch (Exception e) {
                        Log.e(TAG, "Exception occured:" + e.getMessage());
                    }
                    szComData = obj;
                    szComResult = bundle.getString("timestamp");
                    mState = STATE_READ;
                }
            }
        }
    }

    private void claimScanner() {
        Bundle properties = new Bundle();


        //When we press the scan button and read a barcode, a new Broadcast intent will be launched by the service
        properties.putBoolean("DPR_DATA_INTENT", true);

        //That intent will have the action "ACTION_BARCODE_DATA"
        // We will capture the intents with that action (every scan event while in the application)
        // in our BroadcastReceiver barcodeDataReceiver.
        properties.putString("DPR_DATA_INTENT_ACTION", ACTION_BARCODE_DATA);
        //properties.putString("TRIGGER_MODE", "continuous");

        Intent intent = new Intent();
        intent.setAction(ACTION_CLAIM_SCANNER);

        /*
         * We use setPackage() in order to send an Explicit Broadcast Intent, since it is a requirement
         * after API Level 26+ (Android 8)
         */
        intent.setPackage("com.intermec.datacollectionservice");

        //We will use the internal scanner
        intent.putExtra(EXTRA_SCANNER, "dcs.scanner.imager");

        /*
        We can use a profile like "MyProfile1", so a profile with this name has to be created in Scanner settings:
               Android Settings > Honeywell Settings > Scanning > Internal scanner > "+"
        - If we use "DEFAULT" it will apply the settings from the Default profile in Scanner settings
        - If not found, it will use Factory default settings.
         */
        intent.putExtra(EXTRA_PROFILE, "DEFAULT");
        intent.putExtra(EXTRA_PROPERTIES, properties);


        this.cordova.getActivity().sendBroadcast(intent);
    }

    private void releaseScanner() {

        Intent intent = new Intent();
        intent.setAction(ACTION_RELEASE_SCANNER);
        intent.setPackage("com.intermec.datacollectionservice");
        this.cordova.getActivity().sendBroadcast(intent);
    }
}
