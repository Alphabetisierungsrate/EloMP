package com.elomp.app.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Thin wrapper around SQLite; every call opens/uses the shared writable database. */
public final class Repository {

    private final DbHelper dbHelper;

    public Repository(Context context) {
        this.dbHelper = new DbHelper(context);
    }

    // ---- players ----

    public long addPlayer(String name) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("name", name);
        return db.insert("players", null, values);
    }

    public List<Player> listPlayers() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        List<Player> result = new ArrayList<>();
        Cursor c = db.query("players", new String[]{"_id", "name"}, null, null, null, null, "name COLLATE NOCASE ASC");
        try {
            while (c.moveToNext()) {
                result.add(new Player(c.getLong(0), c.getString(1)));
            }
        } finally {
            c.close();
        }
        return result;
    }

    /** Returns false (and deletes nothing) if the player has recorded match history. */
    public boolean deletePlayer(long playerId) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        Cursor c = db.rawQuery("SELECT COUNT(*) FROM match_participants WHERE player_id = ?",
                new String[]{String.valueOf(playerId)});
        boolean hasHistory;
        try {
            c.moveToFirst();
            hasHistory = c.getInt(0) > 0;
        } finally {
            c.close();
        }
        if (hasHistory) {
            return false;
        }
        db.delete("players", "_id = ?", new String[]{String.valueOf(playerId)});
        return true;
    }

    // ---- rankings (per-game leaderboards) ----

    public long addRanking(String name) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("name", name);
        return db.insert("rankings", null, values);
    }

    public List<Ranking> listRankings() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        List<Ranking> result = new ArrayList<>();
        Cursor c = db.query("rankings", new String[]{"_id", "name"}, null, null, null, null, "name COLLATE NOCASE ASC");
        try {
            while (c.moveToNext()) {
                result.add(new Ranking(c.getLong(0), c.getString(1)));
            }
        } finally {
            c.close();
        }
        return result;
    }

    // ---- match entries ----

    public long addMatchEntry(long rankingId, long timestampMillis, List<Long> participantIds, Set<Long> winnerIds) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.beginTransaction();
        try {
            ContentValues entryValues = new ContentValues();
            entryValues.put("ranking_id", rankingId);
            entryValues.put("timestamp", timestampMillis);
            long entryId = db.insert("match_entries", null, entryValues);

            for (Long playerId : participantIds) {
                ContentValues pValues = new ContentValues();
                pValues.put("entry_id", entryId);
                pValues.put("player_id", playerId);
                pValues.put("is_winner", winnerIds.contains(playerId) ? 1 : 0);
                db.insert("match_participants", null, pValues);
            }
            db.setTransactionSuccessful();
            return entryId;
        } finally {
            db.endTransaction();
        }
    }

    public void deleteMatchEntry(long entryId) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.beginTransaction();
        try {
            db.delete("match_participants", "entry_id = ?", new String[]{String.valueOf(entryId)});
            db.delete("match_entries", "_id = ?", new String[]{String.valueOf(entryId)});
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    public List<MatchEntry> listMatchEntries(long rankingId) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        List<MatchEntry> result = new ArrayList<>();
        Cursor c = db.query("match_entries", new String[]{"_id", "timestamp"},
                "ranking_id = ?", new String[]{String.valueOf(rankingId)}, null, null, "timestamp DESC");
        try {
            while (c.moveToNext()) {
                long entryId = c.getLong(0);
                long timestamp = c.getLong(1);
                result.add(new MatchEntry(entryId, rankingId, timestamp,
                        participantsOf(db, entryId), winnersOf(db, entryId)));
            }
        } finally {
            c.close();
        }
        return result;
    }

    public List<MatchEntry> listAllMatchEntries(long rankingId) {
        return listMatchEntries(rankingId);
    }

    private List<Long> participantsOf(SQLiteDatabase db, long entryId) {
        List<Long> ids = new ArrayList<>();
        Cursor c = db.query("match_participants", new String[]{"player_id"},
                "entry_id = ?", new String[]{String.valueOf(entryId)}, null, null, null);
        try {
            while (c.moveToNext()) {
                ids.add(c.getLong(0));
            }
        } finally {
            c.close();
        }
        return ids;
    }

    private Set<Long> winnersOf(SQLiteDatabase db, long entryId) {
        Set<Long> ids = new LinkedHashSet<>();
        Cursor c = db.query("match_participants", new String[]{"player_id"},
                "entry_id = ? AND is_winner = 1", new String[]{String.valueOf(entryId)}, null, null, null);
        try {
            while (c.moveToNext()) {
                ids.add(c.getLong(0));
            }
        } finally {
            c.close();
        }
        return ids;
    }
}
