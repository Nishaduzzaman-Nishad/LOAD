package com.example.load;

import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

import com.example.load.models.Package;
import com.example.load.models.Rider;
import com.example.load.models.ValidationUtils;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "LOAD_TEST";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        TextView tvOutput = findViewById(R.id.tvOutput);

        Rider rider = new Rider(
                "R001", "Rakib Hasan", "rakib@load.com",
                "01712345678", "pass123", 15.0
        );

        Package pkg = new Package(
                "P001", "Dhanmondi Road 5", "Gulshan Road 12",
                "01798765432", 5.0, "Fragile", "URGENT"
        );

        Log.d(TAG, "Rider: " + rider.getFullName() + " | Role: " + rider.getRole() + " | Capacity: " + rider.getMaxCapacityKg() + "kg");
        Log.d(TAG, "Package: " + pkg.getPackageId() + " | Status: " + pkg.getStatus() + " | Weight: " + pkg.getWeightKg() + "kg");

        StringBuilder output = new StringBuilder();
        output.append("Rider: ").append(rider.getFullName()).append("\n");
        output.append("Role: ").append(rider.getRole()).append("\n");
        output.append("Capacity: ").append(rider.getMaxCapacityKg()).append("kg\n\n");
        output.append("Package: ").append(pkg.getPackageId()).append("\n");
        output.append("Status: ").append(pkg.getStatus()).append("\n");
        output.append("Weight: ").append(pkg.getWeightKg()).append("kg\n\n");
        output.append("Valid email? ").append(ValidationUtils.isValidEmail("test@load.com")).append("\n");
        output.append("Invalid email? ").append(ValidationUtils.isValidEmail("bad-email")).append("\n");
        output.append("Valid phone? ").append(ValidationUtils.isValidPhone("01712345678")).append("\n");
        output.append("Weight 60kg error: ").append(ValidationUtils.getWeightError(60)).append("\n");
        output.append("Weight 5kg error: ").append(ValidationUtils.getWeightError(5)).append("\n");
        output.append("Empty email error: ").append(ValidationUtils.getEmailError("")).append("\n");
        output.append("Short password error: ").append(ValidationUtils.getPasswordError("abc"));

        tvOutput.setText(output.toString());
    }
}