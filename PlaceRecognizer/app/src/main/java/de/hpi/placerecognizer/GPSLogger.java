package de.hpi.placerecognizer;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.renderscript.RenderScript;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.webkit.ConsoleMessage;

import network.CNNdroid;

public class GPSLogger extends AppCompatActivity {

    private static final int MY_PERMISSIONS_REQUEST_READ_CONTACTS = 200;
    private static boolean permission_granted = false;
    GPSTracker gps;
    RenderScript rs = null;
    CNNdroid conv = null;//new CNNdroid(rs, "/sdcard/Data_Cifar10/Cifar10_def.txt");

    private boolean hasPermission(String permission) {
        int permissionStatus = ActivityCompat.checkSelfPermission(this, permission);
        return(permissionStatus == PackageManager.PERMISSION_GRANTED);
    }

    private void showCoordinates(View view) {
        gps = new GPSTracker(GPSLogger.this);
        if (gps.canGetLocation() && permission_granted) {
            double lng = gps.getLongitude();
            double lat = gps.getLatitude();
            Snackbar.make(view, "Lat: " + lat + "\nLong: " + lng, Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show();
        } else {
            gps.showSettingsAlert();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gpslogger);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);

        if (!hasPermission(Manifest.permission.ACCESS_FINE_LOCATION) &&
            !hasPermission(Manifest.permission.ACCESS_COARSE_LOCATION)) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    MY_PERMISSIONS_REQUEST_READ_CONTACTS);
        } else if (!hasPermission(Manifest.permission.READ_EXTERNAL_STORAGE) ||
                  (!hasPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE))) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    MY_PERMISSIONS_REQUEST_READ_CONTACTS);
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    MY_PERMISSIONS_REQUEST_READ_CONTACTS);
        } else {
            permission_granted = true;
        }

        rs = RenderScript.create(this);
        new prepareModel().execute(rs);

        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showCoordinates(view);
            }
        });
    }

    private class prepareModel extends AsyncTask<RenderScript, Void, CNNdroid> {
        @Override
        protected CNNdroid doInBackground(RenderScript... params) {
            try {
                conv = new CNNdroid(rs, Environment.getExternalStorageDirectory().getPath() + "/Download/Data_Cifar10/Cifar10_def.txt");
            } catch (Exception e) {
                e.printStackTrace();
            }
            return conv;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[],
                                           int[] grantResults)
    {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_READ_CONTACTS: {
                if (grantResults.length > 0 &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    permission_granted = true;
                }
                return;
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_gpslogger, menu);
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
}
