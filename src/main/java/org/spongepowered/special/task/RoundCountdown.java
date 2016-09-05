package org.spongepowered.special.task;

import org.spongepowered.api.Sponge;
import org.spongepowered.api.boss.BossBarColors;
import org.spongepowered.api.boss.BossBarOverlays;
import org.spongepowered.api.boss.ServerBossBar;
import org.spongepowered.api.effect.particle.ParticleEffect;
import org.spongepowered.api.effect.particle.ParticleTypes;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.scheduler.Task;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.text.title.Title;
import org.spongepowered.api.world.World;
import org.spongepowered.special.Special;
import org.spongepowered.special.map.MapType;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.UUID;

public abstract class RoundCountdown implements Runnable {

    private final MapType mapType;
    private final WeakReference<World> worldRef;

    public RoundCountdown(MapType mapType, World world) {
        this.mapType = mapType;
        this.worldRef = new WeakReference<>(world);
    }

    public final class Start extends RoundCountdown {

        private final ArrayList<Title> startTitles = new ArrayList<>();

        int seconds = 0;

        public Start(MapType mapType, World world) {
            super(mapType, world);

            final Title.Builder builder = Title.builder()
                    .stay(12)
                    .fadeIn(0)
                    .fadeOut(8);

            for (long i = mapType.getRoundStartLength(); i > 0; i--) {

                startTitles.add(builder.title(Text.of(TextColors.DARK_RED, i)).build());

                if (i == 2) {
                    startTitles.add(builder.title(Text.of(TextColors.RED, "2")).build());
                    startTitles.add(builder.title(Text.of(TextColors.GOLD, "1")).build());
                    startTitles.add(builder.title(mapType.getRoundStartTemplate().apply().build()).build());
                    break;
                }
            }
        }

        @Override
        public void run() {
            final World world = worldRef.get();

            // Make sure the world is still around and loaded
            if (world != null && world.isLoaded()) {

                // Make sure a player ref isn't still here
                world.getPlayers().stream().filter(User::isOnline).forEach(player -> {
                    player.sendTitle(startTitles.get(seconds));
                });

                seconds++;

                if (seconds >= startTitles.size()) {
                    final UUID taskUniqueId =
                            Special.instance.getMapManager().getStartTaskUniqueIdFor(world).orElseThrow(() -> new RuntimeException("Task is "
                                    + "executing when manager has no knowledge of it!"));
                    if (taskUniqueId != null) {
                        Sponge.getScheduler().getTaskById(taskUniqueId).ifPresent(Task::cancel);
                    }
                }
            }
        }
    }

    public final class End extends RoundCountdown {

        final ServerBossBar.Builder bossBarTemplate = ServerBossBar.builder()
                .color(BossBarColors.GREEN)
                .playEndBossMusic(false)
                .visible(true)
                .overlay(BossBarOverlays.PROGRESS);
        final int maxTicks = 300;
        int ticks = 300;

        public End(MapType mapType, World world, Player player) {
            super(mapType, world);

            bossBarTemplate.name(player.getDisplayNameData().displayName().get());

            player.spawnParticles(ParticleEffect.builder()
                                    .type(ParticleTypes.FIREWORKS_SPARK)
                                    .count(30)
                                    .build(),
                    player.getLocation().getPosition());
        }

        @Override
        public void run() {
            final World world = worldRef.get();
            final ServerBossBar bossBar = bossBarTemplate.build();

            // Make sure the world is still around and loaded
            if (world != null && world.isLoaded()) {
                bossBar.setPercent((float) ticks * 1f / maxTicks);
                switch (ticks) {
                    case 200:
                        bossBar.setColor(BossBarColors.YELLOW);
                        break;
                    case 100:
                        bossBar.setColor(BossBarColors.RED);
                        break;
                }
                // Make sure a player ref isn't still here
                world.getPlayers().stream().filter(player -> player.isOnline() && !bossBar.getPlayers().contains(player))
                        .forEach(bossBar::addPlayer);

                ticks--;
                if (ticks < 0) {
                    final UUID taskUniqueId =
                            Special.instance.getMapManager().getEndTaskUniqueIdFor(world).orElseThrow(() -> new RuntimeException("Task is "
                                    + "executing when manager has no knowledge of it!"));
                    if (taskUniqueId != null) {
                        bossBar.getPlayers().forEach(bossBar::removePlayer);
                        Sponge.getScheduler().getTaskById(taskUniqueId).ifPresent(Task::cancel);
                    }
                }
            }
        }
    }
}