package com.example.locationapi;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.hardware.SensorManager;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.Looper;
import android.provider.Settings;
import android.telephony.SmsManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.common.api.Result;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.text.DateFormat;
import java.util.Date;

public class MainActivity extends AppCompatActivity implements ShakeDetector.Listener {
    private static final int MY_PERMISSIONS_REQUEST_SEND_SMS =0 ;


    public static final int checksettingscode = 111;
    private static final int REQUEST_LOCATION_PERMISSION = 222;
    private FusedLocationProviderClient fusedLocationClient;
    private SettingsClient settingsClient;
    private LocationRequest locationRequest;
    private LocationSettingsRequest locationSettingsRequest;
    private LocationCallback locationCallback;
    private Location currentlocation;
    private boolean isLocationUpdatesActive;


 private TextView coordinates;
 private  EditText phonenumber;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SensorManager sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        ShakeDetector sd = new ShakeDetector(this);
        sd.start(sensorManager);
        setContentView(R.layout.smsnaw);
        phonenumber = findViewById(R.id.edphone);
        coordinates =  findViewById(R.id.tvcoord);
               fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        settingsClient = LocationServices.getSettingsClient(this);
        buildLocationRequest();
        buildLocationCallBack();
        bulidLocationSettingsRequest();

    }

    private void bulidLocationSettingsRequest() {
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder().addLocationRequest(locationRequest);
        locationSettingsRequest = builder.build();
    }

    private void buildLocationCallBack() {
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                super.onLocationResult(locationResult);
                currentlocation = locationResult.getLastLocation();
                updateLocationUi();

            }
        };
    }

    private void updateLocationUi() {
        if(currentlocation!=null){
        coordinates.setText("Широта: " + currentlocation.getLatitude() + "/"+"Долгота: " + currentlocation.getLongitude());

    }
    }

    private void buildLocationRequest() {
        locationRequest = new LocationRequest();
        locationRequest.setInterval(5000);
        locationRequest.setFastestInterval(3000);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

    }

    public void start(View view) {
        startLocationUpdates();

    }


    private void startLocationUpdates() {
        isLocationUpdatesActive = true;

        settingsClient.checkLocationSettings(locationSettingsRequest)
                .addOnSuccessListener(this, new OnSuccessListener<LocationSettingsResponse>() {
                    @Override
                    public void onSuccess(LocationSettingsResponse locationSettingsResponse) {
                        if (ActivityCompat.checkSelfPermission(MainActivity.this,
                                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                                ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) !=
                                        PackageManager.PERMISSION_GRANTED) {
                            // TODO: Consider calling
                            //    ActivityCompat#requestPermissions
                            // here to request the missing permissions, and then overriding
                            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                            //                                          int[] grantResults)
                            // to handle the case where the user grants the permission. See the documentation
                            // for ActivityCompat#requestPermissions for more details.
                            return;
                        }
                        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.myLooper());
                        updateLocationUi();
                    }
                }).addOnFailureListener(this, new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                int statuscode = ((ApiException) e).getStatusCode();
                switch (statuscode){
                    case LocationSettingsStatusCodes
                            .RESOLUTION_REQUIRED:
                        try {
                            ResolvableApiException resolvableApiException = (ResolvableApiException) e;
                            resolvableApiException.startResolutionForResult(MainActivity.this, checksettingscode);
                        }catch (IntentSender.SendIntentException sie){
                            sie.printStackTrace();

                        }break;
                    case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                        String messaga = "настройте настройки локации на телефоне";
                        Toast.makeText(MainActivity.this, messaga, Toast.LENGTH_LONG).show();
                        isLocationUpdatesActive = false;

                }
                updateLocationUi();
            }
        });

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case checksettingscode:
                switch (resultCode) {
                    case Activity
                            .RESULT_OK:
                        Log.d("Mainact ", "пользователь дал добро");
                        startLocationUpdates();
                        break;
                    case Activity.RESULT_CANCELED:
                        Log.d("mainact", "не дал добро");
                        isLocationUpdatesActive = false;

                        updateLocationUi();
                        break;
                }

                break;
        }
    }

    @Override
    protected void onResume() {


        super.onResume();
        if (isLocationUpdatesActive && checkLocationPermissons()) {
            startLocationUpdates();


        } else if (!checkLocationPermissons()) {
            requestLocationPermission();
        }
    }

    private void requestLocationPermission() {
        boolean shouldProvideRationale = ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION);
        if(shouldProvideRationale){
          showSnackbar("разрешение на определние местоположения необходимо для работы приложения", "OK", new View.OnClickListener() {
              @Override
              public void onClick(View view) {
                  ActivityCompat.requestPermissions(MainActivity.this, new String[]{
                           Manifest.permission.ACCESS_FINE_LOCATION
                  }, REQUEST_LOCATION_PERMISSION);
              }
          });

        }else {ActivityCompat.requestPermissions(MainActivity.this, new String[]{
                Manifest.permission.ACCESS_FINE_LOCATION
        }, REQUEST_LOCATION_PERMISSION);

        }
    }

    private void showSnackbar(final String mainText, final String action, View.OnClickListener listener) {
        Snackbar.make(findViewById(android.R.id.content), mainText, Snackbar.LENGTH_INDEFINITE).setAction(action, listener).show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_LOCATION_PERMISSION) {
            if (grantResults.length <= 0) {
                Log.d("permissionresult", "Запрос был отклонен");
            } else if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startLocationUpdates();
            } else {
                showSnackbar("включи геолокацию", "Настройки", new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Intent intent = new Intent();
                        intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                        Uri uri = Uri.fromParts("package", BuildConfig.APPLICATION_ID, null);
                        intent.setData(uri);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent);
                    }
                });
            }
        }
    }

    private boolean checkLocationPermissons() {
        int permissionState = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION);
        return permissionState ==PackageManager.PERMISSION_GRANTED;
    }


    public void stop(View view) {
        stopLocationUpdates();
    }

    private void stopLocationUpdates() {
        if(!isLocationUpdatesActive){
            return;
        }else {
            fusedLocationClient.removeLocationUpdates(locationCallback)
                    .addOnCompleteListener(this, new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            isLocationUpdatesActive = false;

                        }
                    });
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopLocationUpdates();
    }

    @Override
    public void hearShake() {

       String loc = coordinates.getText().toString();
       if(loc.isEmpty()){
          startLocationUpdates();


      }else{

           String phoneNo = phonenumber.getText().toString() ;
           Log.d("locat1", loc );
        sendSMSMessage(phoneNo, loc);
           Log.d("smssender", "Отправлена смс на номер: "+phoneNo+" c координатами: "+loc);
           coordinates.setText(loc);
           Toast.makeText(this, "Отправлена смс на номер: "+phoneNo+" c координатами: "+loc,Toast.LENGTH_LONG).show();
            stopLocationUpdates();
        }


    }

    @Override
    public void onRequestPermissionsResult1(int requestCode, String[] permissions, int[] grantResults) {

    }

    protected void sendSMSMessage(String phoneNo, String loc) {

        SmsManager smsManager = SmsManager.getDefault();
        smsManager.sendTextMessage(phoneNo, null, loc, null, null);

        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.SEND_SMS)
                != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.SEND_SMS)) {
            } else {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.SEND_SMS},
                        MY_PERMISSIONS_REQUEST_SEND_SMS);
            }
        }
    }


    }
