package com.example.load_app.activities;

import android.graphics.Typeface;
import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.example.load_app.R;
import com.example.load_app.models.Package;

public class TrackPackageActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_track_package);

        TextView tvPkgId = findViewById(R.id.tvTrackPkgId);
        TextView stepPlaced = findViewById(R.id.stepPlaced);
        TextView stepAssigned = findViewById(R.id.stepAssigned);
        TextView stepPickedUp = findViewById(R.id.stepPickedUp);
        TextView stepDelivered = findViewById(R.id.stepDelivered);

        // In a real app, you'd pass the Package ID and fetch from Firestore here.
        // For the demo, we'll simulate a status based on Intent Extras
        String status = getIntent().getStringExtra("STATUS");
        String pkgId = getIntent().getStringExtra("PKG_ID");

        tvPkgId.setText("Package: " + pkgId);

        // Highlight the steps
        if (status != null) {
            if (status.equals(Package.STATUS_PENDING)) {
                stepPlaced.setTextColor(getResources().getColor(R.color.successGreen));
            } else if (status.equals(Package.STATUS_ASSIGNED)) {
                stepPlaced.setTextColor(getResources().getColor(R.color.successGreen));
                stepAssigned.setTextColor(getResources().getColor(R.color.successGreen));
                stepAssigned.setTypeface(null, Typeface.BOLD);
            } else if (status.equals(Package.STATUS_PICKED_UP)) {
                stepPlaced.setTextColor(getResources().getColor(R.color.successGreen));
                stepAssigned.setTextColor(getResources().getColor(R.color.successGreen));
                stepPickedUp.setTextColor(getResources().getColor(R.color.successGreen));
                stepPickedUp.setTypeface(null, Typeface.BOLD);
            } else if (status.equals(Package.STATUS_DELIVERED)) {
                stepPlaced.setTextColor(getResources().getColor(R.color.successGreen));
                stepAssigned.setTextColor(getResources().getColor(R.color.successGreen));
                stepPickedUp.setTextColor(getResources().getColor(R.color.successGreen));
                stepDelivered.setTextColor(getResources().getColor(R.color.successGreen));
                stepDelivered.setTypeface(null, Typeface.BOLD);
            }
        }
    }
}