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

import com.google.common.collect.Lists;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.effect.potion.PotionEffect;
import org.spongepowered.api.effect.potion.PotionEffectType;
import org.spongepowered.api.item.ItemType;
import org.spongepowered.api.item.ItemTypes;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.util.weighted.VariableAmount;

import java.util.List;
import java.util.Random;

class PotionItemArchetype extends BasicItemArchetype {

    private final VariableAmount power;
    private final VariableAmount duration;
    private final PotionEffectType effect;

    PotionItemArchetype(Type type, VariableAmount quantity, PotionEffectType effect, VariableAmount power, VariableAmount duration) {
        super(type.getItemType(), quantity);
        this.effect = checkNotNull(effect);
        this.power = checkNotNull(power);
        this.duration = checkNotNull(duration);
    }

    @Override
    public ItemStack create(Random rand) {
        final int amount = this.getQuantity().getFlooredAmount(rand);
        final List<PotionEffect> itemEffects = Lists.newArrayList();
        itemEffects.add(PotionEffect.of(this.effect, this.power.getFlooredAmount(rand), this.duration.getFlooredAmount(rand)));
        return ItemStack.builder()
                .itemType(this.getType())
                .quantity(amount)
                .keyValue(Keys.POTION_EFFECTS, itemEffects)
                .build();
    }

    public enum Type {
        NORMAL(ItemTypes.POTION),
        SPLASH(ItemTypes.SPLASH_POTION),
        LINGERING(ItemTypes.LINGERING_POTION);

        private ItemType itemType;

        Type(ItemType type) {
            this.itemType = type;
        }

        public ItemType getItemType() {
            return this.itemType;
        }
    }
}
