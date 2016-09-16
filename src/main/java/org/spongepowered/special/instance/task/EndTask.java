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

import org.spongepowered.api.Sponge;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.effect.particle.ParticleEffect;
import org.spongepowered.api.effect.particle.ParticleTypes;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.scheduler.Task;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.text.title.Title;
import org.spongepowered.special.instance.Instance;

import java.util.Set;
import java.util.UUID;

public final class EndTask extends RoundTask {

    private final Set<UUID> winners;
    private final long endLengthTotal;

    private Task task;
    private Title title;
    private long endLengthRemaining;

    public EndTask(Instance instance, Set<UUID> winners) {
        super(instance);
        this.winners = winners;
        this.endLengthTotal = instance.getType().getRoundEndLength();
        this.endLengthRemaining = this.endLengthTotal;
    }

    @Override
    public void accept(Task task) {
        // First tick, kickoff end sequence
        if (this.endLengthTotal == this.endLengthRemaining) {
            this.task = task;

            // TODO Remove this stupid code
            Player winner = null;
            for (UUID uuid : winners) {
                winner = Sponge.getServer().getPlayer(uuid).orElse(null);
            }

            // TODO Handle multiple winners
            if (winner != null) {
                this.title = Title.builder()
                        .stay((int) ((this.endLengthTotal - 1) * 20))
                        .fadeIn(0)
                        .fadeOut(20)
                        .title(Text.of(TextColors.YELLOW, winner.get(Keys.DISPLAY_NAME).get(), TextColors.WHITE, " is the winner!"))
                        .build();

                winner.spawnParticles(ParticleEffect.builder()
                                .type(ParticleTypes.FIREWORKS_SPARK)
                                .count(30)
                                .build(),
                        winner.getLocation().getPosition());

                getInstance().getHandle().ifPresent((world) -> {
                    if (world.isLoaded()) {
                        world.getPlayers().stream().filter(User::isOnline).forEach(onlinePlayer -> onlinePlayer.sendTitle(title));
                    }
                });
            }
        }

        this.endLengthRemaining--;
        if (this.endLengthRemaining < 0) {
            this.cancel();
            this.getInstance().advance();
        }
    }

    @Override
    public void cancel() {
        this.task.cancel();
    }
}
