package com.cmu.locationaware;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Looper;

public class MainActivity extends AppCompatActivity {
    Button mapMeBtn;
    Button textMeBtn;
    LatLonFragment fragment;
    Location currentLocation; // To store the latest location
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 100;
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private static final String ACTION_SMS_SENT = "LOCATION_AWARE_APP_SMS_SENT";
    private static final String ACTION_SMS_DELIVERED = "LOCATION_AWARE_APP_SMS_DELIVERED";

    private BroadcastReceiver broadcastReceiverForSent = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int resultCode = getResultCode();
            if (resultCode == Activity.RESULT_OK) {
                Toast.makeText(context, "SMS sent successfully!", Toast.LENGTH_SHORT).show();
            } else if (resultCode == SmsManager.RESULT_ERROR_GENERIC_FAILURE) {
                Toast.makeText(context, "SMS failed: Generic failure", Toast.LENGTH_SHORT).show();
            } else if (resultCode == SmsManager.RESULT_ERROR_NO_SERVICE) {
                Toast.makeText(context, "SMS failed: No service", Toast.LENGTH_SHORT).show();
            } else if (resultCode == SmsManager.RESULT_ERROR_NULL_PDU) {
                Toast.makeText(context, "SMS failed: Null PDU", Toast.LENGTH_SHORT).show();
            } else if (resultCode == SmsManager.RESULT_ERROR_RADIO_OFF) {
                Toast.makeText(context, "SMS failed: Radio off", Toast.LENGTH_SHORT).show();
            }
            // Unregister the receiver here as per requirement
            context.unregisterReceiver(this);
        }
    };

    private BroadcastReceiver broadcastReceiverDelivered = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int resultCode = getResultCode();
            if (resultCode == Activity.RESULT_OK) {
                Toast.makeText(context, "SMS delivered successfully!", Toast.LENGTH_SHORT).show();
            } else if (resultCode == Activity.RESULT_CANCELED) {
                Toast.makeText(context, "SMS not delivered", Toast.LENGTH_SHORT).show();
            }
            // Unregister the receiver here as per requirement
            context.unregisterReceiver(this);
        }
    };

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        // Initialize UI elements
        mapMeBtn = findViewById(R.id.mapMe);
        textMeBtn = findViewById(R.id.textMe);

        // Initialize FusedLocationProviderClient
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Set up location callback
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) {
                    return;
                }
                for (Location location : locationResult.getLocations()) {
                    currentLocation = location;
                    fragment.updateLocation(location.getLatitude(), location.getLongitude());
                    //Toast.makeText(MainActivity.this, "Location updated", Toast.LENGTH_SHORT).show();
                }
            }
        };

        // Initialize the fragment
        fragment = new LatLonFragment();
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.latLongFragment, fragment)
                .commit();

        // Restore location from saved state if available
        if (savedInstanceState != null && savedInstanceState.containsKey("latitude")) {
            double lat = savedInstanceState.getDouble("latitude");
            double lon = savedInstanceState.getDouble("longitude");
            currentLocation = new Location("restored");
            currentLocation.setLatitude(lat);
            currentLocation.setLongitude(lon);
            fragment.updateLocation(lat, lon);
        }

        // Check and request location permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            startLocationUpdates();
        }

        // Map Me button to open Google Maps
        mapMeBtn.setOnClickListener(v -> {
            if (currentLocation != null) {
                double lat = currentLocation.getLatitude();
                double lon = currentLocation.getLongitude();

                Uri mapUri = Uri.parse("geo:" + lat + "," + lon + "?q=" + lat + "," + lon);
                Intent mapIntent = new Intent(Intent.ACTION_VIEW, mapUri);
                mapIntent.setPackage("com.google.android.apps.maps");
                startActivity(mapIntent);
            } else {
                Toast.makeText(this, "Location not available yet", Toast.LENGTH_SHORT).show();
            }
        });

        // Text Me button to send SMS
        textMeBtn.setOnClickListener(v -> {
            if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(MainActivity.this,
                        new String[]{Manifest.permission.SEND_SMS}, 100);
            } else {
                sendSMS();
            }
        });

        // Register the sent receiver
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) { // API 26 and above
            registerReceiver(broadcastReceiverForSent, new IntentFilter(ACTION_SMS_SENT), RECEIVER_NOT_EXPORTED);
        } else { // API 24 and 25
            registerReceiver(broadcastReceiverForSent, new IntentFilter(ACTION_SMS_SENT));
        }

        // Register the delivered receiver
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) { // API 26 and above
            registerReceiver(broadcastReceiverDelivered, new IntentFilter(ACTION_SMS_DELIVERED), RECEIVER_NOT_EXPORTED);
        } else { // API 24 and 25
            registerReceiver(broadcastReceiverDelivered, new IntentFilter(ACTION_SMS_DELIVERED));
        }

        // Set up window insets
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    private void sendSMS() {
        try {
            String phone = "+250795002971";
            // Validate phone number
            if (!phone.matches("\\+\\d{10,15}")) {
                throw new IllegalArgumentException("Invalid phone number format. Use E.164 format (e.g., +1234567890)");
            }

            String message = "Hi, your current location is latitude: " +
                    (currentLocation != null ? currentLocation.getLatitude() : "N/A") +
                    " longitude: " + (currentLocation != null ? currentLocation.getLongitude() : "N/A");
            Log.d("SendSMS", "Message: " + message);

            SmsManager smsManager = SmsManager.getDefault();
            Log.d("SendSMS", "SmsManager initialized");

            // Register the sent receiver before sending
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) { // API 26 and above
                registerReceiver(broadcastReceiverForSent, new IntentFilter(ACTION_SMS_SENT), RECEIVER_NOT_EXPORTED);
            } else { // API 24 and 25
                registerReceiver(broadcastReceiverForSent, new IntentFilter(ACTION_SMS_SENT));
            }

            // Register the delivered receiver before sending
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) { // API 26 and above
                registerReceiver(broadcastReceiverDelivered, new IntentFilter(ACTION_SMS_DELIVERED), RECEIVER_NOT_EXPORTED);
            } else { // API 24 and 25
                registerReceiver(broadcastReceiverDelivered, new IntentFilter(ACTION_SMS_DELIVERED));
            }
            Log.d("SendSMS", "Receivers registered");

            PendingIntent sentPI = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_SMS_SENT), PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            PendingIntent deliveredPI = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_SMS_DELIVERED), PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            Log.d("SendSMS", "PendingIntents created");

            smsManager.sendTextMessage(phone, null, message, sentPI, deliveredPI);
            Log.d("SendSMS", "SMS sent");
            Toast.makeText(this, "Attempting to send SMS...", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Log.e("SendSMS", "Error sending SMS: " + e.getMessage(), e);
            Toast.makeText(this, "Failed to send SMS: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            LocationRequest locationRequest = LocationRequest.create();
            locationRequest.setInterval(3000); // Update every 3 seconds
            locationRequest.setFastestInterval(1000);
            locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startLocationUpdates();
            } else {
                Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == 100) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                sendSMS();
            } else {
                Toast.makeText(this, "SMS permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (currentLocation != null) {
            outState.putDouble("latitude", currentLocation.getLatitude());
            outState.putDouble("longitude", currentLocation.getLongitude());
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        fusedLocationClient.removeLocationUpdates(locationCallback);
    }
}