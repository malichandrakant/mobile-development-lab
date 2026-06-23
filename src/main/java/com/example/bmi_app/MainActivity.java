package com.example.bmi_app;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        EditText etWeight,etHeight,etHeight2;
        Button btnCalculate;
        TextView txtResult;

        etWeight=findViewById(R.id.etWeight);
        etHeight=findViewById(R.id.etHeight);
        etHeight2=findViewById(R.id.etHeight2);
        btnCalculate=findViewById(R.id.btnCalculate);
        txtResult=findViewById(R.id.txtResult);


        btnCalculate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if(etWeight.getText().toString().isEmpty() ||
                        etHeight.getText().toString().isEmpty() ||
                        etHeight2.getText().toString().isEmpty()) {

                    txtResult.setText("Please fill all fields");
                    return;
                }

                int wt = Integer.parseInt(etWeight.getText().toString());
                int ft = Integer.parseInt(etHeight.getText().toString());
                int in = Integer.parseInt(etHeight2.getText().toString());

                int totalIn = ft * 12 + in;

                double totalCm = totalIn * 2.54;
                double totalM = totalCm / 100;

                double bmi = wt / (totalM * totalM);

                if(bmi >= 25) {

                    txtResult.setText(String.format("BMI = %.2f\nYou are Overweight", bmi));
                    txtResult.setBackgroundColor(getResources().getColor(android.R.color.holo_red_light));

                } else if(bmi < 18.5) {

                    txtResult.setText(String.format("BMI = %.2f\nYou are Underweight", bmi));
                    txtResult.setBackgroundColor(getResources().getColor(android.R.color.holo_orange_light));

                } else {

                    txtResult.setText(String.format("BMI = %.2f\nYou are Healthy", bmi));
                    txtResult.setBackgroundColor(getResources().getColor(android.R.color.holo_green_light));
                }
            }
        });


    }
}