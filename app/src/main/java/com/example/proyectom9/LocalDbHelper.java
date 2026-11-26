// LocalDbHelper crea, actuailza y gestiona la base de datos local SQLite (m9stock_local.db) dentro del propio movil
package com.example.proyectom9;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

public class LocalDbHelper extends SQLiteOpenHelper {

    private static final String DB_NAME = "m9stock_local.db";
    private static final int DB_VERSION = 2;

    public static final String TABLE_PRENDAS = "prendas";
    public static final String COL_ID = "id";
    public static final String COL_REFERENCIA = "referencia";
    public static final String COL_CATEGORIA = "categoria";
    public static final String COL_UBICACION = "ubicacion";
    public static final String COL_PENDIENTE_SYNC = "pendiente_sync";

    //Guarda los datos de una prenda guardada en el movil
    public static class PrendaLocal {
        public long id;
        public String referencia;
        public String categoria;
        public String ubicacion;
        public int pendienteSync;

//-----------------------------------------------------------------------------------------------------------
        // Crea un objeto prenda con sus datos y su estado de subida
        public PrendaLocal(long id, String referencia, String categoria, String ubicacion, int pendienteSync) {
            this.id = id;
            this.referencia = referencia;
            this.categoria = categoria;
            this.ubicacion = ubicacion;
            this.pendienteSync = pendienteSync;
        }
    }


//-----------------------------------------------------------------------------------------------------------
    // Prepara la base de datos local con su nombre y version
    public LocalDbHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

//-----------------------------------------------------------------------------------------------------------
    // Crea la tabla de prendas la primera vez que se abre la base de datos V 0.1 BETA
    @Override
    public void onCreate(SQLiteDatabase db) {
        String sql = "CREATE TABLE " + TABLE_PRENDAS + " (" +
                COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_REFERENCIA + " TEXT NOT NULL, " +
                COL_CATEGORIA + " TEXT NOT NULL DEFAULT 'Fiesta', " +
                COL_UBICACION + " TEXT NOT NULL, " +
                COL_PENDIENTE_SYNC + " INTEGER NOT NULL DEFAULT 1)";
        db.execSQL(sql);
    }

//-----------------------------------------------------------------------------------------------------------
    // Borra y vuelve a crear la tabla si sube la version de la base de datos V 0.2 BETA
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_PRENDAS);
        onCreate(db);
    }

//-----------------------------------------------------------------------------------------------------------
    // Guarda una prenda nueva en el movil marcada como pendiente de subir V 0.2 BETA DEBUG
    public long insertarPrendaLocal(String referencia, String categoria, String ubicacion) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_REFERENCIA, referencia);
        values.put(COL_CATEGORIA, categoria);
        values.put(COL_UBICACION, ubicacion);

        // El 1 significa que aun falta por subir a Firebase
        values.put(COL_PENDIENTE_SYNC, 1);
        return db.insert(TABLE_PRENDAS, null, values);
    }

//-----------------------------------------------------------------------------------------------------------
    // Devuelve todas las prendas guardadas en el movil de la mas nueva a la mas vieja V 0.2 BETA
    public List<PrendaLocal> obtenerTodas() {
        List<PrendaLocal> lista = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_PRENDAS + " ORDER BY " + COL_ID + " DESC", null);
        if (cursor.moveToFirst()) {
            do {
                lista.add(new PrendaLocal(
                        cursor.getLong(cursor.getColumnIndexOrThrow(COL_ID)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COL_REFERENCIA)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COL_CATEGORIA)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COL_UBICACION)),
                        cursor.getInt(cursor.getColumnIndexOrThrow(COL_PENDIENTE_SYNC))
                ));
            } while (cursor.moveToNext());
        }
        cursor.close();
        return lista;
    }

//-----------------------------------------------------------------------------------------------------------
    // Busca solo las prendas que todavia no se han subido a Firebase V 0.3 TEST
    public List<PrendaLocal> obtenerPendientesSync() {
        List<PrendaLocal> lista = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_PRENDAS + " WHERE " + COL_PENDIENTE_SYNC + " = 1", null);
        if (cursor.moveToFirst()) {
            do {
                lista.add(new PrendaLocal(
                        cursor.getLong(cursor.getColumnIndexOrThrow(COL_ID)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COL_REFERENCIA)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COL_CATEGORIA)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COL_UBICACION)),
                        cursor.getInt(cursor.getColumnIndexOrThrow(COL_PENDIENTE_SYNC))
                ));
            } while (cursor.moveToNext());
        }
        cursor.close();
        return lista;
    }

//-----------------------------------------------------------------------------------------------------------
    // Marca una prenda como ya subida para que no vuelva a enviarse en futuras sincronizaciones V 0.3 TEST
    public void marcarComoSincronizado(long id) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();

        // El 0 indica que ya esta sincronizada con Firebase
        values.put(COL_PENDIENTE_SYNC, 0);
        db.update(TABLE_PRENDAS, values, COL_ID + " = ?", new String[]{String.valueOf(id)});
    }
}