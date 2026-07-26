package com.elomp.app.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.elomp.app.R;
import com.elomp.app.data.Ranking;
import com.elomp.app.data.Repository;

import java.util.List;

public class RankingsActivity extends Activity {

    static final String EXTRA_RANKING_ID = "ranking_id";
    static final String EXTRA_RANKING_NAME = "ranking_name";

    private Repository repository;
    private ListView listView;
    private TextView emptyText;
    private RankingAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list_with_add);
        repository = new Repository(this);

        listView = (ListView) findViewById(R.id.list);
        emptyText = (TextView) findViewById(R.id.empty_text);
        emptyText.setText(R.string.empty_rankings);

        adapter = new RankingAdapter();
        listView.setAdapter(adapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Ranking ranking = (Ranking) adapter.getItem(position);
                Intent intent = new Intent(RankingsActivity.this, RankingDetailActivity.class);
                intent.putExtra(EXTRA_RANKING_ID, ranking.id);
                intent.putExtra(EXTRA_RANKING_NAME, ranking.name);
                startActivity(intent);
            }
        });

        Button addButton = (Button) findViewById(R.id.btn_add);
        addButton.setText(R.string.btn_add_ranking);
        addButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showAddRankingDialog();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        reload();
    }

    private void reload() {
        adapter.setData(repository.listRankings());
        boolean empty = adapter.getCount() == 0;
        listView.setVisibility(empty ? View.GONE : View.VISIBLE);
        emptyText.setVisibility(empty ? View.VISIBLE : View.GONE);
    }

    private void showAddRankingDialog() {
        final EditText input = new EditText(this);
        input.setHint(R.string.hint_ranking_name);

        new AlertDialog.Builder(this)
                .setTitle(R.string.btn_add_ranking)
                .setView(input)
                .setPositiveButton(R.string.btn_save, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String name = input.getText().toString().trim();
                        if (TextUtils.isEmpty(name)) {
                            Toast.makeText(RankingsActivity.this, R.string.error_empty_name, Toast.LENGTH_SHORT).show();
                            return;
                        }
                        repository.addRanking(name);
                        reload();
                    }
                })
                .setNegativeButton(R.string.btn_cancel, null)
                .show();
    }

    private final class RankingAdapter extends BaseAdapter {
        private List<Ranking> rankings;

        void setData(List<Ranking> rankings) {
            this.rankings = rankings;
            notifyDataSetChanged();
        }

        @Override
        public int getCount() {
            return rankings == null ? 0 : rankings.size();
        }

        @Override
        public Object getItem(int position) {
            return rankings.get(position);
        }

        @Override
        public long getItemId(int position) {
            return rankings.get(position).id;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view = convertView;
            if (view == null) {
                view = LayoutInflater.from(RankingsActivity.this).inflate(R.layout.item_ranking, parent, false);
            }
            ((TextView) view.findViewById(R.id.text_name)).setText(rankings.get(position).name);
            return view;
        }
    }
}
