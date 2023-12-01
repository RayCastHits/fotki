package com.example.fotki;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.fotki.Models.Image;
import com.example.fotki.adapter.ImageListAdapter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class GalleryActivity extends AppCompatActivity {
    private ActivityResultLauncher<Intent> pickImageLauncher;
    private StorageReference storageReference;
    private DatabaseReference databaseReference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gallery);

        TextView textView = findViewById(R.id.usernameText);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            String username = user.getDisplayName();
            textView.setText("Текущий пользователь: " + username);
        }

        databaseReference = FirebaseDatabase.getInstance().getReference();
        storageReference = FirebaseStorage.getInstance().getReference();

        pickImageLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        Intent data = result.getData();
                        if (data != null) {
                            Uri imageUri = data.getData();
                            if (imageUri != null) {
                                uploadImage(imageUri);
                            }
                        }
                    }
                });

        Button yourButton = findViewById(R.id.uploadButton);
        yourButton.setOnClickListener(view -> {
            Intent intent = new Intent(Intent.ACTION_PICK);
            intent.setType("image/*");
            pickImageLauncher.launch(intent);
        });

        DatabaseReference databaseRef = FirebaseDatabase.getInstance().getReference("images");
        RecyclerView recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new GridLayoutManager(this, 3));

        databaseRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                List<Image> imageModels = new ArrayList<>();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    String imageName = snapshot.child("name").getValue(String.class);
                    String imageUrl = snapshot.child("imageUrl").getValue(String.class);
                    String username = snapshot.child("creator").getValue(String.class);
                    String email = snapshot.child("email").getValue(String.class);
                    imageModels.add(new Image(imageName, imageUrl, username, email));
                }

                createImageViews(imageModels, recyclerView);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        });
    }

    private void uploadImage(Uri imageUri) {
        String uniqueImageName = UUID.randomUUID().toString();
        StorageReference imageRef = storageReference.child(uniqueImageName + ".jpg");

        imageRef.putFile(imageUri)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        imageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                            String imageUrl = uri.toString();
                            DatabaseReference imageRecordRef = databaseReference.child("images").push();

                            imageRecordRef.child("imageUrl").setValue(imageUrl);
                            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                            if (user != null) {
                                String username = user.getDisplayName();
                                String email = user.getEmail();
                                imageRecordRef.child("creator").setValue(username);
                                imageRecordRef.child("email").setValue(email);
                            }
                        });
                    }
                });
    }

    private void createImageViews(List<Image> imageModels, RecyclerView recyclerView) {
        List<String> imageUrls = new ArrayList<>();
        List<String> usernames = new ArrayList<>();
        List<String> emails = new ArrayList<>();
        for (Image imageModel : imageModels) {
            imageUrls.add(imageModel.getImageUrl());
            usernames.add(imageModel.getUsername());
            emails.add(imageModel.getEmail());
        }

        ImageListAdapter adapter = new ImageListAdapter(this, imageUrls, usernames, emails);
        recyclerView.setAdapter(adapter);

    }
}