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

import org.spongepowered.api.block.BlockState;
import org.spongepowered.api.block.BlockTypes;
import org.spongepowered.api.block.entity.BlockEntity;
import org.spongepowered.api.block.entity.Sign;
import org.spongepowered.api.block.entity.carrier.chest.Chest;
import org.spongepowered.api.data.Keys;
import org.spongepowered.api.util.weighted.LootTable;
import org.spongepowered.api.world.BoundedWorldView;
import org.spongepowered.special.Special;
import org.spongepowered.special.instance.Instance;
import org.spongepowered.special.instance.gen.loot.ItemArchetype;
import org.spongepowered.special.instance.gen.loot.Loot;

import java.util.List;

public final class ChestMutator extends SignMutator {

    public ChestMutator() {
        super("chest", "chest");
    }

    @Override
    public BlockState visitSign(Instance instance, BoundedWorldView<?> area, BlockState state, int x, int y, int z, Sign sign) {
        final String lootTableId = sign.lines().get(1).toPlain();
        final LootTable<ItemArchetype> lootTable = Loot.getTable(lootTableId);
        final List<ItemArchetype> items = lootTable.get(Special.instance.getRandom());

        Special.instance.getLogger().debug("Generating loot chest via table [" + lootTableId + "] at " + x + "x " + y + "y " + z + "z.");

        area.setBlock(x, y, z, BlockTypes.CHEST.get().getDefaultState().with(Keys.DIRECTION, state.get(Keys.DIRECTION).orElse(null)).orElse(null));

        final BlockEntity blockEntity = area.getBlockEntity(x, y, z).orElse(null);
        if (blockEntity == null) {
            throw new IllegalStateException("Something is quite wrong...we set a Chest down yet found no tile entity. This is a serious "
                    + "issue likely due to server misconfiguration!");
        } else if (!(blockEntity instanceof Chest)) {
            throw new IllegalStateException("Something is quite wrong...we set a Chest down yet found a [" + blockEntity.getClass()
                    .getSimpleName() + "] instead. This is a serious issue likely due to server misconfiguration!");
        }

        final Chest chest = (Chest) blockEntity;
        for (ItemArchetype item : items) {
            chest.getInventory().offer(item.create(Special.instance.getRandom()));
        }

        return state;
    }

}
