package com.example.proyectom9;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;

public class LoginActivity extends AppCompatActivity {

    private EditText etEmail, etPassword;
    private Button btnLogin;
    private FirebaseAuth mAuth;

//-----------------------------------------------------------------------------------------------------------
    // Prepara la pantalla de inicio de sesion y comprueba el correo y contrasena al pulsar entrar V 0.4 BETA
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Conecta con el sistema de usuarios de Firebase
        mAuth = FirebaseAuth.getInstance();

        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);

        // Comprueba los datos y entra a la pantalla principal si son correctos
        btnLogin.setOnClickListener(v -> {
            String email = etEmail.getText().toString().trim();
            String password = etPassword.getText().toString().trim();

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(LoginActivity.this, "Rellene todos los campos", Toast.LENGTH_SHORT).show();
                return;
            }

            // Pide a Firebase que valide el correo y la contrasena
            mAuth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(this, task -> {
                        if (task.isSuccessful()) {
                            Toast.makeText(LoginActivity.this, "¡Bienvenido a M9Stock!", Toast.LENGTH_SHORT).show();

                            // Abre la pantalla principal y cierra el login para no volver atras con el boton volver
                            Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                            startActivity(intent);
                            finish();
                        } else {
                            Toast.makeText(LoginActivity.this, "Error de acceso: Credenciales incorrectas", Toast.LENGTH_SHORT).show();
                        }
                    });
        });
    }
}