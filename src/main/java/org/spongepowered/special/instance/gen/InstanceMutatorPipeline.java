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
package org.spongepowered.special.instance.gen;

import com.flowpowered.math.vector.Vector3i;
import com.google.common.base.Objects;
import org.spongepowered.api.block.BlockState;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.channel.MessageChannel;
import org.spongepowered.api.util.PositionOutOfBoundsException;
import org.spongepowered.api.world.extent.Extent;
import org.spongepowered.special.Special;
import org.spongepowered.special.instance.Instance;
import org.spongepowered.special.instance.InstanceManager;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Mutates the map templates after loading into the final form.
 */
public final class InstanceMutatorPipeline {

    private static final Map<String, Map<Vector3i, BlockState>> positionCache = new HashMap<>();
    private final Set<InstanceMutator> mutators = new HashSet<>();

    public Set<InstanceMutator> getMutators() {
        return this.mutators;
    }

    public void mutate(Instance instance, boolean tryFastPass) {
        final Extent area = instance.getHandle().get().getExtentView(instance.getType().getBlockMin(), instance.getType().getBlockMax());
        if (tryFastPass && canPerformFastPass(instance.getName(), area)) {
            for (InstanceMutator mutator: this.mutators) {
                mutator.visitInstance(instance);
            }

            for (Map.Entry<Vector3i, BlockState> entry: positionCache.get(instance.getName()).entrySet()) {
                for (InstanceMutator mutator : this.mutators) {
                    mutator.visitBlock(instance, area, area.getBlock(entry.getKey()), entry.getKey().getX(), entry.getKey().getY(), entry.getKey().getZ());
                }
            }
            return;
        }

        for (InstanceMutator mutator: this.mutators) {
            mutator.visitInstance(instance);
        }

        Map<Vector3i, BlockState> cache = new HashMap<>();

        area.getBlockWorker(Special.instance.getPluginCause()).iterate((v, x, y, z) -> {
            final BlockState state = v.getBlock(x, y, z);

            for (InstanceMutator mutator : this.mutators) {
                BlockState expectedState = mutator.visitBlock(instance, area, state, x, y, z);
                if (expectedState != null) {
                    cache.put(new Vector3i(x, y, z), expectedState);
                }
            }
        });

        positionCache.put(instance.getName(), cache);
    }

    private boolean canPerformFastPass(String name, Extent extent) {
        Map<Vector3i, BlockState> cache = positionCache.get(name);

        // We don't have any cached positions, so we can't do a fast pass
        if (cache == null) {
            return false;
        }

        for (Map.Entry<Vector3i, BlockState> entry: cache.entrySet()) {
            try {
                if (!extent.getBlock(entry.getKey()).equals(entry.getValue())) {
                    return false;
                }
            } catch (PositionOutOfBoundsException e) {
                return false;
            }
        }
        return true;
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(this)
                .add("mutators", this.mutators)
                .toString();
    }
}
