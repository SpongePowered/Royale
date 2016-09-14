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

import com.google.common.base.Objects;
import org.spongepowered.api.block.BlockState;
import org.spongepowered.api.world.extent.Extent;
import org.spongepowered.special.Special;
import org.spongepowered.special.instance.Instance;

import java.util.HashSet;
import java.util.Set;

/**
 * Mutates the map templates after loading into the final form.
 */
public final class InstanceMutatorPipeline {

    private final Set<InstanceMutator> mutators = new HashSet<>();

    public Set<InstanceMutator> getMutators() {
        return this.mutators;
    }

    public void mutate(Instance instance) {
        for (InstanceMutator mutator : this.mutators) {
            mutator.visitInstance(instance);

            final Extent area = instance.getHandle().get().getExtentView(instance.getType().getBlockMin(), instance.getType().getBlockMax());

            area.getBlockWorker(Special.instance.getPluginCause()).iterate((v, x, y, z) -> {
                final BlockState state = v.getBlock(x, y, z);

                mutator.visitBlock(instance, area, state, x, y, z);
            });
        }
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(this)
                .add("mutators", this.mutators)
                .toString();
    }
}
