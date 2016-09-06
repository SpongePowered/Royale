/**
 * This file is part of Special, licensed under the MIT License (MIT).
 *
 * Copyright (c) SpongePowered <http://github.com/SpongePowered>
 * Copyright (c) contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.spongepowered.special.task;

import org.spongepowered.api.Sponge;
import org.spongepowered.api.boss.BossBarColors;
import org.spongepowered.api.boss.BossBarOverlays;
import org.spongepowered.api.boss.ServerBossBar;
import org.spongepowered.api.scheduler.Task;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.world.World;
import org.spongepowered.special.Special;
import org.spongepowered.special.map.MapType;

import java.text.DecimalFormat;
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

            int seconds = (int) this.roundLengthRemaining % 60;
            if (this.roundLengthRemaining > 60) {
                int minutes = (int) this.roundLengthRemaining / 60;
                this.bossBar.setName(Text.of(String.format("Time remaining: %02d:%s", minutes, seconds));
            } else {
                this.bossBar.setName(Text.of(String.format("Time remaining: %s", seconds)));
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
