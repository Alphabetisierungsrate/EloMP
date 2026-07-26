package com.elomp.app.ui;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.elomp.app.R;
import com.elomp.app.data.Player;
import com.elomp.app.data.Repository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ResultEntryActivity extends Activity {

    private Repository repository;
    private long rankingId;

    private LinearLayout winnersContainer;
    private final Map<Long, CheckBox> winnerCheckboxes = new HashMap<>();
    private Map<Long, String> nameById;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result_entry);
        repository = new Repository(this);

        rankingId = getIntent().getLongExtra(RankingsActivity.EXTRA_RANKING_ID, -1);

        List<Player> players = repository.listPlayers();
        if (players.size() < 2) {
            Toast.makeText(this, R.string.error_need_two_players, Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        nameById = new HashMap<>();
        for (Player p : players) {
            nameById.put(p.id, p.name);
        }

        LinearLayout participantsContainer = (LinearLayout) findViewById(R.id.participants_container);
        winnersContainer = (LinearLayout) findViewById(R.id.winners_container);

        for (final Player player : players) {
            final CheckBox checkBox = new CheckBox(this);
            checkBox.setText(player.name);
            checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    onParticipantToggled(player, isChecked);
                }
            });
            participantsContainer.addView(checkBox);
        }

        ((Button) findViewById(R.id.btn_save)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                save();
            }
        });
    }

    private void onParticipantToggled(Player player, boolean isChecked) {
        if (isChecked) {
            CheckBox winnerCheckBox = new CheckBox(this);
            winnerCheckBox.setText(player.name);
            winnersContainer.addView(winnerCheckBox);
            winnerCheckboxes.put(player.id, winnerCheckBox);
        } else {
            CheckBox winnerCheckBox = winnerCheckboxes.remove(player.id);
            if (winnerCheckBox != null) {
                winnersContainer.removeView(winnerCheckBox);
            }
        }
    }

    private void save() {
        List<Long> participantIds = new ArrayList<>(winnerCheckboxes.keySet());
        if (participantIds.size() < 2) {
            Toast.makeText(this, R.string.error_need_two_participants, Toast.LENGTH_SHORT).show();
            return;
        }

        Set<Long> winnerIds = new HashSet<>();
        for (Map.Entry<Long, CheckBox> entry : winnerCheckboxes.entrySet()) {
            if (entry.getValue().isChecked()) {
                winnerIds.add(entry.getKey());
            }
        }
        if (winnerIds.isEmpty()) {
            Toast.makeText(this, R.string.error_need_one_winner, Toast.LENGTH_SHORT).show();
            return;
        }

        repository.addMatchEntry(rankingId, System.currentTimeMillis(), participantIds, winnerIds);
        finish();
    }
}
