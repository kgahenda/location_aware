package com.cmu.locationaware;

import android.location.LocationManager;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class LatLonFragment extends Fragment
{
    TextView latitude;
    TextView longitude;

    public LatLonFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState)
    {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_lat_lon, container, false);

        // Initialize TextViews
        latitude = view.findViewById(R.id.latitude);
        longitude = view.findViewById(R.id.longitude);

        // Set initial values
        latitude.setText("Lat: N/A");
        longitude.setText("Long: N/A");

        return view;
    }

    public void updateLocation(Double lat, Double lon)
    {
        if (lat != null && lon != null)
        {
            if (latitude != null && longitude != null) {
                latitude.setText("Lat: " + lat);
                longitude.setText("Long: " + lon);
            } else {
                Log.d("LocationFragment", "TextViews are null in updateLocation");
            }
        }
    }
}