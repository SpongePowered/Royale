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
import org.spongepowered.api.text.Text;
import org.spongepowered.api.world.extent.Extent;
import org.spongepowered.special.Special;
import org.spongepowered.special.instance.Instance;
import org.spongepowered.special.instance.gen.MapMutator;

import java.util.List;
import java.util.Optional;

public abstract class SignMutator extends MapMutator {

    private final String sign_id;

    public SignMutator(String id, String name, String sign_id) {
        super(id, name);
        this.sign_id = sign_id;
    }

    public abstract boolean visitSign(Extent extent, Instance instance, int x, int y, int z, List<Text> signText);

    public final boolean visitBlock(Extent extent, Instance instance, BlockState state, int x, int y, int z) {
        if (state.getType() != BlockTypes.STANDING_SIGN && state.getType() != BlockTypes.WALL_SIGN) {
            return false;
        }
        Optional<TileEntity> tile = extent.getTileEntity(x, y, z);
        if (!tile.isPresent() || !(tile.get() instanceof Sign)) {
            Special.instance.getLogger().error("Something is very wrong... (Expected a sign but was: " + (tile.isPresent() ? "null"
                    : tile.get().getClass().getSimpleName()) + ")");
            return false;
        }
        Sign sign = (Sign) tile.get();
        List<Text> lines = sign.lines().get();
        if (!lines.get(0).toPlain().equalsIgnoreCase(this.sign_id)) {
            return false;
        }
        visitSign(extent, instance, x, y, z, lines);
        return true;
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
