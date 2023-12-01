package com.example.fotki;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import com.example.fotki.Models.User;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import java.util.Objects;


public class MainActivity extends AppCompatActivity {
    Button btnSign, btnReg;
    FirebaseAuth auth;
    FirebaseDatabase db;
    DatabaseReference users;
    ConstraintLayout root;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnSign = findViewById(R.id.btnSign);
        btnReg = findViewById(R.id.btnReg);

        root = findViewById(R.id.root_element);

        auth = FirebaseAuth.getInstance();
        db = FirebaseDatabase.getInstance();
        users = db.getReference("Users");

        btnReg.setOnClickListener(v -> showRegisterWindow());
        btnSign.setOnClickListener(v -> showSignWindow());
    }

    private void showSignWindow() {
        AlertDialog.Builder dialog = new AlertDialog.Builder(this);
        dialog.setTitle("Вход");

        LayoutInflater inflater = LayoutInflater.from(this);
        View sign_window = inflater.inflate(R.layout.sign, null);
        dialog.setView(sign_window);

        final TextInputEditText email = sign_window.findViewById(R.id.emailField);
        final TextInputEditText password = sign_window.findViewById(R.id.passwordField);

        dialog.setPositiveButton("Вход", (dialogInterface, which) -> {
            if(TextUtils.isEmpty(Objects.requireNonNull(email.getText()).toString().trim())) {
                Snackbar.make(root, "Введите почту", Snackbar.LENGTH_SHORT).show();
                return;
            }

            if(Objects.requireNonNull(password.getText()).toString().trim().length() < 5) {
                Snackbar.make(root, "Введите пароль", Snackbar.LENGTH_SHORT).show();
                return;
            }

            auth.signInWithEmailAndPassword(email.getText().toString().trim(), password.getText().toString().trim()).addOnSuccessListener(authResult -> {
                startActivity(new Intent(MainActivity.this, GalleryActivity.class));
                finish();
            }).addOnFailureListener(e -> Snackbar.make(root, "Ошибка авторизации: " + e.getMessage(), Snackbar.LENGTH_SHORT).show());
        });

        dialog.setNegativeButton("Отмена", (dialogInterface, which) -> dialogInterface.dismiss());

        dialog.show();
    }

    private void showRegisterWindow() {
        AlertDialog.Builder dialog = new AlertDialog.Builder(this);
        dialog.setTitle("Регистрация");

        LayoutInflater inflater = LayoutInflater.from(this);
        View register_window = inflater.inflate(R.layout.register, null);
        dialog.setView(register_window);

        final TextInputEditText nickname = register_window.findViewById(R.id.nicknameField);
        final TextInputEditText email = register_window.findViewById(R.id.emailField);
        final TextInputEditText password = register_window.findViewById(R.id.passwordField);

        dialog.setPositiveButton("Регистрация", (dialogInterface, which) -> {
            if(TextUtils.isEmpty(Objects.requireNonNull(email.getText()).toString().trim())) {
                Snackbar.make(root, "Введите почту", Snackbar.LENGTH_SHORT).show();
                return;
            }

            if(TextUtils.isEmpty(Objects.requireNonNull(nickname.getText()).toString().trim())) {
                Snackbar.make(root, "Введите никнейм", Snackbar.LENGTH_SHORT).show();
                return;
            }

            if(Objects.requireNonNull(password.getText()).toString().trim().length() < 5) {
                Snackbar.make(root, "Пароль должен состоять более чем из 5 символов", Snackbar.LENGTH_SHORT).show();
                return;
            }

            auth.createUserWithEmailAndPassword(email.getText().toString().trim(), password.getText().toString().trim()).addOnSuccessListener(authResult -> {
                User user = new User();
                user.setNickname(nickname.getText().toString().trim());
                user.setEmail(email.getText().toString().trim());
                user.setPassword(password.getText().toString().trim());


                users.child(Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid()).setValue(user).addOnSuccessListener(unused -> Snackbar.make(root, "Готово! Можете войти в аккаунт", Snackbar.LENGTH_SHORT).show());
                UserProfileChangeRequest userProfileChangeRequest = new UserProfileChangeRequest.Builder().setDisplayName(nickname.getText().toString().trim()).build();
                FirebaseUser newUser = FirebaseAuth.getInstance().getCurrentUser();
                newUser.updateProfile(userProfileChangeRequest);


            }).addOnFailureListener((e -> Snackbar.make(root, "Ошибка авторизации: " + e.getMessage(), Snackbar.LENGTH_SHORT).show()));
        });

        dialog.setNegativeButton("Отмена", (dialogInterface, which) -> dialogInterface.dismiss());

        dialog.show();
    }
}