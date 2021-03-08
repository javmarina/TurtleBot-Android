package com.javmarina.turtlebot.node;

import org.ros.namespace.GraphName;
import org.ros.node.ConnectedNode;
import org.ros.node.Node;
import org.ros.node.NodeMain;
import org.ros.node.topic.Subscriber;

import sensor_msgs.Image;


public class CameraNode implements NodeMain {

    private final String topicName;
    private final CameraNode.Callback callback;
    private Subscriber<Image> subscriber;

    public CameraNode(final String topicName, final CameraNode.Callback callback) {
        this.topicName = topicName;
        this.callback = callback;
    }

    @Override
    public GraphName getDefaultNodeName() {
        //noinspection HardcodedFileSeparator
        return GraphName.of("android/image");
    }

    @Override
    public void onStart(final ConnectedNode connectedNode) {
        subscriber = connectedNode.newSubscriber(topicName, Image._TYPE);
        subscriber.addMessageListener(callback::onReceived);
    }

    @Override
    public void onShutdown(final Node node) {
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
        void onReceived(final Image image);
    }
}
