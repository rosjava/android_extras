/*
 * Copyright (c) 2015, Tal Regev
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name of the Android Sensors Driver nor the names of its
 *       contributors may be used to endorse or promote products derived from
 *       this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

package org.ros.android.android_all_sensors_driver;


import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.Toast;

import org.ros.internal.node.client.MasterClient;
import org.ros.internal.node.xmlrpc.XmlRpcTimeoutException;
import org.ros.namespace.GraphName;
import org.ros.node.NodeConfiguration;

import java.net.URI;
import java.net.URISyntaxException;

//from https://github.com/talregev/android_core/blob/indigo/android_10/src/org/ros/android/MasterChooser.java
/**
 * Created by Tal Regev on 10/01/15.
 * @author tal.regev@gmail.com  (Tal Regev)
 * My Master Chooser!! :)
 */
public class MasterChooser extends org.ros.android.MasterChooser {
    /**
     * The key with which the last used {@link URI} will be stored as a
     * preference.
     */
    private static final String PREFS_KEY_NAME = "URI_KEY";
    private static final String DEFAULT_ROBOT_NAME = "phone1";

    private android.widget.Button connectButton;
    private EditText uriText;
    private EditText robotName;
    private String radioButtonState="AllSensors";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LayoutInflater layoutInflater = super.getLayoutInflater();
        ViewGroup viewGroup = (ViewGroup) ((ViewGroup) findViewById(android.R.id.content)).getChildAt(0);
        layoutInflater.inflate(R.layout.my_master_chooser,viewGroup);
        robotName = (EditText) findViewById(R.id.robot_text);
        uriText = (EditText) findViewById(R.id.master_chooser_uri);
        connectButton = (Button) findViewById(R.id.master_chooser_ok);
    }

    @Override
    public void okButtonClicked(View unused) {
        // Get the current text entered for URI.
        // Prevent further edits while we verify the URI.
        uriText.setEnabled(false);
        connectButton.setEnabled(false);
        robotName.setEnabled(false);

        final String uri = uriText.getText().toString();
        final String userRobotName = robotName.getText().toString();

        // Make sure the URI can be parsed correctly.
        try {
            new URI(uri);
        } catch (URISyntaxException e) {
            Toast.makeText(MasterChooser.this, "Invalid URI.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Make sure the URI can be parsed correctly and that the master is
        // reachable.
        new AsyncTask<Void, Void, Boolean>() {
            @Override
            protected Boolean doInBackground(Void... params) {
                try {
                    toast("Trying to reach master...");
                    MasterClient masterClient = new MasterClient(new URI(uri));
                    masterClient.getUri(GraphName.of("android/master_chooser_activity"));
                    toast("Connected!");
                    return true;
                } catch (URISyntaxException e) {
                    toast("Invalid URI.");
                    return false;
                } catch (XmlRpcTimeoutException e) {
                    toast("Master unreachable!");
                    return false;
                }
            }
            @Override
            protected void onPostExecute(Boolean result) {
                if (result) {
                    // If the displayed URI is valid then pack that into the intent.
                    String robotName = userRobotName.replaceAll(" ", "_").toLowerCase();
                    SharedPreferences.Editor editor = getPreferences(MODE_PRIVATE).edit();
                    editor.putString(PREFS_KEY_NAME, uri);
                    editor.apply();
                    // Package the intent to be consumed by the calling activity.
                    Intent intent = createNewMasterIntent(false, true);
                    intent.putExtra("ROS_MASTER_URI", uri);
                    intent.putExtra("ROBOT_NAME", robotName);
                    intent.putExtra("APP_STATE", radioButtonState);
                    setResult(RESULT_OK, intent);
                    finish();
                } else {
                    if (uri.length() == 0) {
                        // If there is no text input then set it to the default URI.
                        uriText.setText(NodeConfiguration.DEFAULT_MASTER_URI.toString());
                        toast("Empty URI not allowed.");
                        return;
                    }
                    if (robotName.length() == 0) {
                        // If there is no text input then set it to the default URI.
                        uriText.setText(DEFAULT_ROBOT_NAME);
                        toast("Empty Robot Name not allowed.");
                        return;
                    }
                    connectButton.setEnabled(true);
                    uriText.setEnabled(true);
                    robotName.setEnabled(true);
                }
            }
        }.execute();
    }

    public void onRadioButtonClicked(View view) {
        // Is the button now checked?
        boolean checked = ((RadioButton) view).isChecked();

        // Check which radio button was clicked
        switch(view.getId()) {
            case R.id.radioButtonAllSensors:
                if (checked)
                    radioButtonState = "AllSensors";
                    break;
            case R.id.radioButtonSensors:
                if (checked)
                    radioButtonState = "Sensors";
                    break;
            case R.id.radioButtonCamera:
                if(checked)
                    radioButtonState = "Camera";
                    break;
        }
    }

    @Override
    protected void toast(final String text) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(MasterChooser.this, text, Toast.LENGTH_SHORT).show();
            }
        });
    }
}
