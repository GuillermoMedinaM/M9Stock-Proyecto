package com.example.proyectom9;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.List;
import java.util.Map;


//-----------------------------------------------------------------------------------------------------------
public class MainActivity extends AppCompatActivity {
    private static final String UBI_TIENDA = "Tienda";
    private static final String UBI_ALMACEN = "Almacén";
    private static final String UBI_MODISTA = "Modista y otros";
    private static final String DB_URL = "https://proyectom9-6c840-default-rtdb.europe-west1.firebasedatabase.app";

    private ScrollView vistaStock, vistaControl;
    private TextView tvEstadoRed, tvTituloSeccion, tvSubtituloSeccion, tvListadoSeccion, tvEstadisticasGlobales;
    private EditText etReferencia, etCategoria;
    private Spinner spUbicacion;
    private Button btnGuardarLocal, btnSincronizarAhora;
    private BottomNavigationView bottomNavM9;

    // Guarda en que pestana estamos ahora mismo
    private String pestanaActual = UBI_TIENDA;
    private LocalDbHelper dbHelper;
    private DatabaseReference prendasRef;
    private ConnectivityManager connectivityManager;
    private ConnectivityManager.NetworkCallback networkCallback;

//ONCREATE PRINCIPAL V 0.5 BETA
//-----------------------------------------------------------------------------------------------------------
    // Prepara la pantalla, los botones y la conexion con la base de datos
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        // Evita que la barra superior o inferior tape el contenido
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        dbHelper = new LocalDbHelper(this);

        // Conecta directamente con la base de datos
        prendasRef = FirebaseDatabase.getInstance(DB_URL).getReference("prendas");

        vistaStock = findViewById(R.id.vistaStock);
        vistaControl = findViewById(R.id.vistaControl);
        tvEstadoRed = findViewById(R.id.tvEstadoRed);
        tvTituloSeccion = findViewById(R.id.tvTituloSeccion);
        tvSubtituloSeccion = findViewById(R.id.tvSubtituloSeccion);
        tvListadoSeccion = findViewById(R.id.tvListadoSeccion);
        tvEstadisticasGlobales = findViewById(R.id.tvEstadisticasGlobales);
        etReferencia = findViewById(R.id.etReferencia);
        etCategoria = findViewById(R.id.etCategoria);
        spUbicacion = findViewById(R.id.spUbicacion);
        btnGuardarLocal = findViewById(R.id.btnGuardarLocal);
        btnSincronizarAhora = findViewById(R.id.btnSincronizarAhora);
        bottomNavM9 = findViewById(R.id.bottomNavM9);

