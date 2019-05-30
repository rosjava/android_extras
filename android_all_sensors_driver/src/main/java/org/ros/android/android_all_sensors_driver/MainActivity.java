/*
 * Copyright (c) 2015, Tal Regev
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package org.ros.android.android_all_sensors_driver;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.hardware.Camera;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.location.LocationManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.Surface;
import android.view.SurfaceView;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import com.google.common.base.Preconditions;

import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.OpenCVLoader;
import org.ros.address.InetAddressFactory;
import org.ros.android.RosActivity;
import org.ros.exception.RosRuntimeException;
import org.ros.node.NodeConfiguration;
import org.ros.node.NodeMainExecutor;

import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * @author chadrockey@gmail.com (Chad Rockey)
 * @author axelfurlan@gmail.com (Axel Furlan)
 * @author tal.regev@gmail.com  (Tal Regev)
 */

//from https://github.com/talregev/android_core/blob/indigo/android_10/src/org/ros/android/RosActivity.java

public class MainActivity extends RosActivity {
    static {
        OpenCVLoader.initDebug();
    }

    private static final String TAG = "all::MainActivity";

    public static final int VIEW_MODE_RGBA = 0;
    public static final int VIEW_MODE_GRAY = 1;
    public static final int VIEW_MODE_CANNY = 2;
    public static final int IMAGE_TRANSPORT_COMPRESSION_NONE = 0;
    public static final int IMAGE_TRANSPORT_COMPRESSION_PNG = 1;
    public static final int IMAGE_TRANSPORT_COMPRESSION_JPEG = 2;

    public static int viewMode = VIEW_MODE_RGBA;
    public static int imageCompression = IMAGE_TRANSPORT_COMPRESSION_JPEG;

    public static int imageJPEGCompressionQuality = 80;
    public static int imagePNGCompressionQuality = 3;

    public static int mCameraId = 0;

    protected NodeMainExecutor          nodeMainExecutor;
    protected URI                       masterURI;
    protected NavSatFixPublisher        fix_pub;
    protected ImuPublisher              imu_pub;
    protected MagneticFieldPublisher    magnetic_field_pub;
    protected FluidPressurePublisher    fluid_pressure_pub;
    protected IlluminancePublisher      illuminance_pub;
    protected TemperaturePublisher      temperature_pub;
    protected LocationManager           mLocationManager;
    protected SensorManager             mSensorManager;
    protected CameraPublisher           cam_pub = new CameraPublisher();
    protected CameraBridgeViewBase      mOpenCvCameraView;
    protected String                    robotName;
    protected Boolean                   isCamera = false;
    protected Boolean                   isSensors = false;
    private static final int            MASTER_CHOOSER_REQUEST_CODE = 1;

    @SuppressWarnings("deprecation")
    protected final int numberOfCameras = Camera.getNumberOfCameras();
    protected final int currentApiVersion = android.os.Build.VERSION.SDK_INT;


    public MainActivity() {
        super("ROS All Sensors Driver", "ROS All Sensors Driver");
    }

    @Override
    public void startMasterChooser() {
        Preconditions.checkState(getMasterUri() == null);
        // Call this method on super to avoid triggering our precondition in the
        // overridden startActivityForResult().
        super.startActivityForResult(new Intent(this, MasterChooser.class), MASTER_CHOOSER_REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            if (requestCode == MASTER_CHOOSER_REQUEST_CODE) {
                String host;
                String networkInterfaceName = data.getStringExtra("ROS_MASTER_NETWORK_INTERFACE");
                // Handles the default selection and prevents possible errors
                if (networkInterfaceName == null || networkInterfaceName.equals("")) {
                    host = InetAddressFactory.newNonLoopback().getHostAddress();
                } else {
                    try {
                        NetworkInterface networkInterface = NetworkInterface.getByName(networkInterfaceName);
                        host = InetAddressFactory.newNonLoopbackForNetworkInterface(networkInterface).getHostAddress();
                    } catch (SocketException e) {
                        throw new RosRuntimeException(e);
                    }
                }
                nodeMainExecutorService.setRosHostname(host);
                if (data.getBooleanExtra("ROS_MASTER_CREATE_NEW", false)) {
                    nodeMainExecutorService.startMaster(data.getBooleanExtra("ROS_MASTER_PRIVATE", true));
                } else {
                    URI uri;
                    try {
                        uri = new URI(data.getStringExtra("ROS_MASTER_URI"));
                        robotName = data.getStringExtra("ROBOT_NAME");
                        String appState = data.getStringExtra("APP_STATE");
                        switch (appState)
                        {
                            case "AllSensors":
                                isCamera = true;
                                isSensors = true;
                                break;
                            case "Sensors":
                                isCamera = false;
                                isSensors = true;
                                break;
                            case "Camera":
                                isCamera = true;
                                isSensors = false;
                        }
                    } catch (URISyntaxException e) {
                        throw new RosRuntimeException(e);
                    }
                    nodeMainExecutorService.setMasterUri(uri);
                }
                // Run init() in a new thread as a convenience since it often requires network access.
                new AsyncTask<Void, Void, Void>() {
                    @Override
                    protected Void doInBackground(Void... params) {
                        MainActivity.this.init(nodeMainExecutorService);
                        return null;
                    }
                }.execute();
            } else {
                // Without a master URI configured, we are in an unusable state.
                nodeMainExecutorService.forceShutdown();
            }
        }
    }

