package com.elomp.app.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public final class DbHelper extends SQLiteOpenHelper {

    private static final String DB_NAME = "elomp.db";
    private static final int DB_VERSION = 1;

    public DbHelper(Context context) {
        super(context.getApplicationContext(), DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE players (" +
                "_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "name TEXT NOT NULL)");

        db.execSQL("CREATE TABLE rankings (" +
                "_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "name TEXT NOT NULL)");

        db.execSQL("CREATE TABLE match_entries (" +
                "_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "ranking_id INTEGER NOT NULL," +
                "timestamp INTEGER NOT NULL," +
                "FOREIGN KEY(ranking_id) REFERENCES rankings(_id))");

        db.execSQL("CREATE TABLE match_participants (" +
                "entry_id INTEGER NOT NULL," +
                "player_id INTEGER NOT NULL," +
                "is_winner INTEGER NOT NULL," +
                "PRIMARY KEY(entry_id, player_id)," +
                "FOREIGN KEY(entry_id) REFERENCES match_entries(_id)," +
                "FOREIGN KEY(player_id) REFERENCES players(_id))");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS match_participants");
        db.execSQL("DROP TABLE IF EXISTS match_entries");
        db.execSQL("DROP TABLE IF EXISTS rankings");
        db.execSQL("DROP TABLE IF EXISTS players");
        onCreate(db);
    }
}
