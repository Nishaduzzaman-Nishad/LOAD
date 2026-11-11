package com.example.load_app.activities;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.example.load_app.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class EditProfileActivity extends AppCompatActivity {

    private ImageView ivAvatar;
    private EditText etName, etPhone;
    private String base64Image = "";
    private static final int PICK_IMAGE_REQUEST = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        ivAvatar = findViewById(R.id.ivEditAvatar);
        etName = findViewById(R.id.etEditName);
        etPhone = findViewById(R.id.etEditPhone);
        Button btnSave = findViewById(R.id.btnSaveProfile);

        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        if (mAuth.getCurrentUser() != null) {
            String uid = mAuth.getCurrentUser().getUid();
            db.collection("users").document(uid).get().addOnSuccessListener(doc -> {
                if (doc.exists()) {
                    etName.setText(doc.getString("fullName"));
                    etPhone.setText(doc.getString("phone"));
                    base64Image = doc.getString("profileImage");
                    if (base64Image != null && !base64Image.isEmpty()) {
                        try {
                            byte[] decodedString = Base64.decode(base64Image, Base64.DEFAULT);
                            Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                            ivAvatar.setImageBitmap(decodedByte);
                            ivAvatar.setImageTintList(null);
                        } catch (Exception e) { e.printStackTrace(); }
                    }
                }
            });

            ivAvatar.setOnClickListener(v -> {
                Intent intent = new Intent();
                intent.setType("image/*");
                intent.setAction(Intent.ACTION_GET_CONTENT);
                startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE_REQUEST);
            });

            btnSave.setOnClickListener(v -> {
                String newName = etName.getText().toString().trim();
                String newPhone = etPhone.getText().toString().trim();

                if (newName.isEmpty() || newPhone.isEmpty()) {
                    Toast.makeText(this, "Fields cannot be empty", Toast.LENGTH_SHORT).show();
                    return;
                }

                db.collection("users").document(uid)
                        .update("fullName", newName, "phone", newPhone, "profileImage", base64Image)
                        .addOnSuccessListener(aVoid -> {
                            Toast.makeText(this, "Profile Updated!", Toast.LENGTH_SHORT).show();
                            finish();
                        });
            });
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            Uri imageUri = data.getData();
            try {
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);
                Bitmap resized = getResizedBitmap(bitmap, 400);
                ivAvatar.setImageBitmap(resized);
                ivAvatar.setImageTintList(null);

                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                resized.compress(Bitmap.CompressFormat.JPEG, 50, baos);
                base64Image = Base64.encodeToString(baos.toByteArray(), Base64.DEFAULT);

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private Bitmap getResizedBitmap(Bitmap image, int maxSize) {
        int width = image.getWidth();
        int height = image.getHeight();
        float bitmapRatio = (float) width / (float) height;
        if (bitmapRatio > 1) {
            width = maxSize;
            height = (int) (width / bitmapRatio);
        } else {
            height = maxSize;
            width = (int) (height * bitmapRatio);
        }
        return Bitmap.createScaledBitmap(image, width, height, true);
    }
}