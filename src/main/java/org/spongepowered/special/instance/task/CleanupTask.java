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

import com.flowpowered.math.vector.Vector3d;
import com.google.common.collect.Lists;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.data.Keys;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.data.type.HandTypes;
import org.spongepowered.api.effect.potion.PotionEffect;
import org.spongepowered.api.effect.potion.PotionEffectTypes;
import org.spongepowered.api.entity.EntityTypes;
import org.spongepowered.api.entity.ai.Goal;
import org.spongepowered.api.entity.ai.GoalTypes;
import org.spongepowered.api.entity.ai.goal.Goal;
import org.spongepowered.api.entity.ai.goal.GoalExecutor;
import org.spongepowered.api.entity.ai.goal.GoalExecutorTypes;
import org.spongepowered.api.entity.ai.goal.GoalTypes;
import org.spongepowered.api.entity.ai.goal.builtin.creature.AttackLivingGoal;
import org.spongepowered.api.entity.ai.goal.builtin.creature.RangedAttackAgainstAgentGoal;
import org.spongepowered.api.entity.ai.goal.builtin.creature.target.FindNearestAttackableTargetGoal;
import org.spongepowered.api.entity.ai.task.builtin.LookIdleAITask;
import org.spongepowered.api.entity.ai.task.builtin.SwimmingAITask;
import org.spongepowered.api.entity.ai.task.builtin.WatchClosestAITask;
import org.spongepowered.api.entity.ai.task.builtin.creature.AttackLivingAITask;
import org.spongepowered.api.entity.ai.task.builtin.creature.RangeAgentAITask;
import org.spongepowered.api.entity.ai.task.builtin.creature.WanderAITask;
import org.spongepowered.api.entity.ai.task.builtin.creature.target.FindNearestAttackableTargetAITask;
import org.spongepowered.api.entity.living.Agent;
import org.spongepowered.api.entity.living.Human;
import org.spongepowered.api.entity.living.Ranger;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.gamemode.GameModes;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.item.ItemTypes;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.scheduler.ScheduledTask;
import org.spongepowered.api.world.ServerLocation;
import org.spongepowered.api.world.explosion.Explosion;
import org.spongepowered.api.world.server.ServerWorld;
import org.spongepowered.math.vector.Vector3d;
import org.spongepowered.special.Special;
import org.spongepowered.special.instance.Instance;

import java.time.Duration;
import java.util.Random;

public final class CleanupTask extends InstanceTask {

    private final Title title = Title.of(
            TextComponent.of("Survive!", NamedTextColor.RED),
            TextComponent.empty(), // TODO is this the right way to do it?
            Title.Times.of(Duration.ZERO, Duration.ofSeconds(1), Duration.ofSeconds(2)));
    private ScheduledTask handle;
    private long duration = 0;

    public CleanupTask(Instance instance) {
        super(instance);
    }

    @Override
    public void accept(ScheduledTask task) {
        this.handle = task;

        final Random random = Special.instance.getRandom();
        final ServerWorld world = this.getInstance().getHandle().orElse(null);

        if (world != null && world.isLoaded()) {
            if (this.duration < 10) {
                for (ServerPlayer player : world.getPlayers()) {

                    if (!this.getInstance().getRegisteredPlayers().contains(player.getUniqueId())) {
                        continue;
                    }

                    if (this.duration == 0) {
                        player.showTitle(title);
                    }

                    final Vector3d location = player.getLocation().getPosition();
                    final Human human = (Human) world.createEntity(EntityTypes.HUMAN.get(), location);

                    ServerLocation spawnLocation = null;

                    // 5 tries to spawn
                    int tries = 5;

                    while (tries > 0) {
                        spawnLocation = Sponge.getServer().getTeleportHelper().getSafeLocation(human.getServerLocation().add(random.nextInt(4), 0,
                                random.nextInt(4)), 3, 3).orElse(null);

                        if (spawnLocation != null) {
                            human.setLocation(spawnLocation);
                            if (world.getIntersectingEntities(human.getBoundingBox().get()).isEmpty()) {
                                break;
                            }
                        }

                        tries--;
                    }

                    if (spawnLocation != null) {
                        final GoalExecutor<Agent> targetGoal = human.getGoal(GoalExecutorTypes.TARGET.get()).orElse(null);
                        targetGoal.addGoal(0, FindNearestAttackableTargetGoal.builder().chance(1).target(Player.class).filter(living -> {
                            if (living instanceof Player) {
                                if (this.getInstance().getRegisteredPlayers().contains(living.getUniqueId())) {
                                    return true;
                                }
                            }
                            return false;
                        }).build(human));

                        final GoalExecutor<Agent> normalGoal = human.getGoal(GoalExecutorTypes.NORMAL.get()).orElse(null);
                        normalGoal.addGoal(0, SwimmingGoal.builder().swimChance(0.8f).build(human));


                        float rangerChance = random.nextFloat();

                        boolean ranger = false;

                        if (rangerChance < 0.3f) {
                            normalGoal.addGoal(1, RangedAttackAgainstAgentGoal.builder().moveSpeed(0.4D).attackRadius(20f).delayBetweenAttacks(10).build(
                                    human));
                            human.setItemInHand(HandTypes.MAIN_HAND, ItemStack.of(ItemTypes.BOW, 1));
                            ItemStack tipped = ItemStack.of(ItemTypes.TIPPED_ARROW, 1);
                            tipped.offer(Keys.POTION_EFFECTS, Lists.newArrayList(
                                    PotionEffect.of(PotionEffectTypes.GLOWING.get(), 1, 60),
                                    PotionEffect.of(PotionEffectTypes.SLOWNESS.get(), 1, 60)));

                            human.setItemInHand(HandTypes.OFF_HAND, tipped);
                            ranger = true;
                        } else {
                            normalGoal.addGoal(1, AttackLivingGoal.builder().longMemory().speed(0.4D).build(human));
                            human.setItemInHand(HandTypes.MAIN_HAND, ItemStack.of(ItemTypes.DIAMOND_SWORD, 1));
                        }

                        normalGoal.addGoal(2, WanderGoal.builder().speed(0.3D).build(human));
                        normalGoal.addGoal(3, WatchClosestGoal.builder().maxDistance(8f).watch(Player.class).build(human));
                        normalGoal.addGoal(3, LookIdleGoal.builder().build(human));

                        human.offer(Keys.DISPLAY_NAME, TextComponent.of(ranger ? "Ranger" : "Swordsman",
                                ranger ? NamedTextColor.GREEN : NamedTextColor.BLUE));

                        world.spawnEntity(human);
                    }
                }
            } else {

                // Blow this place to pieces
                for (ServerPlayer player : world.getPlayers()) {
                    if (player.isRemoved() || player.get(Keys.GAME_MODE).get() == GameModes.SPECTATOR) {
                        continue;
                    }

                    final ServerLocation explosionLocation = player.getServerLocation().add(random.nextInt(4), random.nextInt(4), random.nextInt(4));

                    world.triggerExplosion(Explosion.builder()
                            .canCauseFire(true)
                            .shouldBreakBlocks(true)
                            .shouldPlaySmoke(true)
                            .radius(5)
                            .location(explosionLocation)
                            .build());
                }
            }
        }

        this.duration++;
    }

    @Override
    public void cancel() {
        this.handle.cancel();
    }
}
