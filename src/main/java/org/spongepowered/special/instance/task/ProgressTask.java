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
package org.spongepowered.special.instance.task;

import org.spongepowered.api.boss.BossBarColors;
import org.spongepowered.api.boss.BossBarOverlays;
import org.spongepowered.api.boss.ServerBossBar;
import org.spongepowered.api.scheduler.Task;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.world.World;
import org.spongepowered.special.instance.Instance;

import java.util.stream.Collectors;

public final class ProgressTask extends InstanceTask {

    private final ServerBossBar bossBar = ServerBossBar.builder()
            .color(BossBarColors.GREEN)
            .playEndBossMusic(false)
            .visible(true)
            .overlay(BossBarOverlays.PROGRESS)
            .name(Text.of("Time remaining"))
            .build();

    private final long roundLengthTotal;
    private long roundLengthRemaining;

    private Task handle;

    public ProgressTask(Instance instance) {
        super(instance);

        this.roundLengthTotal = instance.getType().getRoundLength();
        this.roundLengthRemaining = this.roundLengthTotal;
    }

    @Override
    public void cancel() {
        this.bossBar.removePlayers(this.bossBar.getPlayers());
        this.handle.cancel();
    }

    @Override
    public void accept(Task task) {
        this.handle = task;

        final World world = this.getInstance().getHandle().orElse(null);

        // Make sure the world is still around and loaded
        if (world != null && world.isLoaded()) {
            if (this.getInstance().getState() == Instance.State.POST_START) {
                this.getInstance().advance();
            }

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
                this.bossBar.setName(Text.of(String.format("Time remaining: %02d:%02d", minutes, seconds)));
            } else {
                this.bossBar.setName(Text.of(String.format("Time remaining: %02d", seconds)));
            }

            this.bossBar.addPlayers(world.getPlayers().stream().filter(player -> player.isOnline() && !this.bossBar.getPlayers().contains(player))
                    .collect(Collectors.toList()));

            this.roundLengthRemaining--;
            if (this.roundLengthRemaining < 0) {
                this.cancel();
                this.getInstance().advance();
            }
        }
    }
}
