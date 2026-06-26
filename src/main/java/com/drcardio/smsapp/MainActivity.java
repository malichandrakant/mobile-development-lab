package com.drcardio.smsapp;


import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;


import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;



public class MainActivity extends AppCompatActivity {


    EditText phoneNumber;
    EditText message;

    TextView smsStatus;

    Button sendButton;


    private static final int SMS_PERMISSION_CODE = 100;



    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);


        setContentView(R.layout.activity_main);



        phoneNumber = findViewById(R.id.phoneNumber);

        message = findViewById(R.id.message);

        sendButton = findViewById(R.id.sendButton);

        smsStatus = findViewById(R.id.smsStatus);




        checkPermission();




        sendButton.setOnClickListener(v -> {


            sendSMS();


        });



    }




    private void checkPermission(){


        if(ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.SEND_SMS
        )
                != PackageManager.PERMISSION_GRANTED){



            ActivityCompat.requestPermissions(

                    this,

                    new String[]{

                            Manifest.permission.SEND_SMS,
                            Manifest.permission.RECEIVE_SMS,
                            Manifest.permission.READ_SMS

                    },

                    SMS_PERMISSION_CODE

            );

        }


    }





    private void sendSMS(){



        String phone =
                phoneNumber.getText()
                        .toString()
                        .trim();



        String msg =
                message.getText()
                        .toString()
                        .trim();




        if(phone.isEmpty()){


            Toast.makeText(
                    this,
                    "Enter phone number",
                    Toast.LENGTH_SHORT
            ).show();


            return;

        }



        if(msg.isEmpty()){


            Toast.makeText(
                    this,
                    "Enter message",
                    Toast.LENGTH_SHORT
            ).show();


            return;

        }





        if(ActivityCompat.checkSelfPermission(

                this,

                Manifest.permission.SEND_SMS

        )
                != PackageManager.PERMISSION_GRANTED){


            Toast.makeText(

                    this,

                    "SMS permission not granted",

                    Toast.LENGTH_SHORT

            ).show();


            checkPermission();

            return;


        }




        SmsManager smsManager =
                SmsManager.getDefault();





        smsManager.sendTextMessage(

                phone,

                null,

                msg,

                null,

                null

        );





        smsStatus.append(

                "\n\nSent To: "
                        + phone
                        + "\nMessage: "
                        + msg

        );





        Toast.makeText(

                this,

                "SMS Sent Successfully",

                Toast.LENGTH_SHORT

        ).show();



    }


}