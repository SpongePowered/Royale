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
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.block.BlockTypes;
import org.spongepowered.api.data.Keys;
import org.spongepowered.api.data.type.HandTypes;
import org.spongepowered.api.effect.potion.PotionEffect;
import org.spongepowered.api.effect.potion.PotionEffectTypes;
import org.spongepowered.api.entity.EntityTypes;
import org.spongepowered.api.entity.ai.goal.GoalExecutor;
import org.spongepowered.api.entity.ai.goal.GoalExecutorTypes;
import org.spongepowered.api.entity.ai.goal.builtin.LookAtGoal;
import org.spongepowered.api.entity.ai.goal.builtin.LookRandomlyGoal;
import org.spongepowered.api.entity.ai.goal.builtin.SwimGoal;
import org.spongepowered.api.entity.ai.goal.builtin.creature.AttackLivingGoal;
import org.spongepowered.api.entity.ai.goal.builtin.creature.RandomWalkingGoal;
import org.spongepowered.api.entity.ai.goal.builtin.creature.RangedAttackAgainstAgentGoal;
import org.spongepowered.api.entity.ai.goal.builtin.creature.target.FindNearestAttackableTargetGoal;
import org.spongepowered.api.entity.living.Agent;
import org.spongepowered.api.entity.living.Human;
import org.spongepowered.api.entity.living.monster.Silverfish;
import org.spongepowered.api.entity.living.monster.guardian.Guardian;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.item.ItemTypes;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.scheduler.ScheduledTask;
import org.spongepowered.api.world.explosion.Explosion;
import org.spongepowered.api.world.server.ServerLocation;
import org.spongepowered.api.world.server.ServerWorld;
import org.spongepowered.math.vector.Vector3d;
import org.spongepowered.royale.instance.InstanceImpl;

import java.time.Duration;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

public final class CleanupTask extends InstanceTask {

    private final BossBar bossBar = BossBar.bossBar(
            Component.text("OVERTIME!"),
            0.0f,
            BossBar.Color.RED,
            BossBar.Overlay.PROGRESS);

    private final Title title = Title.title(
            Component.text("Survive!", NamedTextColor.RED),
            Component.empty(),
            Title.Times.of(Duration.ZERO, Duration.ofSeconds(1), Duration.ofSeconds(2)));

    private ScheduledTask handle;
    private long duration = 0;

    private final Set<UUID> bossBarViewers = new HashSet<>();

    private final long roundLengthTotal;
    private long roundLengthRemaining;
    private final Random random = new Random();

    public CleanupTask(final InstanceImpl instance) {
        super(instance);
        this.roundLengthTotal = 150;
        this.roundLengthRemaining = this.roundLengthTotal;
    }

    @Override
    public void accept(final ScheduledTask task) {
        this.handle = task;

        final ServerWorld world =
                this.getInstance().world();

        final TextComponent append = Component.text("OVERTIME!", NamedTextColor.RED)
                .append(Component.space())
                .append(Component.text(this.getInstance().playersLeft(), NamedTextColor.GOLD))
                .append(Component.text(" Players left", NamedTextColor.RED));
        this.bossBar.name(append);
        final float percent = (float) this.roundLengthRemaining * 1f / this.roundLengthTotal;
        this.bossBar.progress(Math.min(percent, 1));

        if (world != null && world.isLoaded()) {

            for (final ServerPlayer player : world.players()) {
                if (this.bossBarViewers.add(player.uniqueId())) {
                    player.showBossBar(this.bossBar);
                }
                if (!this.getInstance().isPlayerAlive(player)) {
                    continue;
                }

                if (this.duration < 150) {
                    if (this.duration == 0) {
                        player.showTitle(this.title);
                    }

                    this.spawnCleanupCrew(world, random, player);
                } else {
                    final ServerLocation explosionLocation = player.serverLocation().add(random.nextInt(4), random.nextInt(4), random.nextInt(4));

                    world.triggerExplosion(Explosion.builder()
                            .canCauseFire(true)
                            .shouldBreakBlocks(true)
                            .shouldPlaySmoke(true)
                            .radius(8)
                            .location(explosionLocation)
                            .build());
                }
            }
        }

        this.duration++;
        this.roundLengthRemaining --;
    }

