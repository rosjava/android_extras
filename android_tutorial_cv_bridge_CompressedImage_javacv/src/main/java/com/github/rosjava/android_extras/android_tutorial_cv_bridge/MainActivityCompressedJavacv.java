package com.github.rosjava.android_extras.android_tutorial_cv_bridge;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.WindowManager;
import android.widget.ImageView;

import org.bytedeco.javacpp.opencv_core;
import org.bytedeco.javacpp.opencv_imgproc;
import org.bytedeco.javacpp.opencv_core.Point;
import org.bytedeco.javacpp.opencv_core.Scalar;
import org.ros.android.RosActivity;
import org.ros.message.MessageListener;
import org.ros.namespace.GraphName;
import org.ros.node.ConnectedNode;
import org.ros.node.Node;
import org.ros.node.NodeConfiguration;
import org.ros.node.NodeMain;
import org.ros.node.NodeMainExecutor;
import org.ros.node.topic.Publisher;
import org.ros.node.topic.Subscriber;

import cv_bridge.CvImage;
import cv_bridge.Format;
import sensor_msgs.CompressedImage;
import sensor_msgs.ImageEncodings;


/**
 *
 * @author tal.regev@gmail.com (Tal Regev)
 *
 */
@SuppressWarnings("WeakerAccess")
public class MainActivityCompressedJavacv extends RosActivity implements NodeMain{

    protected Publisher<CompressedImage> imagePublisher;
    protected Subscriber<CompressedImage> imageSubscriber;
    protected ConnectedNode node;
    protected static final String TAG = "compressed Tutorial";
    protected Bitmap bmp;
    protected ImageView imageView;
    protected Runnable displayImage;


    public MainActivityCompressedJavacv() {
        // The RosActivity constructor configures the notification title and ticker
        // messages.
        super("compressed_image Tutorial", "compressed_image Tutorial");
    }

    @SuppressWarnings("unchecked")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.main);
        imageView = (ImageView) findViewById(R.id.imageView);
        displayImage = new Runnable() {
            @Override
            public void run() {
                // This code will always run on the UI thread, therefore is safe to modify UI elements.
                imageView.setImageBitmap(bmp);
            }
        };
    }

    @Override
    protected void init(NodeMainExecutor nodeMainExecutor) {
        // At this point, the user has already been prompted to either enter the URI
        // of a master to use or to start a master locally.

        // The user can easily use the selected ROS Hostname in the master chooser
        // activity.
        NodeConfiguration nodeConfiguration = NodeConfiguration.newPublic(getRosHostname());
        nodeConfiguration.setMasterUri(getMasterUri());
        nodeMainExecutor.execute(this, nodeConfiguration);
        onResume();
    }

    @Override
    public GraphName getDefaultNodeName() {
        return GraphName.of("android_tutorial_cv_bridge");
    }


    @Override
    public void onStart(ConnectedNode connectedNode) {
        this.node = connectedNode;
        final org.apache.commons.logging.Log log = node.getLog();
        imagePublisher = node.newPublisher("/image_converter/output_video/compressed", CompressedImage._TYPE);
        imageSubscriber = node.newSubscriber("/camera/image/compressed", CompressedImage._TYPE);
        imageSubscriber.addMessageListener(new MessageListener<CompressedImage>() {
            @Override
            public void onNewMessage(CompressedImage message) {
                CvImage cvImage;
                try {
                    cvImage = CvImage.toCvCopy(message, ImageEncodings.RGBA8);
                } catch (Exception e) {
                    log.error("cv_bridge exception: " + e.getMessage());
                    return;
                }

                //make sure the picture is big enough for my circle.
                if (cvImage.image.rows() > 110 && cvImage.image.cols() > 110) {
                    //place the circle in the middle of the picture with radius 100 and color red.
                    opencv_imgproc.circle(cvImage.image, new Point(cvImage.image.cols() / 2, cvImage.image.rows() / 2), 100, new Scalar(255, 0, 0,0));
                }

                cvImage.image = cvImage.image.t().asMat();
                opencv_core.flip(cvImage.image, cvImage.image, 1);

                //from https://code.google.com/p/javacv/issues/detail?id=67
                bmp = Bitmap.createBitmap(cvImage.image.cols(), cvImage.image.rows(), Bitmap.Config.ARGB_8888);
                bmp.copyPixelsFromBuffer(cvImage.image.createBuffer());
                runOnUiThread(displayImage);

                opencv_core.flip(cvImage.image, cvImage.image, 1);
                cvImage.image = cvImage.image.t().asMat();

                try {
                    imagePublisher.publish(cvImage.toCompressedImageMsg(imagePublisher.newMessage(), Format.JPG));
                } catch (Exception e) {
                    log.error("cv_bridge exception: " + e.getMessage());
                }
            }
        });
        Log.i(TAG, "called onStart");
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onShutdown(Node node) {
    }

    @Override
    public void onShutdownComplete(Node node) {
    }

    @Override
    public void onError(Node node, Throwable throwable) {
    }
}
