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
package org.spongepowered.royale.instance.gen;

import com.google.common.base.MoreObjects;
import org.spongepowered.api.block.BlockState;
import org.spongepowered.api.util.PositionOutOfBoundsException;
import org.spongepowered.api.world.BoundedWorldView;
import org.spongepowered.api.world.server.ServerWorld;
import org.spongepowered.math.vector.Vector3i;
import org.spongepowered.royale.Royale;
import org.spongepowered.royale.instance.Instance;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Mutates the map templates after loading into the final form.
 */
public final class InstanceMutatorPipeline {

    private final Set<InstanceMutator> mutators;

    public InstanceMutatorPipeline() {
        this.mutators = new HashSet<>();
    }

    public Collection<InstanceMutator> getMutators() {
        return this.mutators;
    }

    public void mutate(final Instance instance, final boolean tryFastPass) {
        final ServerWorld world = instance.getWorld()
                .orElseThrow(() -> new RuntimeException(String.format("Attempting to mutate instance '%s' but it's world is not loaded!"
                        , instance.getWorldKey())));

        final BoundedWorldView<?> area = world.getExtentView(instance.getType().getBlockMin(),
                instance.getType().getBlockMax());

        if (!tryFastPass) {
            Royale.instance.getPlugin().getLogger().error("[Mutator] Instance {} is not eligible for a fast pass! Using slow pass.", instance.getWorldKey());
        }

        if (tryFastPass && canPerformFastPass(instance, area)) {
            Royale.instance.getPlugin().getLogger().error("[Mutator] Pre-flight checks succeeded! Performing fast pass for instance {}.",
                    instance.getWorldKey());

            for (final InstanceMutator mutator : this.mutators) {
                Royale.instance.getPlugin().getLogger().info("Mutating instance [{}}] with mutator [{}]...", instance.getWorldKey(), mutator.getKey());
            }

            for (Map.Entry<Vector3i, BlockState> entry : instance.getPositionCache().entrySet()) {
                for (InstanceMutator mutator : this.mutators) {
                    mutator.visitBlock(instance, area, area.getBlock(entry.getKey()), entry.getKey().getX(), entry.getKey().getY(),
                            entry.getKey().getZ());
                }
            }
            return;
        }

        for (final InstanceMutator mutator : this.mutators) {
            Royale.instance.getPlugin().getLogger().info("Mutating instance [{}}] with mutator [{}]...", instance.getWorldKey(), mutator.getKey());
        }

        Vector3i size = instance.getType().getBlockSize();
        Royale.instance.getPlugin().getLogger().error("[Mutator] Performing slow pass for instance {} - {} blocks total.", instance.getWorldKey(), size.getX() *
                size.getY() * size.getZ());

        area.getBlockWorker().iterate((v, x, y, z) -> {
            for (InstanceMutator mutator : this.mutators) {
                BlockState expectedState = mutator.visitBlock(instance, area, v.getBlock(x, y, z), x, y, z);
                if (expectedState != null) {
                    instance.getPositionCache().put(new Vector3i(x, y, z), expectedState);
                }
            }
        });
    }

    private boolean canPerformFastPass(final Instance instance, final BoundedWorldView<?> extent) {
        // We don't have any cached positions, so we can't do a fast pass
        if (instance.getPositionCache().isEmpty()) {
            Royale.instance.getPlugin().getLogger().error("[Mutator] No cached positions for instance {} were found. Falling back to slow pass.",
                    instance.getWorldKey());
            return false;
        }

        for (Map.Entry<Vector3i, BlockState> entry : instance.getPositionCache().entrySet()) {
            try {
                if (!extent.getBlock(entry.getKey()).equals(entry.getValue())) {
                    Royale.instance.getPlugin().getLogger().error("[Mutator] Mutator mismatch! At position {}, expected {} but found {}. Falling back to slow "
                                    + "pass.", entry.getKey(), entry.getValue(), extent.getBlock(entry.getKey()));
                    return false;
                }
            } catch (final PositionOutOfBoundsException e) {
                Royale.instance.getPlugin().getLogger().error("[Mutator] Position {} was out of bounds (not sure how this happened). Falling back to slow pass"
                        + ".", entry.getKey());
                e.printStackTrace();
                return false;
            }
        }
        return true;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("mutators", this.mutators)
                .toString();
    }
}
