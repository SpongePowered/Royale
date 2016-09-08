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

import com.google.common.collect.Lists;
import org.spongepowered.api.block.BlockState;
import org.spongepowered.api.world.World;
import org.spongepowered.api.world.extent.Extent;
import org.spongepowered.special.Special;
import org.spongepowered.special.instance.Instance;

import java.util.List;

/**
 * Mutates the map templates after loading into the final form.
 */
public class MapMutatorPipeline {

    private final List<MapMutator> mutators = Lists.newArrayList();

    public MapMutatorPipeline() {

    }

    public void addMutator(MapMutator mutator) {
        this.mutators.add(mutator);
    }

    public List<MapMutator> getMutators() {
        return this.mutators;
    }

    public void mutate(World world, Instance instance) {
        Extent area = world.getExtentView(instance.getType().getBlockMin(), instance.getType().getBlockMax());

        for (MapMutator mutator : this.mutators) {
            mutator.visitExtent(area, instance);
        }

        area.getBlockWorker(Special.plugin_cause).iterate((v, x, y, z) -> {
            BlockState state = v.getBlock(x, y, z);
            for (MapMutator mutator : this.mutators) {
                if (mutator.visitBlock(area, instance, state, x, y, z)) {
                    break;
                }
            }
        });
    }

}