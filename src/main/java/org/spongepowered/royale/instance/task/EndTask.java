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
package org.spongepowered.royale.instance.task;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.LinearComponents;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import org.spongepowered.api.effect.particle.ParticleEffect;
import org.spongepowered.api.effect.particle.ParticleTypes;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.scheduler.ScheduledTask;
import org.spongepowered.royale.Royale;
import org.spongepowered.royale.instance.Instance;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class EndTask extends InstanceTask {

    private final List<UUID> winners;
    private final long endLengthTotal;

    private ScheduledTask handle;
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
    public void accept(ScheduledTask task) {

        this.handle = task;

        if (this.winners.isEmpty()) {
            return;
        }

        // First tick, kickoff end sequence
        if (this.endLengthTotal == this.endLengthRemaining) {

            final Optional<ServerPlayer> winner;
            if (this.winners.size() > 1) {
                winner = Optional.empty();
            } else {
                winner = this.getInstance().getServer().getPlayer(this.winners.get(0));
            }

            final Component content =
                    winner.map(p -> LinearComponents.linear(NamedTextColor.GREEN, p.displayName().get(),
                            NamedTextColor.WHITE, TextComponent.of(" is the winner!")))
                            .orElseGet(() -> TextComponent.of("Draw", NamedTextColor.YELLOW));

            final Component winnerContent = winner.isPresent() ?
                    TextComponent.of("You are the winner!", NamedTextColor.GREEN) :
                    TextComponent.of("Draw", NamedTextColor.YELLOW);

            this.title = Title.of(content, TextComponent.empty(),
                    Title.Times.of(Duration.ZERO, Duration.ofSeconds(this.endLengthTotal - 1), Duration.ofSeconds(1)));

            this.winnerTitle = Title.of(winnerContent, TextComponent.empty(),
                    Title.Times.of(Duration.ZERO, Duration.ofSeconds(this.endLengthTotal - 1), Duration.ofSeconds(1)));

            if (winner.isPresent()) {
                winner.get().spawnParticles(ParticleEffect.builder()
                                .type(ParticleTypes.FIREWORKS_SPARK)
                                .quantity(30)
                                .build(),
                        winner.get().getLocation().getPosition());

                this.getInstance().getWorld().ifPresent((world) -> {
                    if (world.isLoaded()) {
                        for (final ServerPlayer player : world.getPlayers()) {
                            if (player.getUniqueId().equals(winner.get().getUniqueId())) {
                                player.showTitle(this.winnerTitle);
                            } else {
                                player.showTitle(title);
                            }
                        }
                    }
                });

                this.getInstance().getServer().getBroadcastAudience()
                        .sendMessage(LinearComponents.linear(NamedTextColor.GREEN, winner.get().displayName().get(),
                                NamedTextColor.WHITE, TextComponent.of(" has won the game!")));
                Royale.instance.getPlugin().getLogger().info("Round finished!");
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
