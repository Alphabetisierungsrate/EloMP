package com.elomp.app.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.elomp.app.R;
import com.elomp.app.data.MatchEntry;
import com.elomp.app.data.Player;
import com.elomp.app.data.Repository;
import com.elomp.app.elo.EloEngine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RankingDetailActivity extends Activity {

    private Repository repository;
    private long rankingId;
    private Map<Long, String> nameById;

    private LinearLayout leaderboardContainer;
    private ListView entriesList;
    private TextView emptyEntries;
    private EntryAdapter entryAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ranking_detail);
        repository = new Repository(this);

        rankingId = getIntent().getLongExtra(RankingsActivity.EXTRA_RANKING_ID, -1);
        String rankingName = getIntent().getStringExtra(RankingsActivity.EXTRA_RANKING_NAME);
        if (rankingName != null) {
            setTitle(rankingName);
        }

        leaderboardContainer = (LinearLayout) findViewById(R.id.leaderboard_container);
        entriesList = (ListView) findViewById(R.id.list_entries);
        emptyEntries = (TextView) findViewById(R.id.empty_entries);

        entryAdapter = new EntryAdapter();
        entriesList.setAdapter(entryAdapter);

        findViewById(R.id.btn_record_result).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(RankingDetailActivity.this, ResultEntryActivity.class);
                intent.putExtra(RankingsActivity.EXTRA_RANKING_ID, rankingId);
                startActivity(intent);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        reload();
    }

    private void reload() {
        List<Player> players = repository.listPlayers();
        nameById = new HashMap<>();
        List<Long> allPlayerIds = new ArrayList<>();
        for (Player p : players) {
            nameById.put(p.id, p.name);
            allPlayerIds.add(p.id);
        }

        List<MatchEntry> entries = repository.listMatchEntries(rankingId);

        List<EloEngine.MatchResult> matchResults = new ArrayList<>();
        for (MatchEntry e : entries) {
            matchResults.add(new EloEngine.MatchResult(e.timestampMillis, e.participantIds, e.winnerIds));
        }
        Map<Long, Double> ratings = EloEngine.computeRatings(matchResults, allPlayerIds);

        renderLeaderboard(ratings);

        entryAdapter.setData(entries);
        boolean empty = entries.isEmpty();
        entriesList.setVisibility(empty ? View.GONE : View.VISIBLE);
        emptyEntries.setVisibility(empty ? View.VISIBLE : View.GONE);
    }

    private void renderLeaderboard(Map<Long, Double> ratings) {
        List<RatedPlayer> rows = new ArrayList<>();
        for (Map.Entry<Long, Double> e : ratings.entrySet()) {
            String name = nameById.get(e.getKey());
            if (name != null) {
                rows.add(new RatedPlayer(name, e.getValue()));
            }
        }
        Collections.sort(rows, new Comparator<RatedPlayer>() {
            @Override
            public int compare(RatedPlayer a, RatedPlayer b) {
                int cmp = Double.compare(b.rating, a.rating);
                return cmp != 0 ? cmp : a.name.compareToIgnoreCase(b.name);
            }
        });

        leaderboardContainer.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(this);
        for (int i = 0; i < rows.size(); i++) {
            RatedPlayer row = rows.get(i);
            View view = inflater.inflate(R.layout.item_leaderboard_row, leaderboardContainer, false);
            ((TextView) view.findViewById(R.id.text_name)).setText((i + 1) + ". " + row.name);
            ((TextView) view.findViewById(R.id.text_rating)).setText(String.valueOf(Math.round(row.rating)));
            leaderboardContainer.addView(view);
        }
    }

    private void confirmDeleteEntry(final MatchEntry entry) {
        new AlertDialog.Builder(this)
                .setMessage(R.string.confirm_delete_entry)
                .setPositiveButton(R.string.btn_delete, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        repository.deleteMatchEntry(entry.id);
                        reload();
                    }
                })
                .setNegativeButton(R.string.btn_cancel, null)
                .show();
    }

    private String namesOf(List<Long> ids) {
        StringBuilder sb = new StringBuilder();
        for (Long id : ids) {
            if (sb.length() > 0) sb.append(", ");
            String name = nameById.get(id);
            sb.append(name != null ? name : "?");
        }
        return sb.toString();
    }

    private final class EntryAdapter extends BaseAdapter {
        private List<MatchEntry> entries;

        void setData(List<MatchEntry> entries) {
            this.entries = entries;
            notifyDataSetChanged();
        }

        @Override
        public int getCount() {
            return entries == null ? 0 : entries.size();
        }

        @Override
        public Object getItem(int position) {
            return entries.get(position);
        }

        @Override
        public long getItemId(int position) {
            return entries.get(position).id;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view = convertView;
            if (view == null) {
                view = LayoutInflater.from(RankingDetailActivity.this).inflate(R.layout.item_match_entry, parent, false);
            }
            final MatchEntry entry = entries.get(position);

            List<Long> others = new ArrayList<>();
            for (Long id : entry.participantIds) {
                if (!entry.winnerIds.contains(id)) {
                    others.add(id);
                }
            }

            CharSequence dateStr = DateFormat.format("yyyy-MM-dd HH:mm", entry.timestampMillis);
            ((TextView) view.findViewById(R.id.text_timestamp)).setText(dateStr);
            ((TextView) view.findViewById(R.id.text_winners)).setText(
                    getString(R.string.format_entry_winners, namesOf(new ArrayList<>(entry.winnerIds))));
            ((TextView) view.findViewById(R.id.text_others)).setText(
                    getString(R.string.format_entry_others, namesOf(others)));

            view.findViewById(R.id.btn_delete).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    confirmDeleteEntry(entry);
                }
            });
            return view;
        }
    }
}
