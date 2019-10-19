package ppawelski.walkdisorder;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;


public class MainActivity extends AppCompatActivity {



    private TextView currentX;
    private TextView currentY;
    private TextView currentZ;
    Button btnStartService, btnStopService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        currentX = findViewById(R.id.X);
        currentY = findViewById(R.id.Y);
        currentZ = findViewById(R.id.Z);

        TextView x = findViewById(R.id.textX);
        TextView y = findViewById(R.id.textY);
        TextView z = findViewById(R.id.textZ);

        x.setText("X:");
        y.setText("Y:");
        z.setText("Z:");

        btnStartService = findViewById(R.id.accstart);
        btnStopService = findViewById(R.id.accstop);

        startAccelerometer();
    }

    private void startAccelerometer(){
//        Intent accelerometerIntent = new Intent(this, AccelerometerService.class);
//        startService(accelerometerIntent);
        //AccelerometerService.enqueueWork(getApplicationContext(), AccelerometerService.class, ACCELEROMETER_SERVICE_ID, accelerometerIntent);

//        btnStartService.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
                startService();
//            }
//        });
/*
        btnStopService.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopService();
            }
        });
*/
        LocalBroadcastManager.getInstance(this).registerReceiver(accDataReceiver, new IntentFilter("ACCDATA"));
    }

    public void displayCurrentValues(double[] linear_acceleration) {
        currentX.setText(String.format("%.2f", linear_acceleration[0]));
        currentY.setText(String.format("%.2f", linear_acceleration[1]));
        currentZ.setText(String.format("%.2f", linear_acceleration[2]));
    }

    private BroadcastReceiver accDataReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            double[] linear_acceleration = intent.getDoubleArrayExtra("ACCDATAARRAY");
            displayCurrentValues(linear_acceleration);
        }
    };

    public void startService(){
        Intent accelerometerIntent = new Intent(this, AccelerometerService.class);
        ContextCompat.startForegroundService(this, accelerometerIntent);
    }

    public void stopService(){
        Intent accelerometerIntent = new Intent(this, AccelerometerService.class);
        stopService(accelerometerIntent);
    }
}

