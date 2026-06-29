package com.drcardio.splashactivity;


import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.Button;


import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;



public class PermissionActivity extends AppCompatActivity {


    private static final int LOCATION_REQUEST = 100;



    @Override
    protected void onCreate(Bundle savedInstanceState) {


        super.onCreate(savedInstanceState);


        setContentView(R.layout.activity_permission);



        Button allowButton =
                findViewById(R.id.allowButton);



        allowButton.setOnClickListener(v -> {


            checkPermission();


        });


    }



    private void checkPermission(){


        if(ContextCompat.checkSelfPermission(

                this,

                Manifest.permission.ACCESS_FINE_LOCATION

        ) != PackageManager.PERMISSION_GRANTED){



            ActivityCompat.requestPermissions(

                    this,

                    new String[]{
                            Manifest.permission.ACCESS_FINE_LOCATION
                    },

                    LOCATION_REQUEST

            );


        }

        else{


            openMainActivity();


        }


    }




    @Override
    public void onRequestPermissionsResult(

            int requestCode,

            String[] permissions,

            int[] grantResults

    ){


        super.onRequestPermissionsResult(
                requestCode,
                permissions,
                grantResults
        );



        if(requestCode == LOCATION_REQUEST){


            if(grantResults.length > 0 &&

                    grantResults[0]
                            ==
                            PackageManager.PERMISSION_GRANTED){


                openMainActivity();


            }


        }


    }



    private void openMainActivity(){


        Intent intent =
                new Intent(
                        PermissionActivity.this,
                        MainActivity.class
                );


        startActivity(intent);


        finish();


    }


}