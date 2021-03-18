package com.javmarina.turtlebot.node;

import android.graphics.Bitmap;
import android.graphics.Color;

import androidx.annotation.Nullable;

import org.jboss.netty.buffer.ChannelBuffer;
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
        if (!"rgb8".equals(message.getEncoding())) {
            return null;
        }
        final int width = message.getWidth();
        final int height = message.getHeight();
        final Bitmap bitmap = Bitmap.createBitmap(
                width,
                height,
                Bitmap.Config.ARGB_8888
        );
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                final ChannelBuffer data = message.getData();
                final byte red = data.getByte((int) (y * message.getStep() + 3 * x));
                final byte green = data.getByte((int) (y * message.getStep() + 3 * x + 1));
                final byte blue = data.getByte((int) (y * message.getStep() + 3 * x + 2));
                bitmap.setPixel(x, y, Color.argb(255, red & 0xFF, green & 0xFF, blue & 0xFF));
            }
        }
        return bitmap;
    }
}
