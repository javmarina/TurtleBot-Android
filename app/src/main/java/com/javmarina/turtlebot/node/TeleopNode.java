package com.javmarina.turtlebot.node;

import org.ros.concurrent.CancellableLoop;
import org.ros.concurrent.Rate;
import org.ros.concurrent.WallTimeRate;
import org.ros.namespace.GraphName;
import org.ros.node.ConnectedNode;
import org.ros.node.Node;
import org.ros.node.NodeMain;
import org.ros.node.topic.Publisher;

import geometry_msgs.Twist;


public class TeleopNode implements NodeMain {

    private final TeleopNode.TwistProvider twistProvider;
    private Publisher<Twist> teleopPublisher;

    public TeleopNode(final TeleopNode.TwistProvider twistProvider) {
        this.twistProvider = twistProvider;
    }

    @Override
    public GraphName getDefaultNodeName() {
        //noinspection HardcodedFileSeparator
        return GraphName.of("android/teleop");
    }

    @Override
    public void onStart(final ConnectedNode connectedNode) {
        //noinspection HardcodedFileSeparator
        teleopPublisher = connectedNode.newPublisher("/turtle1/cmd_vel", Twist._TYPE);
        connectedNode.executeCancellableLoop(new TeleopNode.TeleopLoop(teleopPublisher, twistProvider));
    }

    @Override
    public void onShutdown(final Node node) {
        teleopPublisher.shutdown();
    }

    @Override
    public void onShutdownComplete(final Node node) {
    }

    @Override
    public void onError(final Node node, final Throwable throwable) {
    }

    public interface TwistProvider {
        Twist getTwist(final Twist emptyTwist);
    }

    private static final class TeleopLoop extends CancellableLoop {

        private final Publisher<Twist> twistPublisher;
        private final TeleopNode.TwistProvider twistProvider;
        private final Rate rate = new WallTimeRate(10); // TODO

        TeleopLoop(final Publisher<Twist> twistPublisher,
                   final TeleopNode.TwistProvider twistProvider) {
            this.twistPublisher = twistPublisher;
            this.twistProvider = twistProvider;
        }

        @Override
        protected void loop() {
            final Twist twist = twistProvider.getTwist(twistPublisher.newMessage());
            twistPublisher.publish(twist);
            rate.sleep();
        }
    }
}
