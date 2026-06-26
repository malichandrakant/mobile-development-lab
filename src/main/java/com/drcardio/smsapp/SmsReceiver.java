package com.drcardio.smsapp;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.telephony.SmsMessage;
import android.widget.Toast;



public class SmsReceiver extends BroadcastReceiver {



    @Override
    public void onReceive(
            Context context,
            Intent intent) {



        Bundle bundle =
                intent.getExtras();



        if(bundle != null){



            Object[] pdus =
                    (Object[]) bundle.get("pdus");



            for(Object pdu: pdus){



                SmsMessage sms =
                        SmsMessage.createFromPdu(
                                (byte[])pdu
                        );



                String sender =
                        sms.getOriginatingAddress();



                String message =
                        sms.getMessageBody();



                Toast.makeText(

                        context,

                        "From: "
                                + sender
                                + "\n"
                                + message,

                        Toast.LENGTH_LONG

                ).show();



            }

        }


    }

}