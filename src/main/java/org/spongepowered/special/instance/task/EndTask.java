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
import org.spongepowered.special.Special;
import org.spongepowered.special.instance.Instance;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class EndTask extends InstanceTask {

    private final List<UUID> winners;
    private final long endLengthTotal;

    private Task handle;
    private Title title;
    private Title winnerTitle;
    private long endLengthRemaining;

    public EndTask(Instance instance, List<UUID> winners) {
        super(instance);
        this.winners = winners;
        this.endLengthTotal = instance.getType().getRoundEndLength();
        this.endLengthRemaining = this.endLengthTotal;
    }

    @Override
    public void accept(Task task) {

        this.handle = task;

        if (winners.isEmpty()) {
            return;
        }

        // First tick, kickoff end sequence
        if (this.endLengthTotal == this.endLengthRemaining) {

            final Optional<Player> winner;
            if (this.winners.size() > 1) {
                winner = Optional.empty();
            } else {
                winner = Sponge.getServer().getPlayer(winners.get(0));
            }

            final Text content = winner.isPresent() ?
                    Text.of(TextColors.GREEN, winner.get().get(Keys.DISPLAY_NAME).get(), TextColors.WHITE, " is the winner!") :
                    Text.of(TextColors.YELLOW, "Draw");

            final Text winnerContent = winner.isPresent() ?
                    Text.of(TextColors.GREEN, "You are the winner!") :
                    Text.of(TextColors.YELLOW, "Draw");

            this.title = Title.builder()
                    .fadeIn(0)
                    .fadeOut(20)
                    .stay((int) ((this.endLengthTotal - 1) * 20))
                    .title(content)
                    .build();

            this.winnerTitle = Title.builder()
                    .fadeIn(0)
                    .fadeOut(20)
                    .stay((int) ((this.endLengthTotal - 1) * 20))
                    .title(winnerContent)
                    .build();

            if (winner.isPresent()) {
                winner.get().spawnParticles(ParticleEffect.builder()
                                .type(ParticleTypes.FIREWORKS_SPARK)
                                .count(30)
                                .build(),
                        winner.get().getLocation().getPosition());

                getInstance().getHandle().ifPresent((world) -> {
                    if (world.isLoaded()) {
                        world.getPlayers().stream().filter(User::isOnline).forEach(onlinePlayer -> {
                            if (onlinePlayer.getUniqueId().equals(winner.get().getUniqueId())) {
                                onlinePlayer.sendTitle(this.winnerTitle);
                            } else {
                                onlinePlayer.sendTitle(title);
                            }
                        });
                    }
                });

                Sponge.getServer().getBroadcastChannel()
                        .send(Text.of(TextColors.RED, winner.get().getName(), TextColors.RESET, " has won the game!"));
                Special.instance.getLogger().info("Round finished!");
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
        this.handle.cancel();
    }
}
