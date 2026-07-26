package com.elomp.app.elo;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Multiplayer Elo rating engine.
 *
 * Standard Elo (Elo, A.E., "The Rating of Chessplayers, Past and Present", 1978)
 * only defines an update for a single pairwise match. A boardgame-night result
 * usually involves more than two players and only records "who won" vs.
 * "who lost" (not a full 1..n placement), so a plain 2-player Elo update does
 * not directly apply.
 *
 * The method used here decomposes every multiplayer match into all of its
 * pairwise comparisons and updates each pair exactly as in standard Elo. This
 * is the standard generalization of Elo to multiplayer/free-for-all results;
 * it rests on the Bradley-Terry paired-comparison model:
 *
 *   Bradley, R.A.; Terry, M.E. (1952). "Rank Analysis of Incomplete Block
 *   Designs: I. The Method of Paired Comparisons." Biometrika, 39(3/4),
 *   324-345. doi:10.2307/2334029
 *
 * which shows that a ranking over more than two competitors can be modelled
 * as a set of independent pairwise win probabilities of exactly the logistic
 * form Elo already uses. The same decomposition (with the per-match K-factor
 * divided across the n-1 pairings each player took part in, so a big
 * free-for-all doesn't swing ratings n times harder than a 1v1) is what the
 * open-source "multielo" project (D. Cunningham, multielo, 2020,
 * https://github.com/djcunningham0/multielo) and several public boardgame
 * rating trackers use in practice. See ELO_METHOD.md at the repo root for
 * the full writeup.
 */
public final class EloEngine {

    public static final double DEFAULT_RATING = 1500.0;
    public static final double K_FACTOR = 32.0;

    private EloEngine() {
    }

    /** One played match: a set of participants, a subset of which are the winners. */
    public static final class MatchResult {
        public final long timestampMillis;
        public final List<Long> participantIds;
        public final Set<Long> winnerIds;

        public MatchResult(long timestampMillis, List<Long> participantIds, Set<Long> winnerIds) {
            this.timestampMillis = timestampMillis;
            this.participantIds = participantIds;
            this.winnerIds = winnerIds;
        }
    }

    /**
     * Replays every match for one ranking (game) in chronological order and
     * returns each player's current rating. Players who never played default
     * to {@link #DEFAULT_RATING}.
     */
    public static Map<Long, Double> computeRatings(Collection<MatchResult> matches, Collection<Long> allPlayerIds) {
        Map<Long, Double> ratings = new HashMap<>();
        for (Long id : allPlayerIds) {
            ratings.put(id, DEFAULT_RATING);
        }

        List<MatchResult> sorted = new ArrayList<>(matches);
        Collections.sort(sorted, new Comparator<MatchResult>() {
            @Override
            public int compare(MatchResult a, MatchResult b) {
                return Long.compare(a.timestampMillis, b.timestampMillis);
            }
        });

        for (MatchResult match : sorted) {
            applyMatch(ratings, match);
        }
        return ratings;
    }

    private static void applyMatch(Map<Long, Double> ratings, MatchResult match) {
        List<Long> participants = match.participantIds;
        int n = participants.size();
        if (n < 2) {
            return;
        }

        Map<Long, Double> before = new HashMap<>();
        for (Long p : participants) {
            Double r = ratings.get(p);
            before.put(p, r == null ? DEFAULT_RATING : r);
        }

        Map<Long, Double> scoreMinusExpectedSum = new HashMap<>();
        for (Long p : participants) {
            scoreMinusExpectedSum.put(p, 0.0);
        }

        for (int i = 0; i < n; i++) {
            for (int j = i + 1; j < n; j++) {
                long a = participants.get(i);
                long b = participants.get(j);
                double ra = before.get(a);
                double rb = before.get(b);

                double expectedA = 1.0 / (1.0 + Math.pow(10.0, (rb - ra) / 400.0));
                double expectedB = 1.0 - expectedA;

                double actualA = pairwiseScore(a, b, match.winnerIds);
                double actualB = 1.0 - actualA;

                scoreMinusExpectedSum.put(a, scoreMinusExpectedSum.get(a) + (actualA - expectedA));
                scoreMinusExpectedSum.put(b, scoreMinusExpectedSum.get(b) + (actualB - expectedB));
            }
        }

        double perPairK = K_FACTOR / (n - 1);
        for (Long p : participants) {
            double updated = before.get(p) + perPairK * scoreMinusExpectedSum.get(p);
            ratings.put(p, updated);
        }
    }

    /**
     * Pairwise result between two participants of the same match:
     * 1.0 if a is in the winning group and b is not, 0.0 the other way round,
     * and 0.5 if a and b are on the same side (both won, or both lost) since
     * no relative order is recorded between them.
     */
    private static double pairwiseScore(long a, long b, Set<Long> winnerIds) {
        boolean aWon = winnerIds.contains(a);
        boolean bWon = winnerIds.contains(b);
        if (aWon == bWon) {
            return 0.5;
        }
        return aWon ? 1.0 : 0.0;
    }

    /**
     * Combines a player's ratings across every ranking (game) they've played
     * into one overall number: 1500 plus the average of their per-game
     * deviations from 1500. Averaging (rather than summing) means a player's
     * total ranking reflects overall strength relative to their peers in
     * each game, not just how many games they've played.
     */
    public static Map<Long, Double> computeTotalRatings(Map<Long, Map<Long, Double>> perRankingRatings) {
        Map<Long, Double> offsetSum = new HashMap<>();
        Map<Long, Integer> gameCount = new HashMap<>();

        for (Map<Long, Double> ratingsInRanking : perRankingRatings.values()) {
            for (Map.Entry<Long, Double> entry : ratingsInRanking.entrySet()) {
                long playerId = entry.getKey();
                double offset = entry.getValue() - DEFAULT_RATING;
                Double prevSum = offsetSum.get(playerId);
                offsetSum.put(playerId, (prevSum == null ? 0.0 : prevSum) + offset);
                Integer prevCount = gameCount.get(playerId);
                gameCount.put(playerId, (prevCount == null ? 0 : prevCount) + 1);
            }
        }

        Map<Long, Double> total = new HashMap<>();
        for (Map.Entry<Long, Double> entry : offsetSum.entrySet()) {
            long playerId = entry.getKey();
            int count = gameCount.get(playerId);
            total.put(playerId, DEFAULT_RATING + entry.getValue() / count);
        }
        return total;
    }
}
