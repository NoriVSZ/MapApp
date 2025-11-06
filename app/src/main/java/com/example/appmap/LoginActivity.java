package com.example.appmap;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast; // Importa Toast para mostrar mensajes

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;

public class LoginActivity extends AppCompatActivity {

    private static final int RC_SIGN_IN = 9001;
    private GoogleSignInClient mGoogleSignInClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Configura las opciones de inicio de sesión de Google
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail() // Solicita el email del usuario
                .build();

        // Construye un cliente de GoogleSignIn con las opciones especificadas
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        // Configura el botón de inicio de sesión
        SignInButton signInButton = findViewById(R.id.sign_in_button);
        signInButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                signIn();
            }
        });
    }

    // Método para iniciar el flujo de inicio de sesión
    private void signIn() {
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    /**
     * Este método se llama automáticamente después de que el usuario selecciona una cuenta de Google.
     * Aquí es donde recibimos el resultado.
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Comprueba si el resultado corresponde a nuestra solicitud de inicio de sesión
        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            handleSignInResult(task);
        }
    }

    /**
     * Procesa el resultado del intento de inicio de sesión.
     */
    private void handleSignInResult(Task<GoogleSignInAccount> completedTask) {
        try {
            // Intenta obtener la cuenta del usuario. Si esto no lanza una excepción, el login fue exitoso.
            GoogleSignInAccount account = completedTask.getResult(ApiException.class);
            Log.d("LoginActivity", "Inicio de sesión exitoso para: " + account.getEmail());

            // Actualiza la UI (en este caso, navega a la siguiente pantalla)
            updateUI(account);

        } catch (ApiException e) {
            // El inicio de sesión falló. Registra el error.
            Log.w("LoginActivity", "signInResult:failed code=" + e.getStatusCode());

            // Informa al usuario que algo salió mal y no hagas nada más.
            updateUI(null);
        }
    }

    /**
     * Actualiza la interfaz de usuario según el estado del inicio de sesión.
     * Si la cuenta no es nula, navega a la actividad principal.
     * Si es nula, significa que hubo un fallo.
     */
    private void updateUI(@Nullable GoogleSignInAccount account) {
        if (account != null) {
            // El usuario ha iniciado sesión correctamente.
            // Crea un Intent para ir a la actividad del mapa.
            Intent intent = new Intent(this, MainActivity.class); // <-- ¡Asegúrate de que 'MainActivity.class' sea tu actividad del mapa!
            startActivity(intent);
            finish(); // Cierra LoginActivity para que el usuario no pueda volver a ella con el botón "atrás".
        } else {
            // El inicio de sesión falló. Muestra un mensaje al usuario.
            Toast.makeText(this, "Falló el inicio de sesión con Google", Toast.LENGTH_SHORT).show();
        }
    }
}