    @Override
    @SuppressWarnings("deprecation")
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_main);

        mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.HelloOpenCvView);
        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
        mOpenCvCameraView.setCvCameraViewListener(cam_pub);

        mLocationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        mSensorManager = (SensorManager) this.getSystemService(SENSOR_SERVICE);
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1) //API = 15
    protected void init(NodeMainExecutor nodeEx) {
        nodeMainExecutor = nodeEx;
        masterURI = getMasterUri();

        String[] PERMISSIONS = {"", ""};

        if(isSensors) {
            PERMISSIONS[0] = Manifest.permission.ACCESS_FINE_LOCATION;
        }
        if(isCamera) {
            PERMISSIONS[1] = Manifest.permission.CAMERA;
        }

        ActivityCompat.requestPermissions(this, PERMISSIONS, 0);

        if(isSensors) {

            int sensorDelay = 10000; // 10,000 us == 100 Hz for Android 3.1 and above
            if (currentApiVersion <= android.os.Build.VERSION_CODES.HONEYCOMB) {
                sensorDelay = SensorManager.SENSOR_DELAY_UI; // 16.7Hz for older devices.  They only support enum values, not the microsecond version.
            }


            int tempSensor;
            if (currentApiVersion >= android.os.Build.VERSION_CODES.ICE_CREAM_SANDWICH) {

                tempSensor = Sensor.TYPE_AMBIENT_TEMPERATURE; // Use newer temperature if possible
            } else {
                //noinspection deprecation
                tempSensor = Sensor.TYPE_TEMPERATURE; // Older temperature
            }

            NodeConfiguration nodeConfiguration = NodeConfiguration.newPublic(InetAddressFactory.newNonLoopback().getHostAddress());
            nodeConfiguration.setMasterUri(masterURI);
            nodeConfiguration.setNodeName(robotName + "_android_sensors_driver_magnetic_field");
            this.magnetic_field_pub = new MagneticFieldPublisher(mSensorManager, sensorDelay, robotName);
            nodeMainExecutor.execute(this.magnetic_field_pub, nodeConfiguration);

            NodeConfiguration nodeConfiguration3 = NodeConfiguration.newPublic(InetAddressFactory.newNonLoopback().getHostAddress());
            nodeConfiguration3.setMasterUri(masterURI);
            nodeConfiguration3.setNodeName(robotName + "_android_sensors_driver_imu");
            this.imu_pub = new ImuPublisher(mSensorManager, sensorDelay, robotName);
            nodeMainExecutor.execute(this.imu_pub, nodeConfiguration3);

            NodeConfiguration nodeConfiguration4 = NodeConfiguration.newPublic(InetAddressFactory.newNonLoopback().getHostAddress());
            nodeConfiguration4.setMasterUri(masterURI);
            nodeConfiguration4.setNodeName(robotName + "_android_sensors_driver_pressure");
            this.fluid_pressure_pub = new FluidPressurePublisher(mSensorManager, sensorDelay, robotName);
            nodeMainExecutor.execute(this.fluid_pressure_pub, nodeConfiguration4);

            NodeConfiguration nodeConfiguration5 = NodeConfiguration.newPublic(InetAddressFactory.newNonLoopback().getHostAddress());
            nodeConfiguration5.setMasterUri(masterURI);
            nodeConfiguration5.setNodeName(robotName + "_android_sensors_driver_illuminance");
            this.illuminance_pub = new IlluminancePublisher(mSensorManager, sensorDelay, robotName);
            nodeMainExecutor.execute(this.illuminance_pub, nodeConfiguration5);

            NodeConfiguration nodeConfiguration6 = NodeConfiguration.newPublic(InetAddressFactory.newNonLoopback().getHostAddress());
            nodeConfiguration6.setMasterUri(masterURI);
            nodeConfiguration6.setNodeName(robotName + "_android_sensors_driver_temperature");
            this.temperature_pub = new TemperaturePublisher(mSensorManager, sensorDelay, tempSensor, robotName);
            nodeMainExecutor.execute(this.temperature_pub, nodeConfiguration6);
        }
    }

    protected void executeGPS() {
        NodeConfiguration nodeConfiguration2 = NodeConfiguration.newPublic(InetAddressFactory.newNonLoopback().getHostAddress());
        nodeConfiguration2.setMasterUri(masterURI);
        nodeConfiguration2.setNodeName(robotName + "_android_sensors_driver_nav_sat_fix");
        this.fix_pub = new NavSatFixPublisher(mLocationManager, robotName);
        nodeMainExecutor.execute(this.fix_pub, nodeConfiguration2);
    }

    protected void executeCamera() {
        mOpenCvCameraView.enableView();
        NodeConfiguration nodeConfiguration7 = NodeConfiguration.newPublic(InetAddressFactory.newNonLoopback().getHostAddress());
        nodeConfiguration7.setMasterUri(masterURI);
        nodeConfiguration7.setNodeName(robotName + "_android_camera_driver");
        cam_pub.robotName = robotName;
        cam_pub.mainActivity = this;
        nodeMainExecutor.execute(this.cam_pub, nodeConfiguration7);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case 0: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted, yay! Do the
                    executeGPS();
                }
                if (grantResults.length > 1
                        && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted, yay! Do the
                    executeCamera();
                }
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.activity_main, menu);

        SubMenu subPreview = menu.addSubMenu("Color settings");
        subPreview.add(1, VIEW_MODE_RGBA, 0, "RGB color").setChecked(true);
        subPreview.add(1, VIEW_MODE_GRAY, 0, "Gray scale");
        subPreview.add(1, VIEW_MODE_CANNY, 0, "Canny edges");
        subPreview.setGroupCheckable(1, true, true);

        SubMenu subCompression = menu.addSubMenu("Compression");
        subCompression.add(2,IMAGE_TRANSPORT_COMPRESSION_NONE,0,"None");
        SubMenu subPNGCompressionRate = subCompression.addSubMenu(2, IMAGE_TRANSPORT_COMPRESSION_PNG, 0, "Png");
        subPNGCompressionRate.setHeaderTitle("Compression quality");
        subPNGCompressionRate.getItem().setChecked(true);
        subPNGCompressionRate.add(4, 3, 0, "3").setChecked(true);
        subPNGCompressionRate.add(4, 4, 0, "4");
        subPNGCompressionRate.add(4, 5, 0, "5");
        subPNGCompressionRate.add(4, 6, 0, "6");
        subPNGCompressionRate.add(4, 7, 0, "7");
        subPNGCompressionRate.add(4, 8, 0, "8");
        subPNGCompressionRate.add(4, 9, 0, "9");
        subPNGCompressionRate.setGroupCheckable(4, true, true);

        SubMenu subJPEGCompressionRate = subCompression.addSubMenu(2, IMAGE_TRANSPORT_COMPRESSION_JPEG, 0, "Jpeg");
        subCompression.setGroupCheckable(2, true, true);
        subJPEGCompressionRate.setHeaderTitle("Compression quality");
        subJPEGCompressionRate.getItem().setChecked(true);
        subJPEGCompressionRate.add(3, 50, 0, "50");
        subJPEGCompressionRate.add(3, 60, 0, "60");
        subJPEGCompressionRate.add(3, 70, 0, "70");
        subJPEGCompressionRate.add(3, 80, 0, "80").setChecked(true);
        subJPEGCompressionRate.add(3, 90, 0, "90");
        subJPEGCompressionRate.add(3, 100, 0, "100");
        subJPEGCompressionRate.setGroupCheckable(3, true, true);

        if (numberOfCameras > 1) {
            menu.addSubMenu(5, 0, 0, "Swap camera");
        }

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menu_help) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setCancelable(true);
            builder.setTitle(getResources().getString(R.string.help_title));
            builder.setMessage(getResources().getString(R.string.help_message));
            //noinspection deprecation
            builder.setInverseBackgroundForced(true);
            builder.setNegativeButton(getResources().getString(R.string.help_ok),
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    });
            builder.setNeutralButton(getResources().getString(R.string.help_wiki),
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Intent i = new Intent(Intent.ACTION_VIEW);
                            Uri u = Uri.parse("http://www.ros.org/wiki/android_sensors_driver");
                            try {
                                // Start the activity
                                i.setData(u);
                                startActivity(i);
                            } catch (ActivityNotFoundException e) {
                                // Raise on activity not found
                                Toast toast = Toast.makeText(MainActivity.this, "Browser not found.", Toast.LENGTH_SHORT);
                                toast.show();
                            }
                        }
                    });
            builder.setPositiveButton(getResources().getString(R.string.help_report),
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Intent i = new Intent(Intent.ACTION_VIEW);
                            Uri u = Uri.parse("https://github.com/ros-android/android_sensors_driver/issues/new");
                            try {
                                // Start the activity
                                i.setData(u);
                                startActivity(i);
                            } catch (ActivityNotFoundException e) {
                                // Raise on activity not found
                                Toast toast = Toast.makeText(MainActivity.this, "Browser not found.", Toast.LENGTH_SHORT);
                                toast.show();
                            }
                        }
                    });
            AlertDialog alert = builder.create();
            alert.show();

        }
        switch (item.getGroupId()) {
            case 1:
                viewMode = item.getItemId();
                item.setChecked(true);
                break;
            case 2:
                imageCompression = item.getItemId();
                item.setChecked(true);
                break;
            case 3:
                imageJPEGCompressionQuality = item.getItemId();
                item.setChecked(true);
                break;
            case 4:
                imagePNGCompressionQuality = item.getItemId();
                item.setChecked(true);
                break;
            case 5:
                swapCamera();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    //from http://stackoverflow.com/questions/16273370/opencvandroidsdk-switching-between-front-camera-and-back-camera-at-run-time
    public void swapCamera() {
        mCameraId = (mCameraId + 1) % numberOfCameras;
        mOpenCvCameraView.disableView();
        mOpenCvCameraView.setCameraIndex(mCameraId);
        mOpenCvCameraView.enableView();
    }
    public enum eScreenOrientation
    {
        PORTRAIT (ActivityInfo.SCREEN_ORIENTATION_PORTRAIT),
        LANDSCAPE (ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE),
        PORTRAIT_REVERSE (ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT),
        LANDSCAPE_REVERSE (ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE),
        UNSPECIFIED_ORIENTATION (ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);

        public final int activityInfoValue;

        eScreenOrientation ( int orientation )
        {
            activityInfoValue = orientation;
        }
    }

    public  eScreenOrientation getCurrentScreenOrientation()
    {
        final int rotation = ((WindowManager) getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay().getRotation();

        final int orientation = getResources().getConfiguration().orientation;
        switch ( orientation )
        {
            case Configuration.ORIENTATION_PORTRAIT:
                if ( rotation == Surface.ROTATION_0 || rotation == Surface.ROTATION_90 )
                    return eScreenOrientation.PORTRAIT;
                else
                    return eScreenOrientation.PORTRAIT_REVERSE;
            case Configuration.ORIENTATION_LANDSCAPE:
                if ( rotation == Surface.ROTATION_0 || rotation == Surface.ROTATION_90 )
                    return eScreenOrientation.LANDSCAPE;
                else
                    return eScreenOrientation.LANDSCAPE_REVERSE;
            default:
                return eScreenOrientation.UNSPECIFIED_ORIENTATION;
        }
    }
}
