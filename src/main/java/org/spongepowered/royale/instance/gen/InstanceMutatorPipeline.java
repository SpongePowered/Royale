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
import org.spongepowered.api.world.server.ServerWorld;
import org.spongepowered.api.world.volume.stream.StreamOptions;
import org.spongepowered.api.world.volume.stream.VolumeCollectors;
import org.spongepowered.math.vector.Vector3i;
import org.spongepowered.royale.Royale;
import org.spongepowered.royale.instance.Instance;

import java.util.Collection;
import java.util.HashSet;
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

        final Vector3i min = instance.getType().getBlockMin();
        final Vector3i max = instance.getType().getBlockMax();

        for (final InstanceMutator mutator : this.mutators) {
            Royale.instance.getPlugin().getLogger().info("Mutating instance [{}}] with mutator [{}]...", instance.getWorldKey(), mutator.getKey());
        }

        final Vector3i size = instance.getType().getBlockSize();
        Royale.instance.getPlugin().getLogger().error("[Mutator] Performing slow pass for instance {} - {} blocks total.", instance.getWorldKey(), size.getX() *
                size.getY() * size.getZ());

        this.mutators.forEach(mutator -> mutator.prepare(instance));

//        this.mutators.forEach(mutator -> world.getBlockEntityStream(min, max, StreamOptions.forceLoadedAndCopied())
//            .filter(mutator.getBlockEntityPredicate(instance))
//            .map(mutator.getBlockEntityMapper(instance))
//            .apply(VolumeCollectors.applyBlockEntityToWorld(world)));

    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("mutators", this.mutators)
                .toString();
    }
}
