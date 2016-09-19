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

import com.flowpowered.math.vector.Vector3d;
import org.spongepowered.api.block.BlockState;
import org.spongepowered.api.block.BlockTypes;
import org.spongepowered.api.block.tileentity.Sign;
import org.spongepowered.api.world.extent.Extent;
import org.spongepowered.special.Special;
import org.spongepowered.special.instance.Instance;

public final class PlayerSpawnMutator extends SignMutator {

    public PlayerSpawnMutator() {
        super("player_spawn", "Player Spawn Finder", "player_spawn");
    }

    @Override
    public BlockState visitSign(Instance instance, Extent area, BlockState state, int x, int y, int z, Sign sign) {
        area.setBlock(x, y, z, BlockTypes.AIR.getDefaultState(), Special.instance.getPluginCause());

        instance.addPlayerSpawn(new Vector3d(x + 0.5, y + 0.0125, z + 0.5));

        Special.instance.getLogger().info("Found player spawn at " + x + "x " + y + "y " + z + "z.");

        return state;
    }
}
