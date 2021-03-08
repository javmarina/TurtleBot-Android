package com.javmarina.turtlebot;

import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.javmarina.turtlebot.node.CameraNode;
import com.javmarina.turtlebot.node.TeleopNode;

import org.jboss.netty.buffer.ChannelBuffer;
import org.ros.address.InetAddressFactory;
import org.ros.android.MasterChooser;
import org.ros.android.NodeMainExecutorService;
import org.ros.exception.RosRuntimeException;
import org.ros.node.NodeConfiguration;
import org.ros.node.NodeMain;
import org.ros.node.NodeMainExecutor;

import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;


public final class MainActivity extends AppCompatActivity {

    private static final int MASTER_CHOOSER_REQUEST_CODE = 0;

    private ServiceConnection nodeMainExecutorServiceConnection;
    private NodeMainExecutorService nodeMainExecutorService;

    //private TextView odometryTextView;
    private CustomSeekBar seekBarLinear;
    private CustomSeekBar seekBarAngular;
    private ImageView imageView;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //odometryTextView = findViewById(R.id.odometry);
        final TextView textViewLinear = findViewById(R.id.linear_text);
        final TextView textViewAngular = findViewById(R.id.angular_text);

        seekBarLinear = findViewById(R.id.linear);
        seekBarLinear.init(progress ->
                textViewLinear.setText(String.format(
                        Locale.US, "%.2f", seekBarLinear.getNormalizedProgress())));

        seekBarAngular = findViewById(R.id.angular);
        seekBarAngular.init(progress ->
                textViewAngular.setText(String.format(
                        Locale.US, "%.2f", seekBarAngular.getNormalizedProgress())));

        imageView = findViewById(R.id.imageView);

        nodeMainExecutorServiceConnection = new NodeMainExecutorServiceConnection(null);
    }

    @Override
    protected void onStart() {
        super.onStart();

        final Intent intent = new Intent(this, NodeMainExecutorService.class);
        intent.setAction(NodeMainExecutorService.ACTION_START);
        intent.putExtra(NodeMainExecutorService.EXTRA_NOTIFICATION_TICKER, getString(R.string.app_name));
        intent.putExtra(NodeMainExecutorService.EXTRA_NOTIFICATION_TITLE, getString(R.string.app_name));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent);
        } else {
            startService(intent);
        }
        if (!bindService(intent, nodeMainExecutorServiceConnection, BIND_AUTO_CREATE)) {
            Toast.makeText(this, "Failed to bind NodeMainExecutorService.", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(nodeMainExecutorServiceConnection);
        final Intent intent = new Intent(this, NodeMainExecutorService.class);
        stopService(intent);
    }

    @Override
    protected void onActivityResult(final int requestCode, final int resultCode, @Nullable final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            if (requestCode == MASTER_CHOOSER_REQUEST_CODE) {
                final String host;
                final String networkInterfaceName = data.getStringExtra("ROS_MASTER_NETWORK_INTERFACE");
                // Handles the default selection and prevents possible errors
                if (TextUtils.isEmpty(networkInterfaceName)) {
                    host = InetAddressFactory.newNonLoopback().getHostAddress();
                } else {
                    try {
                        final NetworkInterface networkInterface = NetworkInterface.getByName(networkInterfaceName);
                        host = InetAddressFactory.newNonLoopbackForNetworkInterface(networkInterface).getHostAddress();
                    } catch (final SocketException e) {
                        throw new RosRuntimeException(e);
                    }
                }
                nodeMainExecutorService.setRosHostname(host);
                if (data.getBooleanExtra("ROS_MASTER_CREATE_NEW", false)) {
                    nodeMainExecutorService.startMaster(data.getBooleanExtra("ROS_MASTER_PRIVATE", true));
                } else {
                    final URI uri;
                    try {
                        uri = new URI(data.getStringExtra("ROS_MASTER_URI"));
                    } catch (final URISyntaxException e) {
                        throw new RosRuntimeException(e);
                    }
                    nodeMainExecutorService.setMasterUri(uri);
                }
                // Run init() in a new thread as a convenience since it often requires network access.
                new Thread(() -> init(nodeMainExecutorService)).start();
            } else {
                // Without a master URI configured, we are in an unusable state.
                nodeMainExecutorService.forceShutdown();
            }
        }
    }

    protected void init(final NodeMainExecutor nodeMainExecutor) {
        // ROS Nodes
        final NodeMain teleopNode = new TeleopNode(emptyTwist -> {
            final double linear = seekBarLinear.getNormalizedProgress();
            final double angular = seekBarAngular.getNormalizedProgress();
            emptyTwist.getAngular().setZ(angular * 0.5);
            emptyTwist.getLinear().setX(linear * 0.5);
            return emptyTwist;
        });
        //noinspection HardcodedFileSeparator
        final NodeMain cameraNode = new CameraNode("camera/image", image -> {
            final ChannelBuffer buffer = image.getData();
            final Bitmap bitmap = BitmapFactory.decodeByteArray(
                    buffer.array(),
                    buffer.arrayOffset(),
                    buffer.readableBytes()
            );
            imageView.post(() -> imageView.setImageBitmap(bitmap));
        });

        // Network configuration with ROS master
        //final String hostname = nodeMainExecutorService.getRosHostname();
        final NodeConfiguration nodeConfiguration = NodeConfiguration.newPublic(
                InetAddressFactory.newNonLoopback().getHostAddress()
        );
        nodeConfiguration.setMasterUri(nodeMainExecutorService.getMasterUri());

        // Run nodes
        nodeMainExecutor.execute(teleopNode, nodeConfiguration);
        nodeMainExecutor.execute(cameraNode, nodeConfiguration);
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        final MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.activity_main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        // Handle item selection
        if (item.getItemId() == R.id.item_settings) {
            startActivityForResult(
                    new Intent(this, MasterChooser.class),
                    MASTER_CHOOSER_REQUEST_CODE
            );
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("NonStaticInnerClassInSecureContext")
    private final class NodeMainExecutorServiceConnection implements ServiceConnection {

        private final URI customMasterUri;

        public NodeMainExecutorServiceConnection(final URI customUri) {
            customMasterUri = customUri;
        }

        @Override
        public void onServiceConnected(final ComponentName name, final IBinder binder) {
            nodeMainExecutorService = ((NodeMainExecutorService.LocalBinder) binder).getService();

            if (customMasterUri != null) {
                nodeMainExecutorService.setMasterUri(customMasterUri);
                final String host = InetAddressFactory.newNonLoopback().getHostAddress();
                nodeMainExecutorService.setRosHostname(host);
            }
            nodeMainExecutorService.addListener(executorService -> {
                // We may have added multiple shutdown listeners and we only want to
                // call finish() once.
                if (!isFinishing()) {
                    finish();
                }
            });
            if (nodeMainExecutorService.getMasterUri() == null) {
                startActivityForResult(
                        new Intent(MainActivity.this, MasterChooser.class),
                        MASTER_CHOOSER_REQUEST_CODE
                );
            } else {
                init(nodeMainExecutorService);
            }
        }

        @Override
        public void onServiceDisconnected(final ComponentName name) {
            Toast.makeText(MainActivity.this, "Service disconnected", Toast.LENGTH_LONG).show();
        }
    }
}
