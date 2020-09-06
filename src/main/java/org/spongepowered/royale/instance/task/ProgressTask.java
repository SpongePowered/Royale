/*
 * This file is part of Royale, licensed under the MIT License (MIT).
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
package org.spongepowered.royale.instance.task;

import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.TextComponent;
import org.spongepowered.api.scheduler.ScheduledTask;
import org.spongepowered.api.world.server.ServerWorld;
import org.spongepowered.royale.instance.Instance;

import java.util.stream.Collectors;

public final class ProgressTask extends InstanceTask {

    private final BossBar bossBar = BossBar.of(
            TextComponent.of("Time remaining"),
            0.0f,
            BossBar.Color.GREEN,
            BossBar.Overlay.PROGRESS);

    private final long roundLengthTotal;
    private long roundLengthRemaining;

    private ScheduledTask handle;

    public ProgressTask(final Instance instance) {
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
    public void accept(ScheduledTask task) {
        this.handle = task;

        final ServerWorld world = this.getInstance().getWorld().orElse(null);

        // Make sure the world is still around and loaded
        if (world != null && world.isLoaded()) {
            if (this.getInstance().getState() == Instance.State.POST_START) {
                this.getInstance().advance();
            }

            final float percent = (float) this.roundLengthRemaining * 1f / this.roundLengthTotal;
            this.bossBar.percent(percent);

            if (percent < 0.33) {
                this.bossBar.color(BossBar.Color.RED);
            } else if (percent < 0.66) {
                this.bossBar.color(BossBar.Color.YELLOW);
            } else {
                this.bossBar.color(BossBar.Color.GREEN);
            }

            int seconds = (int) this.roundLengthRemaining % 60;
            if (this.roundLengthRemaining > 60) {
                int minutes = (int) this.roundLengthRemaining / 60;
                this.bossBar.name(TextComponent.of(String.format("Time remaining: %02d:%02d", minutes, seconds)));
            } else {
                this.bossBar.name(TextComponent.of(String.format("Time remaining: %02d", seconds)));
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
