package de.hpi.placerecognizer;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.renderscript.RenderScript;
import android.speech.tts.TextToSpeech;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.ViewFlipper;

import java.util.Locale;
import java.util.concurrent.ExecutionException;

import de.hpi.placerecognizer.ImageClassifier;

public class MainActivity extends AppCompatActivity {

    String rootpath = Environment.getExternalStorageDirectory().getPath()+"/Download/";
    String imgpath = rootpath + "imgs/";

    private static final int PERMISSION_REQ_CODE = 200;
    static final int REQUEST_IMAGE_CAPTURE = 201;
    private static boolean permission_granted = false;
    RenderScript rs = null;

    private TextView textView;
    private ImageView mImageView;

    private GPSTracker gps;
    private ImageClassifier ic = new ImageClassifier();
    private TextToSpeech textToSpeech;


    private boolean hasPermission(String permission) {
        int permissionStatus = ActivityCompat.checkSelfPermission(this, permission);
        return(permissionStatus == PackageManager.PERMISSION_GRANTED);
    }

    private void showCoordinates(View view) {
        gps = new GPSTracker(MainActivity.this);
        if (gps.canGetLocation() && permission_granted) {
            double lng = gps.getLongitude();
            double lat = gps.getLatitude();
            Snackbar.make(view, "Lat: " + lat + "\nLong: " + lng, Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show();
        } else {
            gps.showSettingsAlert();
        }
    }

    private void addImageViewToFlipper(final String pathToImg, final ViewFlipper viewFlipper) {
        ImageView imageView = new ImageView(this);
        imageView.setImageBitmap(BitmapFactory.decodeFile(imgpath + pathToImg));
        viewFlipper.addView(imageView);
        imageView.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                viewFlipper.showNext();
            }
        });
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gpslogger);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        textView = (TextView) findViewById(R.id.text);
        mImageView = (ImageView) findViewById(R.id.image);

        //textToSpeech setup
        textToSpeech=new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if(status != TextToSpeech.ERROR) {
                    textToSpeech.setLanguage(Locale.UK);
                }
            }
        });

        FloatingActionButton camera = (FloatingActionButton) findViewById(R.id.camera);
        FloatingActionButton tts = (FloatingActionButton) findViewById(R.id.tts);

        if (!hasPermission(Manifest.permission.ACCESS_FINE_LOCATION) &&
            !hasPermission(Manifest.permission.ACCESS_COARSE_LOCATION)) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    PERMISSION_REQ_CODE);
        } else if (!hasPermission(Manifest.permission.READ_EXTERNAL_STORAGE) ||
                  (!hasPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE))) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    PERMISSION_REQ_CODE);
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    PERMISSION_REQ_CODE);
        } else {
            permission_granted = true;
        }

        rs = RenderScript.create(this);
        ic.rs = rs;
        ic.new prepareModel().execute(rs);

        camera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dispatchTakePictureIntent();
            }
        });

        tts.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(!textToSpeech.isSpeaking()) {
                    textToSpeech.speak(textView.getText(), 0, null, "");
                }
                else {
                    textToSpeech.stop();
                }
            }
        });
    }

    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            Bundle extras = data.getExtras();
            Bitmap imageBitmap = (Bitmap) extras.get("data");
            mImageView.setImageBitmap(imageBitmap);
            String imageClass = ic.classifyImage(imageBitmap).get_label();
//            textView.setText(imageClass);
            try {
                textView.setText(new GetWiki().execute(imageClass).get().descritpion);
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[],
                                           int[] grantResults)
    {
        switch (requestCode) {
            case PERMISSION_REQ_CODE: {
                if (grantResults.length > 0 &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    permission_granted = true;
                }
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