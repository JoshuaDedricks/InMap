package com.example.instoremap;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcel;
import android.telephony.CellIdentityLte;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import java.util.Stack;


public class MainActivity extends AppCompatActivity implements SensorEventListener{
    private SensorManager pathSensorManager;
    public Sensor pathSensor;
    public Sensor gyroscopeSensor;
    public CellIdentityLte Cell;
    private int STORAGE_PERMISSION_CODE;
    private float[]time_stamp = new float[9];
    float dataInDegree = 0;
    private String cellLocationLatLong;
    private Button DataAcc;
    private int xAxes = 0, yAxes = 0;
    private float entryAngle = 0;


    Stack<Float> stack = new Stack<Float>();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        DataAcc = (Button) findViewById(R.id.DataAc);
        DataAcc.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                openBarcodeScannerActivity();
            }
        });

        if(ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACTIVITY_RECOGNITION) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_MEDIA_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED){
            Log.d("Permission Status", "All Permissions granted");
        }else if(ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this, Manifest.permission.ACCESS_MEDIA_LOCATION) &&
                ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this, Manifest.permission.ACTIVITY_RECOGNITION) &&
                ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) &&
                ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this, Manifest.permission.READ_PHONE_STATE)){
            Log.d("User should fix this", "They really should");
        }else{
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.ACCESS_MEDIA_LOCATION,
                    Manifest.permission.ACTIVITY_RECOGNITION,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.READ_PHONE_STATE
            }, STORAGE_PERMISSION_CODE);
        }

        pathSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        pathSensor = pathSensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR);
        gyroscopeSensor = pathSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);



    }

    private void openBarcodeScannerActivity() {
        Intent intent = new Intent(this, BarcodeScanner.class);
        startActivity(intent);
    }

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

    @Override
    protected void onResume(){
        super.onResume();
        pathSensorManager.registerListener(this, pathSensor, SensorManager.SENSOR_DELAY_FASTEST);
        pathSensorManager.registerListener(this, gyroscopeSensor, SensorManager.SENSOR_DELAY_FASTEST);
    }
    @Override
    public void onSensorChanged(SensorEvent event) {

        if(event.sensor == pathSensor) {
            float angle = stack.peek();

            if (angle > 0) {
                angle = 360 - angle;
            } else {
                angle = -1 * angle;
            }
            //angle = angle + entryAngle;

            //entryAngle = angle;

            if (angle >= 0) {
                if (angle <= 8) {
                    yAxes = yAxes + 1;
                }

            }
            if (angle >= 351) {
                if (angle <= 360) {
                    yAxes = yAxes + 1;
                }
            }
            if (angle >= 8.01){
                if(angle <= 80.0 ) {
                    xAxes = xAxes + 1;
                    yAxes = yAxes + 1;
                }
            }

            if (angle >= 80.01){
                if(angle <= 109.0)
                    xAxes = xAxes + 1;
            }
            if (angle >= 109.01){
                if(angle <= 172.0) {
                    xAxes = xAxes + 1;
                    yAxes = yAxes - 1;
                }
            }
            if (angle >= 278.01){
                if(angle <= 350.99) {
                    xAxes = xAxes - 1;
                    yAxes = yAxes + 1;
                }
            }
            if (angle >= 172.01) {
                if (angle <= 188.00) {
                    yAxes = yAxes - 1;
                }
            }
            if (angle >= 188.01){
                if(angle <= 260.00) {
                    xAxes = xAxes - 1;
                    yAxes = yAxes - 1;
                }
            }
            if (angle >= 260.01){
                if(angle <= 278.00) {
                    xAxes = xAxes - 1;
                }
            }

            Log.d("Axes", String.valueOf(xAxes) + " and " + String.valueOf(yAxes));
        }

        if(event.sensor == gyroscopeSensor) {
            // when bearing changes
            dataInDegree = degreePerSecond(event.values[0]);
            firstInLastOut(dataInDegree, stack);
            Log.d("Angle", String.valueOf(stack));
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        pathSensorManager.unregisterListener(this, pathSensor);
        pathSensorManager.unregisterListener(this, gyroscopeSensor);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    public float degreePerSecond(float rps){
        rps = (float) (rps * (180/3.142)); // converts from radianspersecond to degreespersecond
        return rps;
    }

    public void firstInLastOut (float item, Stack<Float> stack ){
        if(stack.size() > 8)
            stack.removeElementAt(0); // Restrict the size of the stack to 8 values
        stack.push(item);
    }



}


