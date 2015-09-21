package gr.unipi.orientationpassenger;

import android.app.Activity;
import android.app.Application;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.ImageView;
import android.widget.TextView;

import java.net.DatagramPacket;
import java.net.DatagramSocket;

public class MainActivity extends Activity {

    float azimuthReference = -1;
    float azimuthLocal = -1;
    TextView view;
    private SensorManager sensorManager;
    private Sensor sensor;
    private ImageView image;
    private float currentDegree = 0f;

    //runs without a timer by reposting this handler at the end of the runnable
    Handler timerHandler = new Handler();
    Runnable timerRunnable = new Runnable() {

        @Override
        public void run() {

            System.out.println("PPPPPPPPP Local: " + azimuthLocal);
            System.out.println("PPPPPPPPP Ref: " + azimuthReference);
            if (azimuthReference != -1 && azimuthLocal != -1) {
                view.setText("Relative Orientation: " + String.valueOf(azimuthLocal - azimuthReference));

                float degree = - (azimuthLocal - azimuthReference);

                // create a rotation animation (reverse turn degree degrees)
                RotateAnimation ra = new RotateAnimation(
                        currentDegree,
                        -degree,
                        Animation.RELATIVE_TO_SELF, 0.5f,
                        Animation.RELATIVE_TO_SELF,
                        0.5f);

                // how long the animation will take place
                ra.setDuration(210);

                // set the animation after the end of the reservation status
                ra.setFillAfter(true);

                // Start the animation
                image.startAnimation(ra);

                currentDegree = -degree;

            } else if (azimuthReference != -1 && azimuthLocal == -1){
                view.setText("Relative Orientation: Unable to get local Azimuth");
            } else if (azimuthLocal != -1 && azimuthReference == -1){
                view.setText("Relative Orientation: Unable to get reference Azimuth");
            } else if (azimuthLocal == -1 && azimuthReference == -1){
                view.setText("Relative Orientation: Unable to get reference Azimuth");
            }

            timerHandler.postDelayed(this, 2000);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        view = (TextView) findViewById(R.id.textView);
        image = (ImageView) findViewById(R.id.imageViewCompass);

        UDPClient udpClient = new UDPClient();
        new Thread(udpClient).start();

        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION);
        sensorManager.registerListener(mySensorEventListener, sensor,
                SensorManager.SENSOR_DELAY_NORMAL);

        timerHandler.postDelayed(timerRunnable, 0);

    }


    private SensorEventListener mySensorEventListener = new SensorEventListener() {

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }

        @Override
        public void onSensorChanged(SensorEvent event) {
            // angle between the magnetic north direction
            // 0=North, 90=East, 180=South, 270=West
            azimuthLocal = event.values[0];
        }
    };

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public class UDPClient extends Application implements Runnable {
        private final static int LISTENING_PORT = 37767;

        @Override
        public void run() {
            try {
                //Opening listening socket
                Log.e("UDP Receiver", "Opening listening socket on port " + LISTENING_PORT + "...");
                DatagramSocket socket = new DatagramSocket(LISTENING_PORT);
                socket.setBroadcast(true);
                socket.setReuseAddress(true);
                Log.e("UDP Receiver", "Listening...");
                while (true) {
                    //Listening on socket
                    byte[] buf = new byte[1024];
                    DatagramPacket packet = new DatagramPacket(buf, buf.length);
                    socket.receive(packet);
                    String message = new String(packet.getData()).trim();
                    Log.e("UDP Receiver", "UDP Packet Received:" + message);
                    message.replaceAll("[\\~\\`\\!\\@\\#\\$\\%\\^\\&\\*\\(\\)\\_\\-\\+\\{\\}\\[\\]\\;\"\\|\\\\,\\.\\/\\<\\>\\?]", "");
                    if (message.contains("azimuth")) {

                        Log.e("UDP", message);

                        String[] input = message.split("=");
                        azimuthReference = Float.parseFloat(input[1]);

                    } else {
                        Log.e("UDP Receiver", "Unknown Button");
                    }
                }
            } catch (Exception e) {
                Log.e("UDP", "Receiver error", e);
            }
        }
    }
}
