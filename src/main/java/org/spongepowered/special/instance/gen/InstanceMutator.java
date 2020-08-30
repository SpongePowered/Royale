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

import com.google.common.base.MoreObjects;
import org.spongepowered.api.CatalogType;
import org.spongepowered.api.ResourceKey;
import org.spongepowered.api.block.BlockState;
import org.spongepowered.api.util.annotation.CatalogedBy;
import org.spongepowered.api.world.BoundedWorldView;
import org.spongepowered.special.Special;
import org.spongepowered.special.instance.Instance;

@CatalogedBy(InstanceMutators.class)
public abstract class InstanceMutator implements CatalogType {

    private final ResourceKey id;

    protected InstanceMutator(ResourceKey id) {
        this.id = id;
    }

    @Override
    public final ResourceKey getKey() {
        return this.id;
    }

    void visitInstance(Instance instance) {
        Special.instance.getLogger().info("Mutating instance [" + instance.getName() + "] with mutator [" + this.getKey() + "]...");
    }

    public BlockState visitBlock(Instance instance, BoundedWorldView<?> area, BlockState state, int x, int y, int z) {
        return null;
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(id);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final InstanceMutator that = (InstanceMutator) o;
        return java.util.Objects.equals(id, that.id);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("id", this.id)
                .toString();
    }
}
