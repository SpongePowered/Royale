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
package org.spongepowered.special.instance.gen.loot;

import static com.google.common.base.Preconditions.checkNotNull;

import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.data.manipulator.mutable.item.EnchantmentData;
import org.spongepowered.api.data.meta.ItemEnchantment;
import org.spongepowered.api.item.Enchantment;
import org.spongepowered.api.item.ItemType;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.util.weighted.VariableAmount;

import java.util.Collections;
import java.util.Map;
import java.util.Random;

class EnchantedItemArchetype implements ItemArchetype {

    private final boolean unbreakable;
    private final ItemType type;
    private final Map<Enchantment, VariableAmount> enchantments;
    private final Text name;
    private final VariableAmount quantity;

    EnchantedItemArchetype(ItemType type, VariableAmount quantity, boolean unbreakable, Text name, Map<Enchantment, VariableAmount>
            enchantments) {
        this.type = checkNotNull(type);
        this.quantity = checkNotNull(quantity);
        this.unbreakable = checkNotNull(unbreakable);
        this.name = checkNotNull(name);
        this.enchantments = checkNotNull(enchantments);
    }

    @Override
    public ItemStack create(Random rand) {
        final int amount = this.quantity.getFlooredAmount(rand);
        final ItemStack itemStack = ItemStack.builder()
                .itemType(this.type)
                .quantity(amount)
                .build();

        final EnchantmentData enchantmentData = itemStack.getOrCreate(EnchantmentData.class).orElse(null);
        for (Map.Entry<Enchantment, VariableAmount> entry : enchantments.entrySet()) {
            enchantmentData.addElement(new ItemEnchantment(entry.getKey(), entry.getValue().getFlooredAmount(rand)));
        }

        itemStack.offer(enchantmentData);
        itemStack.offer(Keys.DISPLAY_NAME, name);
        itemStack.offer(Keys.UNBREAKABLE, unbreakable);

        return itemStack;
    }

    public final ItemType getType() {
        return this.type;
    }

    public final VariableAmount getQuantity() {
        return this.quantity;
    }

    public final boolean isUnbreakable() {
        return this.unbreakable;
    }

    public final Map<Enchantment, VariableAmount> getEnchantments() {
        return Collections.unmodifiableMap(enchantments);
    }
}