        // Rellena el desplegable
        String[] opcionesUbicacion = {UBI_TIENDA, UBI_ALMACEN, UBI_MODISTA};
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                opcionesUbicacion
        );
        spUbicacion.setAdapter(spinnerAdapter);

        actualizarEstadoRed();
        registrarEscuchaRed();
        mostrarSeccionStock(UBI_TIENDA, "Stock en Tienda", "Vestidos de fiesta y comunión en sala de venta");
        actualizarEstadisticas();

        // Sube lo que quedo pendiente sin mensajes al arrancar
        if (hayInternet()) {
            sincronizarPendientesConFirebase(false);
        }

        // Boton para pasar de pagina entre si; cambia entre las secciones de stock y control
        bottomNavM9.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_tienda) {
                mostrarSeccionStock(UBI_TIENDA, "Stock en Tienda", "Vestidos de fiesta y comunión en sala de venta");
                return true;
            } else if (id == R.id.nav_almacen) {
                mostrarSeccionStock(UBI_ALMACEN, "Stock en Almacén", "Reserva y reposición de prendas en almacén");
                return true;
            } else if (id == R.id.nav_modista) {
                mostrarSeccionStock(UBI_MODISTA, "Modista y Otros", "Prendas en arreglo, entallado o ubicaciones especiales");
                return true;
            } else if (id == R.id.nav_control) {
                // Esconde el listado y muestra el panel de alta y resumen
                vistaStock.setVisibility(View.GONE);
                vistaControl.setVisibility(View.VISIBLE);
                actualizarEstadisticas();
                return true;
            }
            return false;
        });


        // Guarda el articulo en el movil y la sube si hay internet
        btnGuardarLocal.setOnClickListener(v -> {
            String ref = etReferencia.getText().toString().trim();
            String cat = etCategoria.getText().toString().trim();
            String ubi = spUbicacion.getSelectedItem().toString();

            if (ref.isEmpty()) {
                Toast.makeText(this, "Indica la referencia del vestido", Toast.LENGTH_SHORT).show();
                return;
            }

            //Si no se escribe tipo, se pone Fiesta por defecto
            if (cat.isEmpty()) {
                cat = "Fiesta";
            }

            // Guarda primero en la base de datos del movil por si no hay internet
            dbHelper.insertarPrendaLocal(ref, cat, ubi);
            etReferencia.setText("");
            etCategoria.setText("");

            actualizarEstadisticas();

            // Solo actualiza la lista visible si coincide con la pestana abierta
            if (pestanaActual.equals(ubi)) {
                refrescarListadoSeccion();
            }

            if (hayInternet()) {
                sincronizarPendientesConFirebase(true);
            } else {
                Toast.makeText(this, "Sin conexión: guardado en SQLite para subir luego", Toast.LENGTH_SHORT).show();
            }
        });

        //Fuerza la subida manual de las prendas que esten pendientes DEBUG
        btnSincronizarAhora.setOnClickListener(v -> {
            if (hayInternet()) {
                sincronizarPendientesConFirebase(true);
            } else {
                Toast.makeText(this, "No hay conexion a Internet", Toast.LENGTH_SHORT).show();
            }
        });
    }

//-----------------------------------------------------------------------------------------------------------
    // Cambia el titulo, el texto y las prendas que se ven en pantalla V0.2 BETA
    private void mostrarSeccionStock(String ubicacion, String titulo, String subtitulo) {
        pestanaActual = ubicacion;
        vistaControl.setVisibility(View.GONE);
        vistaStock.setVisibility(View.VISIBLE);
        tvTituloSeccion.setText(titulo);
        tvSubtituloSeccion.setText(subtitulo);
        refrescarListadoSeccion();
    }


//-----------------------------------------------------------------------------------------------------------
    //Muestra en pantalla las prendas de esta  pestana V 0.3 BETA
    private void refrescarListadoSeccion() {
        List<LocalDbHelper.PrendaLocal> todas = dbHelper.obtenerTodas();
        StringBuilder sb = new StringBuilder();
        int contador = 0;

        for (LocalDbHelper.PrendaLocal p : todas) {
            if (p.ubicacion.equalsIgnoreCase(pestanaActual)) {
                contador++;

                // Indica si ya esta en Firebase o sigue esperando subida DEBUG
                String syncTag = (p.pendienteSync == 1) ? "· Pendiente sync" : "· Sincronizado";
                sb.append("• ").append(p.referencia)
                        .append("  [").append(p.categoria).append("] ")
                        .append(syncTag).append("\n");
            }
        }

        if (contador == 0) {
            sb.append("No hay prendas registradas en esta sección por el momento.");
        }
        tvListadoSeccion.setText(sb.toString());
    }

//-----------------------------------------------------------------------------------------------------------
    //Cuenta cuantas prendas hay en cada sitio y cuantas faltan por subir V 0.1 BETA
    private void actualizarEstadisticas() {
        List<LocalDbHelper.PrendaLocal> todas = dbHelper.obtenerTodas();
        int total = todas.size();
        int enTienda = 0;
        int enAlmacen = 0;
        int enModista = 0;
        int pendientes = 0;

        for (LocalDbHelper.PrendaLocal p : todas) {
            if (UBI_TIENDA.equalsIgnoreCase(p.ubicacion)) enTienda++;
            else if (UBI_ALMACEN.equalsIgnoreCase(p.ubicacion)) enAlmacen++;
            else enModista++;

            if (p.pendienteSync == 1) pendientes++;
        }

        String stats = "Total prendas registradas: " + total + "\n"
                + "• En Tienda: " + enTienda + "\n"
                + "• En Almacén: " + enAlmacen + "\n"
                + "• Modista y otros: " + enModista + "\n"
                + "• Pendientes de subir a Firebase: " + pendientes;
        tvEstadisticasGlobales.setText(stats);
    }

