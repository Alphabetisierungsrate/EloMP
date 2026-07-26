package com.elomp.app.ui;

import android.app.Activity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.elomp.app.R;
import com.elomp.app.data.MatchEntry;
import com.elomp.app.data.Player;
import com.elomp.app.data.Ranking;
import com.elomp.app.data.Repository;
import com.elomp.app.elo.EloEngine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TotalRankingActivity extends Activity {

    private Repository repository;
    private ListView listView;
    private TextView emptyText;
    private RowAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_total_ranking);
        repository = new Repository(this);

        listView = (ListView) findViewById(R.id.list);
        emptyText = (TextView) findViewById(R.id.empty_text);

        adapter = new RowAdapter();
        listView.setAdapter(adapter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        reload();
    }

    private void reload() {
        List<Player> players = repository.listPlayers();
        Map<Long, String> nameById = new HashMap<>();
        List<Long> allPlayerIds = new ArrayList<>();
        for (Player p : players) {
            nameById.put(p.id, p.name);
            allPlayerIds.add(p.id);
        }

        List<Ranking> rankings = repository.listRankings();
        Map<Long, Map<Long, Double>> perRankingRatings = new HashMap<>();
        for (Ranking ranking : rankings) {
            List<MatchEntry> entries = repository.listMatchEntries(ranking.id);
            if (entries.isEmpty()) {
                continue;
            }
            List<EloEngine.MatchResult> matchResults = new ArrayList<>();
            for (MatchEntry e : entries) {
                matchResults.add(new EloEngine.MatchResult(e.timestampMillis, e.participantIds, e.winnerIds));
            }
            perRankingRatings.put(ranking.id, EloEngine.computeRatings(matchResults, allPlayerIds));
        }

        Map<Long, Double> totals = EloEngine.computeTotalRatings(perRankingRatings);

        List<RatedPlayer> rows = new ArrayList<>();
        for (Map.Entry<Long, Double> e : totals.entrySet()) {
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

        adapter.setData(rows);
        boolean empty = rows.isEmpty();
        listView.setVisibility(empty ? View.GONE : View.VISIBLE);
        emptyText.setVisibility(empty ? View.VISIBLE : View.GONE);
    }

    private final class RowAdapter extends BaseAdapter {
        private List<RatedPlayer> rows = new ArrayList<>();

        void setData(List<RatedPlayer> rows) {
            this.rows = rows;
            notifyDataSetChanged();
        }

        @Override
        public int getCount() {
            return rows.size();
        }

        @Override
        public Object getItem(int position) {
            return rows.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view = convertView;
            if (view == null) {
                view = LayoutInflater.from(TotalRankingActivity.this).inflate(R.layout.item_leaderboard_row, parent, false);
            }
            RatedPlayer row = rows.get(position);
            ((TextView) view.findViewById(R.id.text_name)).setText((position + 1) + ". " + row.name);
            ((TextView) view.findViewById(R.id.text_rating)).setText(String.valueOf(Math.round(row.rating)));
            return view;
        }
    }
}
