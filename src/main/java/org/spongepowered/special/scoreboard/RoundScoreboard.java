package org.spongepowered.special.scoreboard;

import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.scoreboard.Score;
import org.spongepowered.api.scoreboard.Scoreboard;
import org.spongepowered.api.scoreboard.Team;
import org.spongepowered.api.scoreboard.critieria.Criteria;
import org.spongepowered.api.scoreboard.objective.Objective;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.text.format.TextStyles;
import org.spongepowered.special.map.Map;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class RoundScoreboard {

    private Scoreboard scoreboard;
    private Objective objective;
    private java.util.Map<UUID, PlayerData> playerData = new HashMap<>();

    public RoundScoreboard(Map map) {
        this.scoreboard = Scoreboard.builder().build();
        this.objective = Objective.builder().name("main").displayName(Text.of(TextColors.YELLOW, "Players")).criterion(Criteria.DUMMY).build();
    }

    public void addPlayer(Player player) {
        Score score = this.objective.getOrCreateScore(Text.of(player.getName()));
        score.setScore(0);

        Team team = Team.builder().name(player.getUniqueId().toString()).build();
        team.addMember(player.getTeamRepresentation());
        this.scoreboard.registerTeam(team);

        this.playerData.put(player.getUniqueId(), new PlayerData(score, team));
        player.setScoreboard(this.scoreboard);

        this.sortScoreboard();
    }

    public void killPlayer(Player player) {
        if (!this.playerData.containsKey(player.getUniqueId())) {
            throw new IllegalArgumentException(String.format("Player %s is not on this scoreboard!", player.getName()));
        }
        PlayerData data = this.playerData.get(player.getUniqueId());
        data.team.setPrefix(Text.of(TextStyles.STRIKETHROUGH, ""));
        data.dead = true;

        this.sortScoreboard();
    }

    private void sortScoreboard() {
        List<Player> alive = new ArrayList<>();
        List<Player> dead = new ArrayList<>();

        for (java.util.Map.Entry<UUID, PlayerData> entry: this.playerData.entrySet()) {
            Player player = Sponge.getServer().getPlayer(entry.getKey()).orElseThrow(() -> new RuntimeException(String.format("No player with UUID %s found!", entry.getKey())));
            if (entry.getValue().dead) {
                dead.add(player);
            } else {
                alive.add(player);
            }
        }

        // Inverse alphabetical order
        Comparator<Player> comparator = (p1, p2) -> -p1.getName().compareTo(p2.getName());

        dead.sort(comparator);
        alive.sort(comparator);

        for (int i = 0; i < dead.size(); i++) {
            this.playerData.get(dead.get(i).getUniqueId()).score.setScore(i);
        }
        for (int i = 0; i < alive.size(); i++) {
            this.playerData.get(alive.get(i).getUniqueId()).score.setScore(dead.size() + i);
        }

    }

    private class PlayerData {
        Score score;
        Team team;
        boolean dead;

        public PlayerData(Score score, Team team) {
            this.score = score;
            this.team = team;
            this.dead = dead;
        }
    }

}
