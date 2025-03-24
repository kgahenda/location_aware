package com.cmu.locationaware;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class MainActivity extends AppCompatActivity implements LocationListener
{
    Button mapMeBtn;
    Button textMeBtn;
    LatLonFragment fragment;
    LocationManager locationManager;
    Location currentLocation; // To store the latest location

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        // Initialize UI elements
        mapMeBtn = findViewById(R.id.mapMe); // Ensure IDs match your XML
        textMeBtn = findViewById(R.id.textMe);

        // Set up location manager
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        fragment = new LatLonFragment();
        getSupportFragmentManager().beginTransaction()
           .replace(R.id.latLongFragment, fragment)
           .commit();

        // Check and request location permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)
        {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
        }
        else
        {
            startLocationUpdates();
        }

        // Map Me button to open Google Maps
        mapMeBtn.setOnClickListener(v -> {
            if (currentLocation != null)
            {
                double lat = currentLocation.getLatitude();
                double lon = currentLocation.getLongitude();

                Uri mapUri = Uri.parse("geo:" + lat + "," + lon + "?q=" + lat + "," + lon);
                Intent mapIntent = new Intent(Intent.ACTION_VIEW, mapUri);
                mapIntent.setPackage("com.google.android.apps.maps");
                startActivity(mapIntent);
            }
            else
            {
                Toast.makeText(this, "Location not available yet", Toast.LENGTH_SHORT).show();
            }
        });

        // Ignore Text Me for now as requested
        textMeBtn.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.SEND_SMS) ==
                PackageManager.PERMISSION_GRANTED)
                {
                    sendSMS();
                }
                else
                {
                    ActivityCompat.requestPermissions(MainActivity.this,
                            new String[]{
                                    Manifest.permission.SEND_SMS
                            }, 100);
                }
            }
        });

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults, int deviceId)
    {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults, deviceId);

        if (requestCode == 100 && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
        {
            sendSMS();
        }
        else
        {
            Toast.makeText(this, "The app is useless with no permissions really!", Toast.LENGTH_SHORT).show();
        }
    }

    private void sendSMS()
    {
        Toast.makeText(this, "Just triggered the send sms thing!", Toast.LENGTH_SHORT).show();
        String phone = "+250782018008";
        String message = "Hi, your current location is latitude: " + currentLocation.getLatitude() + " longitude: " + currentLocation.getLongitude();

        SmsManager smsManager = SmsManager.getDefault();
        smsManager.sendTextMessage(phone, null, message, null, null);
        Toast.makeText(this, "SMS sent successfully!", Toast.LENGTH_SHORT).show();
    }

    private void startLocationUpdates()
    {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)
        {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 3000, 1, this);

            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 3000, 1, this);

            // Get last known location to display immediately
            Location lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);

            if (lastKnownLocation == null)
            {
                lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            }

            if (lastKnownLocation != null)
            {
                currentLocation = lastKnownLocation;
                fragment.updateLocation(currentLocation.getLatitude(), currentLocation.getLongitude());
            }
        }
    }

//    @Override
//    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults)
//    {
//        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
//        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE)
//        {
//            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
//            {
//                startLocationUpdates();
//            }
//            else
//            {
//                Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show();
//            }
//        }
//    }

    @Override
    public void onLocationChanged(@NonNull Location location)
    {
        currentLocation = location;
        fragment.updateLocation(currentLocation.getLatitude(), currentLocation.getLongitude());
        Toast.makeText(this, "Location updated", Toast.LENGTH_SHORT).show();
    }

    // Required LocationListener methods (empty for now)
    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {}
    @Override
    public void onProviderEnabled(@NonNull String provider) {}
    @Override
    public void onProviderDisabled(@NonNull String provider) {}
}