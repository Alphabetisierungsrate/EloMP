package com.elomp.app.data;

import java.util.List;
import java.util.Set;

public final class MatchEntry {
    public final long id;
    public final long rankingId;
    public final long timestampMillis;
    public final List<Long> participantIds;
    public final Set<Long> winnerIds;

    public MatchEntry(long id, long rankingId, long timestampMillis, List<Long> participantIds, Set<Long> winnerIds) {
        this.id = id;
        this.rankingId = rankingId;
        this.timestampMillis = timestampMillis;
        this.participantIds = participantIds;
        this.winnerIds = winnerIds;
    }
}