    private void spawnCleanupCrew(ServerWorld world, Random random, ServerPlayer player) {
        final ServerLocation playerLocaction = player.serverLocation();
        final Vector3d location = playerLocaction.position();

        ServerLocation spawnLocation = null;

        Human human = world.createEntity(EntityTypes.HUMAN.get(), location);
        Guardian guardian = world.createEntity(EntityTypes.GUARDIAN.get(), location);

        // 4 tries to spawn
        int tries = 4;

        boolean waterSpawn = false;
        while (tries > 0) {
            spawnLocation = Sponge.server().teleportHelper().findSafeLocation(
                    playerLocaction.add((random.nextInt(4) + 5) * (random.nextBoolean() ? 1 : -1), 0,
                                        (random.nextInt(4) + 5) * (random.nextBoolean() ? 1 : -1)) , 3, 3).orElse(null);

            if (spawnLocation != null) {
                waterSpawn = playerLocaction.blockType().isAnyOf(BlockTypes.WATER);
                if (waterSpawn) {
                    guardian.setLocation(spawnLocation);
                    human.remove();
                    if (world.entities(guardian.boundingBox().get()).isEmpty()) {
                        tries--;
                        continue;
                    }
                } else {
                    human.setLocation(spawnLocation);
                    guardian.remove();
                    if (world.entities(human.boundingBox().get()).isEmpty()) {
                        tries--;
                        continue;
                    }
                }
            }

            tries--;
        }

        if (spawnLocation != null) {
            if (waterSpawn) {
                world.spawnEntity(guardian);
            } else {
                world.spawnEntity(this.customizeHuman(random, human));
            }
        } else {
            // someone tried to be smart
            final Silverfish silverfish = world.createEntity(EntityTypes.SILVERFISH, location);
            silverfish.offer(Keys.HEALTH, 200.0);
            silverfish.offer(Keys.POTION_EFFECTS, Arrays.asList(PotionEffect.of(PotionEffectTypes.POISON, 1, 100)));
            world.spawnEntity(silverfish);
        }
    }

    private Human customizeHuman(Random random, Human human) {
        final GoalExecutor<Agent> targetGoal = human.goal(GoalExecutorTypes.TARGET.get()).orElse(null);
        targetGoal.addGoal(0, FindNearestAttackableTargetGoal.builder().chance(1).target(ServerPlayer.class)
                .filter(e -> this.getInstance().isPlayerAlive((ServerPlayer) e)).build(human));

        final GoalExecutor<Agent> normalGoal = human.goal(GoalExecutorTypes.NORMAL.get()).orElse(null);
        normalGoal.addGoal(0, SwimGoal.builder().swimChance(0.8f).build(human));

        final float rangerChance = random.nextFloat();

        boolean ranger = false;

        if (rangerChance < 0.3f) {
            normalGoal.addGoal(1,
                    RangedAttackAgainstAgentGoal.builder().moveSpeed(7.75).attackRadius(15f).delayBetweenAttacks(65).build(
                            human));
            human.setItemInHand(HandTypes.MAIN_HAND, ItemStack.of(ItemTypes.BOW, 1));
            final ItemStack tipped = ItemStack.of(ItemTypes.TIPPED_ARROW, 1);
            tipped.offer(Keys.POTION_EFFECTS, Arrays.asList(
                    PotionEffect.of(PotionEffectTypes.GLOWING.get(), 1, 60),
                    PotionEffect.of(PotionEffectTypes.SLOWNESS.get(), 1, 60)));

            human.setItemInHand(HandTypes.OFF_HAND, tipped);
            ranger = true;
        } else {
            normalGoal.addGoal(1, AttackLivingGoal.builder().longMemory().speed(8.25).build(human));
            human.setItemInHand(HandTypes.MAIN_HAND, ItemStack.of(ItemTypes.DIAMOND_SWORD, 1));
        }

        normalGoal.addGoal(2, RandomWalkingGoal.builder().speed(6.5).build(human));
        normalGoal.addGoal(3, LookAtGoal.builder().maxDistance(8f).watch(ServerPlayer.class).build(human));
        normalGoal.addGoal(3, LookRandomlyGoal.builder().build(human));

        human.offer(Keys.CUSTOM_NAME, Component.text(ranger ? "Ranger" : "Swordsman",
                ranger ? NamedTextColor.GREEN : NamedTextColor.BLUE));
        return human;
    }

    @Override
    public boolean cancel() {
        for (final UUID profile : this.bossBarViewers) {
            Sponge.server().player(profile).ifPresent(p -> p.hideBossBar(this.bossBar));
        }
        return this.handle.cancel();
    }
}
