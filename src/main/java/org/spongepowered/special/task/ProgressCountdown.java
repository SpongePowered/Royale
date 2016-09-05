package org.spongepowered.special.task;

import com.google.common.collect.ImmutableMap;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.boss.BossBarColors;
import org.spongepowered.api.boss.BossBarOverlays;
import org.spongepowered.api.boss.ServerBossBar;
import org.spongepowered.api.effect.particle.ParticleEffect;
import org.spongepowered.api.effect.particle.ParticleTypes;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.scheduler.Task;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.world.World;
import org.spongepowered.special.Special;
import org.spongepowered.special.map.MapType;

import java.util.UUID;

public class ProgressCountdown extends RoundCountdown {
    final ServerBossBar bossBar = ServerBossBar.builder()
            .color(BossBarColors.GREEN)
            .playEndBossMusic(false)
            .visible(true)
            .overlay(BossBarOverlays.PROGRESS)
            .name(Text.of("Time remaining"))
            .build();
    final long roundLengthTotal;
    long roundLengthRemaining;

    public ProgressCountdown(MapType mapType, World world) {
        super(mapType, world);

        this.roundLengthTotal = mapType.getRoundEndLength();
        this.roundLengthRemaining = this.roundLengthTotal;
    }

    @Override
    public void run() {
        final World world = this.getWorldRef().get();

        // Make sure the world is still around and loaded
        if (world != null && world.isLoaded()) {
            final float percent = (float) this.roundLengthRemaining * 1f / this.roundLengthTotal;
            this.bossBar.setPercent(percent);

            if (percent < 0.33) {
                this.bossBar.setColor(BossBarColors.RED);
            } else if (percent < 0.66) {
                this.bossBar.setColor(BossBarColors.YELLOW);
            } else {
                this.bossBar.setColor(BossBarColors.GREEN);
            }

            // Make sure a player ref isn't still here
            world.getPlayers().stream().filter(player -> player.isOnline() && !bossBar.getPlayers().contains(player))
                    .forEach(bossBar::addPlayer);

            this.roundLengthRemaining--;
            if (this.roundLengthRemaining < 0) {
                final UUID taskUniqueId =
                        Special.instance.getMapManager().getEndTaskUniqueIdFor(world).orElseThrow(() -> new RuntimeException("Task is "
                                + "executing when manager has no knowledge of it!"));
                if (taskUniqueId != null) {
                    this.bossBar.getPlayers().forEach(this.bossBar::removePlayer);
                    Sponge.getScheduler().getTaskById(taskUniqueId).ifPresent(Task::cancel);
                }
            }
        }
    }
}