//-----------------------------------------------------------------------------------------------------------
    // Envia a Firebase las cosas guardadas en el movil que aun no se subieron V 0.3 TEST
    private void sincronizarPendientesConFirebase(boolean mostrarAviso) {
        List<LocalDbHelper.PrendaLocal> pendientes = dbHelper.obtenerPendientesSync();
        if (pendientes.isEmpty()) {
            refrescarListadoSeccion();
            actualizarEstadisticas();
            if (mostrarAviso) {
                Toast.makeText(this, "No hay prendas pendientes por subir", Toast.LENGTH_SHORT).show();
            }
            return;
        }

        if (mostrarAviso) {
            Toast.makeText(this, "Subiendo " + pendientes.size() + " prenda(s) a Firebase...", Toast.LENGTH_SHORT).show();
        }

        for (LocalDbHelper.PrendaLocal prenda : pendientes) {
            // Crea un hueco nuevo con un id para Firebase
            String key = prendasRef.push().getKey();
            if (key == null) continue;

            Map<String, Object> datos = new HashMap<>();
            datos.put("referencia", prenda.referencia);
            datos.put("categoria", prenda.categoria);
            datos.put("ubicacion", prenda.ubicacion);
            datos.put("localId", prenda.id);

            //Si en fibrase llego bien el articulo lo confirma como sincronizado
            prendasRef.child(key).setValue(datos)
                    .addOnSuccessListener(unused -> {
                        dbHelper.marcarComoSincronizado(prenda.id);
                        refrescarListadoSeccion();
                        actualizarEstadisticas();
                    })
                    .addOnFailureListener(e -> Toast.makeText(this, "Error Firebase: " + e.getMessage(), Toast.LENGTH_LONG).show()
                    );
        }
    }

//-----------------------------------------------------------------------------------------------------------
    // Comprueba si el movil tiene conexion a internet
    private boolean hayInternet() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return false;
        Network network = cm.getActiveNetwork();
        if (network == null) return false;
        NetworkCapabilities caps = cm.getNetworkCapabilities(network);
        return caps != null && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
    }

//-----------------------------------------------------------------------------------------------------------
    // Cambia el texto y el color del estado de conexion arriba en la pantalla V 0.4 ALPHA
    private void actualizarEstadoRed() {
        if (hayInternet()) {
            tvEstadoRed.setText("Conectado · Sincronización activa con Firebase");
            tvEstadoRed.setTextColor(0xFF8FD19E);
        } else {
            tvEstadoRed.setText("Sin conexión · Trabajando en SQLite local");
            tvEstadoRed.setTextColor(0xFFE0B063);
        }
    }

//-----------------------------------------------------------------------------------------------------------
    // Queda pendiente de si vuelve o se va el internet para actualizar y sincronizar solo V 0.2 BETA
    private void registrarEscuchaRed() {
        connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager == null) return;

        NetworkRequest request = new NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build();

        networkCallback = new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(@NonNull Network network) {
                // Vuelve al hilo de la pantalla para poder cambiar textos y avisos sin fallo
                runOnUiThread(() -> {
                    actualizarEstadoRed();
                    sincronizarPendientesConFirebase(false);
                });
            }

            @Override
            public void onLost(@NonNull Network network) {
                runOnUiThread(() -> actualizarEstadoRed());
            }
        };
        connectivityManager.registerNetworkCallback(request, networkCallback);
    }

//-----------------------------------------------------------------------------------------------------------
    // Quita el aviso de red al cerrar la pantalla V 0.1 BETA
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (connectivityManager != null && networkCallback != null) {
            try {
                connectivityManager.unregisterNetworkCallback(networkCallback);
            } catch (Exception ignored) {
            }
        }
    }
}