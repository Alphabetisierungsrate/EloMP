package com.elomp.app.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.elomp.app.R;
import com.elomp.app.data.Player;
import com.elomp.app.data.Repository;

import java.util.List;

public class PlayersActivity extends Activity {

    private Repository repository;
    private ListView listView;
    private TextView emptyText;
    private PlayerAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list_with_add);
        repository = new Repository(this);

        listView = (ListView) findViewById(R.id.list);
        emptyText = (TextView) findViewById(R.id.empty_text);
        emptyText.setText(R.string.empty_players);

        adapter = new PlayerAdapter();
        listView.setAdapter(adapter);

        Button addButton = (Button) findViewById(R.id.btn_add);
        addButton.setText(R.string.btn_add_player);
        addButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showAddPlayerDialog();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        reload();
    }

    private void reload() {
        adapter.setData(repository.listPlayers());
        boolean empty = adapter.getCount() == 0;
        listView.setVisibility(empty ? View.GONE : View.VISIBLE);
        emptyText.setVisibility(empty ? View.VISIBLE : View.GONE);
    }

    private void showAddPlayerDialog() {
        final EditText input = new EditText(this);
        input.setHint(R.string.hint_player_name);

        new AlertDialog.Builder(this)
                .setTitle(R.string.btn_add_player)
                .setView(input)
                .setPositiveButton(R.string.btn_save, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String name = input.getText().toString().trim();
                        if (TextUtils.isEmpty(name)) {
                            Toast.makeText(PlayersActivity.this, R.string.error_empty_name, Toast.LENGTH_SHORT).show();
                            return;
                        }
                        repository.addPlayer(name);
                        reload();
                    }
                })
                .setNegativeButton(R.string.btn_cancel, null)
                .show();
    }

    private void confirmDeletePlayer(final Player player) {
        new AlertDialog.Builder(this)
                .setMessage(R.string.confirm_delete_player)
                .setPositiveButton(R.string.btn_delete, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        boolean deleted = repository.deletePlayer(player.id);
                        if (!deleted) {
                            Toast.makeText(PlayersActivity.this, R.string.error_delete_player_has_history, Toast.LENGTH_LONG).show();
                        }
                        reload();
                    }
                })
                .setNegativeButton(R.string.btn_cancel, null)
                .show();
    }

    private final class PlayerAdapter extends BaseAdapter {
        private List<Player> players;

        void setData(List<Player> players) {
            this.players = players;
            notifyDataSetChanged();
        }

        @Override
        public int getCount() {
            return players == null ? 0 : players.size();
        }

        @Override
        public Object getItem(int position) {
            return players.get(position);
        }

        @Override
        public long getItemId(int position) {
            return players.get(position).id;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view = convertView;
            if (view == null) {
                view = LayoutInflater.from(PlayersActivity.this).inflate(R.layout.item_player, parent, false);
            }
            final Player player = players.get(position);
            ((TextView) view.findViewById(R.id.text_name)).setText(player.name);
            view.findViewById(R.id.btn_delete).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    confirmDeletePlayer(player);
                }
            });
            return view;
        }
    }
}
