package com.javmarina.turtlebot.node;

import android.graphics.Bitmap;

import androidx.annotation.Nullable;

import com.javmarina.turtlebot.cv_bridge.CvImage;

import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.ros.namespace.GraphName;
import org.ros.node.ConnectedNode;
import org.ros.node.Node;
import org.ros.node.NodeMain;
import org.ros.node.topic.Subscriber;

import sensor_msgs.Image;


public class CameraNode implements NodeMain {

    @Nullable
    private ConnectedNode connectedNode;
    private final CameraNode.Callback callback;
    private Subscriber<Image> subscriber;

    public CameraNode(final CameraNode.Callback callback) {
        this.callback = callback;
    }

    public void setTopic(final String topicName) {
        if (connectedNode != null) {
            if (subscriber != null) {
                subscriber.removeAllMessageListeners();
                subscriber.shutdown();
            }
            subscriber = connectedNode.newSubscriber(topicName, Image._TYPE);
            subscriber.addMessageListener(image -> callback.onReceived(bitmapFromImage(image)));
        }
    }

    @Override
    public GraphName getDefaultNodeName() {
        //noinspection HardcodedFileSeparator
        return GraphName.of("android/image");
    }

    @Override
    public void onStart(final ConnectedNode connectedNode) {
        this.connectedNode = connectedNode;
    }

    @Override
    public void onShutdown(final Node node) {
        connectedNode = null;
        // TODO: check this
        subscriber.removeAllMessageListeners();
        subscriber.shutdown();
    }

    @Override
    public void onShutdownComplete(final Node node) {
    }

    @Override
    public void onError(final Node node, final Throwable throwable) {
    }

    public interface Callback {
        void onReceived(final Bitmap bitmap);
    }

    @Nullable
    public static Bitmap bitmapFromImage(final Image message) {
        // 1. Image -> Mat
        final Mat mat;
        try {
            mat = CvImage.toCvCopy(message).image;
        } catch (final Exception e) {
            return null;
        }

        // 2. Mat -> 8UCx Mat
        final Mat mat2;
        final int type = mat.type();
        if (CvType.depth(type) > 0) {
            mat2 = new Mat();
            mat.convertTo(mat2, CvType.CV_8UC(CvType.channels(type)), 1/16.0); // assumes 12 significant bits
        } else {
            mat2 = mat;
        }

        // 3. 8UC3 Mat -> Bitmap
        final int width = message.getWidth();
        final int height = message.getHeight();
        final Bitmap bitmap = Bitmap.createBitmap(
                width,
                height,
                Bitmap.Config.ARGB_8888
        );
        Utils.matToBitmap(mat2, bitmap);

        return bitmap;
    }
}
