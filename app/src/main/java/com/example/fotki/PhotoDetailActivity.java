package com.example.fotki;

import android.content.Context;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.bitmap.Rotate;
import com.bumptech.glide.request.RequestOptions;
import com.example.fotki.Models.Comment;
import com.example.fotki.Utils.FileUtils;
import com.example.fotki.adapter.CommentAdapter;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class PhotoDetailActivity extends AppCompatActivity {
    private DatabaseReference databaseReference;
    private StorageReference storageReference;
    private String imageUrl;
    private String username;
    private String email;
    private CommentAdapter commentAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_photo_detail);

        databaseReference = FirebaseDatabase.getInstance().getReference();
        storageReference = FirebaseStorage.getInstance().getReference();

        imageUrl = getIntent().getStringExtra("imageUrl");
        username = getIntent().getStringExtra("username");
        email = getIntent().getStringExtra("email");

        TextView textViewAuthor = findViewById(R.id.authorLabel);
        textViewAuthor.setText("Загрузил: " + username);

        EditText commentEditText = findViewById(R.id.commentEditText);
        Button postCommentButton = findViewById(R.id.postCommentButton);

        Button deleteButton = findViewById(R.id.deleteButton);

        FirebaseUser currUser = FirebaseAuth.getInstance().getCurrentUser();
        String currentUsername = currUser.getDisplayName();
        if (!Objects.equals(currUser.getEmail(), email)) {
            deleteButton.setVisibility(View.GONE);

        }

        postCommentButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String commentText = commentEditText.getText().toString().trim();

                if (!TextUtils.isEmpty(commentText)) {
                    addComment(commentText, currentUsername);
                    commentEditText.setText("");

                }
            }
        });

        deleteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                deletePhoto(imageUrl);
            }
        });

        String imageUrl = getIntent().getStringExtra("imageUrl");

        ImageView imageView = findViewById(R.id.detailImageView);

        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        int screenWidth = displayMetrics.widthPixels;

        ViewGroup.LayoutParams layoutParams = imageView.getLayoutParams();
        layoutParams.height = screenWidth;
        imageView.setLayoutParams(layoutParams);

        RequestOptions requestOptions = new RequestOptions()
                .transform(new Rotate(getImageRotation(imageUrl)))
                .diskCacheStrategy(DiskCacheStrategy.ALL);

        Glide.with(this)
                .load(imageUrl)
                .apply(requestOptions)
                .into(imageView);

        RecyclerView commentsRecyclerView = findViewById(R.id.commentsRecyclerView);
        commentsRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        List<Comment> comments = new ArrayList<>();
        commentAdapter = new CommentAdapter(comments);
        commentsRecyclerView.setAdapter(commentAdapter);

        loadComments(imageUrl);

        setComments(comments);

        setComments(comments);
    }

    private void addComment(String commentText, String username) {
        DatabaseReference commentsRef = databaseReference.child("comments").child(generateSafeKey(imageUrl));

        String commentId = commentsRef.push().getKey();
        Comment comment = new Comment(commentText, username);

        commentsRef.child(commentId).setValue(comment);
    }

    private String generateSafeKey(String imageUrl) {
        return imageUrl.replaceAll("[.#$\\[\\]]", "_");
    }

    private void loadComments(String imageUrl) {
        DatabaseReference commentsRef = databaseReference.child("comments").child(generateSafeKey(imageUrl));

        commentsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                List<Comment> comments = new ArrayList<>();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    Comment comment = snapshot.getValue(Comment.class);
                    if (comment != null) {
                        comments.add(comment);
                    }
                }

                if (commentAdapter == null) {
                    commentAdapter = new CommentAdapter(comments);
                    RecyclerView recyclerView = findViewById(R.id.commentsRecyclerView);
                    recyclerView.setLayoutManager(new LinearLayoutManager(PhotoDetailActivity.this));
                    recyclerView.setAdapter(commentAdapter);
                } else {
                    commentAdapter.getComments().addAll(comments);
                }

                if (commentAdapter != null) {
                    commentAdapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(PhotoDetailActivity.this, "Ошибка при загрузке комментариев", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setComments(List<Comment> comments) {
        RecyclerView recyclerView = findViewById(R.id.commentsRecyclerView);

        CommentAdapter commentAdapter = new CommentAdapter(comments);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(commentAdapter);
    }

    private void deletePhoto(String imageUrl) {
        StorageReference imageRef = FirebaseStorage.getInstance().getReferenceFromUrl(imageUrl);
        imageRef.delete().addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                DatabaseReference databaseRef = FirebaseDatabase.getInstance().getReference("images");
                Query query = databaseRef.orderByChild("imageUrl").equalTo(imageUrl);
                query.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                            snapshot.getRef().removeValue().addOnSuccessListener(new OnSuccessListener<Void>() {
                                @Override
                                public void onSuccess(Void aVoid) {
                                    Toast.makeText(PhotoDetailActivity.this, "Изображение удалено", Toast.LENGTH_SHORT).show();

                                    finish();
                                }
                            }).addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    Toast.makeText(PhotoDetailActivity.this, "Ошибка при удалении записи из базы данных", Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        Toast.makeText(PhotoDetailActivity.this, "Ошибка при получении данных из базы данных", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(PhotoDetailActivity.this, "Ошибка при удалении изображения из хранилища", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private int getImageRotation(String imageUrl) {
        try {
            Context context = this;
            Uri imageUri = Uri.parse(imageUrl);
            String imagePath = FileUtils.getPath(context, imageUri);

            ExifInterface exif = new ExifInterface(imagePath);
            int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);

            switch (orientation) {
                case ExifInterface.ORIENTATION_ROTATE_90:
                    return 90;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    return 180;
                case ExifInterface.ORIENTATION_ROTATE_270:
                    return 270;
                default:
                    return 0;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }
}