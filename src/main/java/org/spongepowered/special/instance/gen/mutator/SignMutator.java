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
package org.spongepowered.special.instance.gen.mutator;

import com.google.common.base.Objects;
import org.spongepowered.api.block.BlockState;
import org.spongepowered.api.block.BlockTypes;
import org.spongepowered.api.block.tileentity.Sign;
import org.spongepowered.api.block.tileentity.TileEntity;
import org.spongepowered.api.world.extent.Extent;
import org.spongepowered.special.Special;
import org.spongepowered.special.instance.Instance;
import org.spongepowered.special.instance.gen.InstanceMutator;

abstract class SignMutator extends InstanceMutator {

    private final String sign_id;

    SignMutator(String id, String name, String sign_id) {
        super(id, name);
        this.sign_id = sign_id;
    }

    public abstract BlockState visitSign(Instance instance, Extent area, BlockState state, int x, int y, int z, Sign sign);

    @Override
    public BlockState visitBlock(Instance instance, Extent area, BlockState state, int x, int y, int z) {
        if (state.getType() != BlockTypes.STANDING_SIGN && state.getType() != BlockTypes.WALL_SIGN) {
            return null;
        }

        final TileEntity tileEntity = area.getTileEntity(x, y, z).orElse(null);
        if (tileEntity == null) {
            throw new IllegalStateException("Something is quite wrong...we have a sign type yet found no tile entity. This is a serious "
                    + "issue likely due to server misconfiguration!");
        } else if (!(tileEntity instanceof Sign)) {
            throw new IllegalStateException("Something is quite wrong...we have a sign type yet found a [" + tileEntity.getClass()
                    .getSimpleName() + "] instead. This is a serious issue likely due to server misconfiguration!");
        }

        final Sign sign = (Sign) tileEntity;
        if (!sign.lines().get(0).toPlain().equalsIgnoreCase(this.sign_id)) {
            if (sign.lines().get(1).toPlain().equalsIgnoreCase(this.sign_id)) {
                Special.instance.getLogger().error("Found mismatched sign at {}x {}y {}z!", x, y, z);
            } else {
                return null;
            }
        }

        return visitSign(instance, area, state, x, y, z, sign);
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(this)
                .add("id", this.getId())
                .add("name", this.getName())
                .add("signId", this.sign_id)
                .toString();
    }
}
