package ppawelski.walkdisorder;


import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;

public class AccelerometerService extends Service implements SensorEventListener {

    public static final String CHANNEL_ID = "ForegroundServiceChannel";
    private SensorManager sensorManager;
    private Sensor sensor;
    private double[] gravity = {0,0,0};
    private double[] linear_acceleration = {0,0,0};
    private Double[] tempArray = new Double[210];
    private int iterate = 0;
    private boolean pass = false;
    RequestQueue requestQueue;
    PowerManager mgr;
    PowerManager.WakeLock wakeLock;

    private final boolean TRAINING = false;

    private final String trainUrl = "http://156.17.236.106:3000";
    private final String predictUrl = "http://156.17.236.106:5000/check";


    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy){}


    @Override
    public void onSensorChanged(SensorEvent event){
        final double alpha = 0.8;

        gravity[0] = alpha * gravity[0] + (1 - alpha) * event.values[0];
        gravity[1] = alpha * gravity[1] + (1 - alpha) * event.values[1];
        gravity[2] = alpha * gravity[2] + (1 - alpha) * event.values[2];

        linear_acceleration[0] = event.values[0] - gravity[0];
        linear_acceleration[1] = event.values[1] - gravity[1];
        linear_acceleration[2] = event.values[2] - gravity[2];

        Intent localIntent = new Intent("ACCDATA").putExtra("ACCDATAARRAY", linear_acceleration);

        LocalBroadcastManager.getInstance(this).sendBroadcast(localIntent);



        if(TRAINING) {
            JSONObject json = new JSONObject();

            try {
                json.put("date", System.currentTimeMillis());
                json.put("x", linear_acceleration[0]);
                json.put("y", linear_acceleration[1]);
                json.put("z", linear_acceleration[2]);
            } catch (JSONException e) {
                e.printStackTrace();
            }

            postTrainData(trainUrl, json);
        }
        else{
            double abs = Math.abs(linear_acceleration[0]) + Math.abs(linear_acceleration[1]) + Math.abs(linear_acceleration[2]);
            if(abs > 15)
                pass = true;
            if(pass) {
                if (iterate < 209) {
                    tempArray[iterate] = linear_acceleration[0];
                    tempArray[iterate + 1] = linear_acceleration[1];
                    tempArray[iterate + 2] = linear_acceleration[2];
                    iterate += 3;
                } else {
                    JSONObject json = new JSONObject();
                    try {

                        JSONArray jsonArray = new JSONArray(Arrays.asList(tempArray));
                        json.put("data", jsonArray);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    iterate = 0;
                    pass = false;

                    postPredictData(predictUrl, json);
                }
            }
        }

    }


    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String input = intent.getStringExtra("inputExtra");
        createNotificationChannel();
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this,
                0, notificationIntent, 0);

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Foreground Service")
                .setContentText(input).setContentIntent(pendingIntent)
                .build();


        startForeground(1, notification);

        mgr = (PowerManager)this.getSystemService(Context.POWER_SERVICE);
        wakeLock = mgr.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "disorder:internet");

        wakeLock.acquire();

        requestQueue = Volley.newRequestQueue(this);

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        sensorManager.registerListener(this, sensor, 10000);


 //       stopSelf();

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        wakeLock.release();
        sensorManager.unregisterListener(this);

    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "Foreground Service Channel",
                    NotificationManager.IMPORTANCE_DEFAULT
            );

            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(serviceChannel);
        }
    }

    public void postTrainData(String url, JSONObject data){

        JsonObjectRequest obj = new JsonObjectRequest(Request.Method.POST, url,data,
             /*  new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        if(mResultCallback != null){
                            mResultCallback.notifySuccess(response);
                        }
                    }
                }*/ null,
             /*   new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        if(mResultCallback != null){
                            mResultCallback.notifyError(error);
                        }
                    }
                }*/ null
        );
        requestQueue.add(obj);

    }

    public void postPredictData(String url, JSONObject data){

        JsonObjectRequest obj = new JsonObjectRequest(Request.Method.POST, url,data,
               new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        broadcastResponse(response);
                    }
                },
             /*   new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        if(mResultCallback != null){
                            mResultCallback.notifyError(error);
                        }
                    }
                }*/ null
        );
        requestQueue.add(obj);

    }

    public void broadcastResponse(JSONObject response){
        int result = 0;
        try {
            result = response.getInt("result");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        Intent localIntent = new Intent("ACCRESPONSE").putExtra("ACCRESPONSEINT", result);

        LocalBroadcastManager.getInstance(this).sendBroadcast(localIntent);
    }
}