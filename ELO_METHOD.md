# Rating method

EloMP tracks a separate Elo-style rating per game ("ranking"), plus one
combined rating across all games. This document explains why a plain 2-player
Elo update isn't enough for boardgame-night results, which method was chosen
instead, and where it comes from.

## The problem with plain Elo

Standard Elo (Elo, A.E. *The Rating of Chessplayers, Past and Present*,
1978) only defines a rating update for a match between exactly two players:

```
E_A = 1 / (1 + 10^((R_B − R_A) / 400))          # A's expected score
R_A' = R_A + K · (S_A − E_A)                    # S_A ∈ {0, 0.5, 1}
```

Boardgame-night results, however, regularly involve more than two players,
and the app only records **which group won and which group lost** (not a
full 1st/2nd/3rd/.../nth placement) — e.g. one winner out of five players in
a *Catan* game, or two winners out of four in a team game. Plain Elo has no
built-in notion of "won against three people at once."

## The chosen method: pairwise decomposition (Bradley–Terry)

The method implemented in `EloEngine` (`app/src/main/java/com/elomp/app/elo/EloEngine.java`)
treats every match as **all of its pairwise comparisons**, and applies the
ordinary Elo update to each pair independently:

- For every pair of participants `(a, b)` in a match, compute the usual Elo
  expected scores `E_a`, `E_b` from their ratings *before* the match.
- The actual pairwise score is `1` for a beating b (a is a winner, b isn't),
  `0` the other way round, and `0.5` if `a` and `b` are on the same side
  (both in the winning group, or both in the losing group) — since the app
  doesn't record any order *within* a group.
- Each player's total rating change for the match is the sum of their
  `(S − E)` terms over every pairing they were part of, scaled by
  `K / (n − 1)` (n = number of participants), so a big free-for-all doesn't
  swing ratings proportionally harder than a 1-on-1 game.
- All pairwise expectations are computed from the ratings **before** the
  match (a simultaneous update), so the result doesn't depend on the order
  the pairs happen to be processed in.

This "decompose an n-way result into pairwise comparisons" idea isn't
something invented for this app — it is the direct, standard consequence of
the **Bradley–Terry model** for paired comparisons:

> Bradley, R. A.; Terry, M. E. (1952). "Rank Analysis of Incomplete Block
> Designs: I. The Method of Paired Comparisons." *Biometrika*, 39(3/4),
> 324–345. doi:[10.2307/2334029](https://doi.org/10.2307/2334029)

Bradley–Terry shows that a comparison among more than two competitors can be
modelled as a set of independent pairwise win probabilities of exactly the
same logistic form Elo already uses — which is what justifies applying Elo's
own update rule pairwise instead of inventing a new formula. The same
technique (pairwise decomposition + K divided across the n−1 opponents in a
match) is used in practice by public projects such as
[multielo](https://github.com/djcunningham0/multielo) (D. Cunningham, 2020),
which documents this exact generalization for free-for-all games.

**Why not something fancier, like TrueSkill?** Microsoft's
[TrueSkill](https://www.microsoft.com/en-us/research/publication/trueskill-a-bayesian-skill-rating-system/)
(Herbrich, Minka & Graepel, NeurIPS 2006) is a Bayesian alternative purpose-built
for multiplayer ranked matches and handles uncertainty/placement more richly.
It was considered, but it requires Gaussian-factor-graph message passing
(moment matching, `erf`, etc.) that's a lot more machinery for a casual
boardgame-night tracker, for a result (rank order of a handful of players)
that the simpler pairwise-Elo approach already models well. If the group
ever wants richer placement-aware ratings (not just "won"/"lost" groups),
TrueSkill would be the natural upgrade path.

## Per-game rating

- Every ranking (one per game) starts every player at **1500**.
- Ratings are recomputed by replaying that ranking's match entries in
  chronological order — so deleting an old entry correctly recalculates
  everyone's current rating, it isn't a separately-stored running total that
  could drift out of sync.
- `K = 32` (the standard default used for most Elo implementations).

## Total ranking across all games

A player's **Total Elo** combines every ranking they've appeared in:

```
Total = 1500 + average( per-game rating − 1500 )   over every game played
```

Averaging (rather than summing) the per-game deviation from the 1500
baseline means the total reflects overall strength *relative to the group*
in each game, rather than just rewarding whoever has played the most
different games.

## Known simplifications

- A "win" only means "beat everyone in the losing group"; the app doesn't
  record fine-grained placement (1st vs. 2nd vs. 3rd) within a multi-player
  free-for-all, only "the winners" vs. "everyone else." Marking multiple
  players as winners in the same match models a tie for first (or a team
  win); marking every participant as a winner models a full draw.
- Deleting a match entry always recomputes the whole ranking from scratch
  from remaining entries — correct, but potentially slow for very large
  histories (not a concern at boardgame-night scale).
