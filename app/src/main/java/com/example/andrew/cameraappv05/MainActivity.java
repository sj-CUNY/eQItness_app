package com.example.andrew.cameraappv05;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Writer;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private static final File TODO = null;
    private static final String TAG = "LoginActivity";
    private static final String URL_FOR_LOGIN = "https://localhost:8888/android_login/login.php";
    ProgressDialog progressDialog;
    private EditText loginInputEmail, loginInputPassword;
    private Button btnlogin;
    private Button btnLinkSignup;
    ImageView result;
    ImageView galleryPic;
    static final int REQUEST_IMAGE_CAPTURE = 1;
    static final int REQUEST_TAKE_PHOTO = 1;
    private Location location;
    double latitude;
    double longitude;
    String hashFileName;
    String timeStamp;
    android.location.Location loc = null;
    String mCurrentPhotoPath;
    String mNewFile;
    String mCurrentHashPath;




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        loginInputEmail = (EditText) findViewById(R.id.login_input_email);
        loginInputPassword = (EditText) findViewById(R.id.login_input_password);
        btnlogin = (Button) findViewById(R.id.btn_login);
        btnLinkSignup = (Button) findViewById(R.id.btn_link_signup);
        //Progress Dialog
        progressDialog = new ProgressDialog(this);
        progressDialog.setCancelable(false);

        btnlogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                loginUser(loginInputEmail.getText().toString(),
                        loginInputPassword.getText().toString());
            }
        });
        btnLinkSignup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(getApplicationContext(), RegisterActivity.class);
                startActivity(i);

            }
        });

        setContentView(R.layout.activity_main);
        Button click = (Button) findViewById(R.id.camera);
        result = (ImageView) findViewById(R.id.imageView);
        galleryPic = (ImageView) findViewById(R.id.imageView2);

        LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 20, 10, locationListener); // possible error here
        List<String> list = lm.getAllProviders();
        for (int i = 0; i < list.size(); i++) {
            loc = lm.getLastKnownLocation(list.get(i));
            if (loc != null) {
                latitude = loc.getLatitude();
                longitude = loc.getLongitude();
                break;

            }
        }


    }

    private void loginUser(final String email, final String password) {
        //Tag used to cancel the request
        String cancel_req_tag = "login";
        progressDialog.setMessage("Logging you in...");
        showDialog();
        StringRequest strReq = new StringRequest(Request.Method.POST,
                URL_FOR_LOGIN, new Response.Listener<String>(){
            @Override
            public void onResponse(String response) {
                Log.d(TAG, "Register Response: " + response.toString());
                hideDialog();
                try{
                    JSONObject jObj = new JSONObject(response);
                    boolean error = jObj.getBoolean("error");

                    if(!error){
                        String user = jObj.getJSONObject("user").getString("name");
                        // Launch user Activity
                        Intent intent = new Intent(
                                String.valueOf(MainActivity.this));
                        intent.putExtra("username", user);
                        startActivity(intent);
                        finish();
                    } else {
                        String errorMsg = jObj.getString("error_msg");
                        Toast.makeText(getApplicationContext(),
                                errorMsg, Toast.LENGTH_LONG).show();
                    }
                } catch (JSONException e){
                    e.printStackTrace();
                }
            }
        }, new Response.ErrorListener(){
                    @Override
            public void onErrorResponse(VolleyError error){
                        Log.e(TAG, "Login Error: " + error.getMessage());
                        Toast.makeText(getApplicationContext(),
                                error.getMessage(), Toast.LENGTH_LONG).show();
                        hideDialog();
                    }
        }) {
            @Override
            protected Map<String, String> getParams() {
                //Posting params to login url
                Map<String, String> params = new HashMap<String, String>();
                params.put("email", email);
                params.put("password", password);
                return params;
            }
        };
        AppSingleton.getInstance(getApplicationContext()).addToRequestQueue(strReq,cancel_req_tag);
    }

    private void showDialog(){
        if(!progressDialog.isShowing())
            progressDialog.show();
    }

    private void hideDialog(){
        if(progressDialog.isShowing())
            progressDialog.dismiss();
    }

    public void dispatchTakePictureIntent(View view) throws Exception {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        //Ensure that there's a camera activity to handle the intent
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            // Create the File where the photo should go
            File photoFile = null;

            try {
                photoFile = createImageFile();
                createHashFile(photoFile);

            } catch (IOException e) {
                e.printStackTrace();
            }
            if (photoFile != null) {
                Uri photoURI = FileProvider.getUriForFile(this,
                        "com.android.fileprovider",
                        photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, REQUEST_TAKE_PHOTO);
            }

        }

    }

    private void galleryAddPic() {
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        File f = new File(mCurrentPhotoPath);
        Uri contentUri = Uri.fromFile(f);
        mediaScanIntent.setData(contentUri);
        this.sendBroadcast(mediaScanIntent);
    }

    private void setPic() {
        //Get dimensions of the View
        int targetW = galleryPic.getWidth();
        int targetH = galleryPic.getHeight();

        //Get the dimensions of the bitmap
        BitmapFactory.Options bmOptions = new BitmapFactory.Options();
        bmOptions.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(mCurrentPhotoPath, bmOptions);
        int photoW = bmOptions.outWidth;
        int photoH = bmOptions.outHeight;

        //Determine how much to scale down the image
        int scaleFactor = Math.min(photoW / targetW, photoH / targetH);

        //Decode the image file into a bitmap sized to fill the View
        bmOptions.inJustDecodeBounds = false;
        bmOptions.inSampleSize = scaleFactor;
        bmOptions.inPurgeable = true;

        Bitmap bitmap = BitmapFactory.decodeFile(mCurrentPhotoPath, bmOptions);
        galleryPic.setImageBitmap(bitmap);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        setPic();
        galleryAddPic();

    }

    // The following block is from http://www.java2s.com/Code/Java/File-Input-Output/ConvertInputStreamtoString.html

    public static String convertStreamtoString(InputStream is) throws Exception {
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        StringBuilder sb = new StringBuilder();
        String line = null;
        while ((line = reader.readLine()) != null) {
            sb.append(line).append("\n");
        }
        reader.close();
        return sb.toString();
    }

    public static String getStringFromFile(String filePath) throws Exception {
        File fl = new File(filePath);
        FileInputStream fin = new FileInputStream(fl);
        String ret = convertStreamtoString(fin);
        //Make sure you close all streams.
        fin.close();
        return ret;
    }


    public static String getSHAEncryptedString(String encTarget) {
        MessageDigest mdEnc = null;
        try {
            mdEnc = MessageDigest.getInstance("SHA");
        } catch (NoSuchAlgorithmException e) {
            System.out.println("Exception while encrypting to md5");
            e.printStackTrace();
        } //Encryption Algorithm
        mdEnc.update(encTarget.getBytes(), 0, encTarget.length());
        String md5 = new BigInteger(1, mdEnc.digest()).toString(16);
        while (md5.length() < 32) {
            md5 = "0" + md5;
        }
        return md5;
    }




    private File createImageFile() throws Exception {
        //Create an image file name
        timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,
                ".jpg",
                storageDir
        );

        mCurrentPhotoPath = image.getAbsolutePath();
        return image;
    }


    public double getLatitude(){
        return latitude;
    }

    public double getLongitude(){
        return longitude;
    }
    LocationListener locationListener =new LocationListener() {


        @Override
        public void onLocationChanged(android.location.Location location) {

            latitude=location.getLatitude();
            longitude=location.getLongitude();
            String msg="New Latitude: "+latitude + "New Longitude: "+longitude;
            Log.d("Error", msg);


        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {

        }

        @Override
        public void onProviderEnabled(String provider) {

        }

        @Override
        public void onProviderDisabled(String provider) {

        }
    };

    public class Location extends AppCompatActivity {
        LocationManager lm;
        Context mContext;
        double latitude;
        double longitude;
        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            // setContentView(R.layout.activity_location);
            mContext = this;

            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return;
            }
            lm.requestLocationUpdates(LocationManager.GPS_PROVIDER,
                    20,
                    10, locationListener);
            isLocationEnabled();

        }






        protected void onResume(){
            super.onResume();
            isLocationEnabled();
        }

        private void isLocationEnabled() {

            if(!lm.isProviderEnabled(LocationManager.GPS_PROVIDER)){
                AlertDialog.Builder alertDialog=new AlertDialog.Builder(mContext);
                alertDialog.setTitle("Enable Location");
                alertDialog.setMessage("Your locations setting is not enabled. Please enabled it in settings menu.");
                alertDialog.setPositiveButton("Location Settings", new DialogInterface.OnClickListener(){
                    public void onClick(DialogInterface dialog, int which){
                        Intent intent=new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                        startActivity(intent);
                    }
                });
                alertDialog.setNegativeButton("Cancel", new DialogInterface.OnClickListener(){
                    public void onClick(DialogInterface dialog, int which){
                        dialog.cancel();
                    }
                });
                AlertDialog alert=alertDialog.create();
                alert.show();
            }
            else{
                AlertDialog.Builder alertDialog=new AlertDialog.Builder(mContext);
                alertDialog.setTitle("Confirm Location");
                alertDialog.setMessage("Your Location is enabled, please enjoy");
                alertDialog.setNegativeButton("Back to interface",new DialogInterface.OnClickListener(){
                    public void onClick(DialogInterface dialog, int which){
                        dialog.cancel();
                    }
                });
                AlertDialog alert=alertDialog.create();
                alert.show();
            }
        }
    }




    private File createHashFile(File input) throws Exception {

        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        mNewFile = getStringFromFile(input.getAbsolutePath());
        hashFileName = getSHAEncryptedString(mNewFile);
        File hash = File.createTempFile(
                hashFileName,
                ".json",
                storageDir
        );


        mCurrentHashPath = hash.getAbsolutePath();
        FileOutputStream fout = new FileOutputStream(hash);
        Writer output = null;
        output = new BufferedWriter(new FileWriter(hash));



        JSONObject json = new JSONObject();
        JSONObject manJson = new JSONObject();

        manJson.put("REQUEST TYPE:", 1);
        manJson.put("HASH_DATA:",hashFileName);
        manJson.put("LATITUDE:", getSHAEncryptedString(Double.toString(getLatitude())));
        manJson.put("LONGITUDE:", getSHAEncryptedString(Double.toString(getLongitude())));
        manJson.put("TIMESTAMP:", timeStamp);
        json.put("USER_ID", manJson);
        output.write(json.toString());
        output.close();

        fout.flush();
        fout.close();




        return hash;
    }




}

