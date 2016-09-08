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
import org.spongepowered.api.block.tileentity.Sign;
import org.spongepowered.api.block.tileentity.TileEntity;
import org.spongepowered.api.block.tileentity.carrier.Chest;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.util.weighted.LootTable;
import org.spongepowered.api.world.extent.Extent;
import org.spongepowered.special.Constants.Map;
import org.spongepowered.special.Special;
import org.spongepowered.special.instance.gen.MapMutator;
import org.spongepowered.special.instance.gen.loot.ItemArchetype;
import org.spongepowered.special.instance.gen.loot.Loot;

import java.util.List;
import java.util.Optional;
import java.util.Random;

public class ChestMutator extends MapMutator {

    private Random rand = new Random();

    public ChestMutator() {
        super("chest", "Chest Mutator");
    }

    public boolean visitBlock(Extent extent, Map map, BlockState state, int x, int y, int z) {
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
        if (!lines.get(0).toPlain().equalsIgnoreCase("chest")) {
            return false;
        }
        String loot_table_id = lines.get(1).toPlain();
        LootTable<ItemArchetype> loot_table = Loot.getTable(loot_table_id);
        List<ItemArchetype> items = loot_table.get(this.rand);

        extent.setBlock(x, y, z, BlockTypes.CHEST.getDefaultState(), Special.plugin_cause);
        Optional<TileEntity> chestTile = extent.getTileEntity(x, y, z);
        if (!chestTile.isPresent() || !(chestTile.get() instanceof Chest)) {
            Special.instance.getLogger().error("Something is very wrong... (Expected a chest but was: " + (chestTile.isPresent() ? "null"
                    : chestTile.get().getClass().getSimpleName()) + ")");
            return false;
        }
        Chest chest = (Chest) chestTile.get();
        for (ItemArchetype item : items) {
            ItemStack stack = item.create(this.rand);
            chest.getInventory().offer(stack);
        }
        return true;
    }

}