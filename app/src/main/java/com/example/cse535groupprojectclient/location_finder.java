package com.example.cse535groupprojectclient;



import android.annotation.SuppressLint;

import android.app.Activity;
import android.content.Context;

import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;

import androidx.core.app.ActivityCompat;

import java.util.List;
import java.util.Locale;

public class location_finder extends Activity {
    private Context mContext;
    private LocationManager locationManager;
    private Location gps_loc;
    private double longitude;
    private double latitude;
    String userCountry, userAddress;

    public location_finder(Context mContext) {
        this.mContext = mContext;
        locationManager = (LocationManager) mContext.getSystemService(Context.LOCATION_SERVICE);
        get_current_location();
    }



    public Double get_longitude() {
        return longitude;
    }

    public Double get_latitude() {
        return latitude;
    }

    public String get_country(){
        return userCountry;
    }

    public String get_address(){
        return userAddress;
    }

    @SuppressLint("MissingPermission")
    private void get_current_location() {

        gps_loc = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        latitude = gps_loc.getLatitude();
        longitude = gps_loc.getLongitude();

        try {
            Geocoder geocoder = new Geocoder(mContext, Locale.getDefault());
            List<Address> addresses = geocoder.getFromLocation(latitude, longitude, 1);
            if (addresses != null && addresses.size() > 0) {
                userCountry = addresses.get(0).getCountryName();
                userAddress = addresses.get(0).getAddressLine(0);
            }
            else {
                userCountry = "Unknown";
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
