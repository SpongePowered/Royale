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

import static org.spongepowered.api.util.weighted.VariableAmount.fixed;
import static org.spongepowered.api.util.weighted.VariableAmount.range;
import static org.spongepowered.special.instance.gen.loot.PotionItemArchetype.Type.LINGERING;
import static org.spongepowered.special.instance.gen.loot.PotionItemArchetype.Type.NORMAL;
import static org.spongepowered.special.instance.gen.loot.PotionItemArchetype.Type.SPLASH;

import com.google.common.collect.Maps;
import org.spongepowered.api.effect.potion.PotionEffectType;
import org.spongepowered.api.effect.potion.PotionEffectTypes;
import org.spongepowered.api.item.ItemType;
import org.spongepowered.api.item.ItemTypes;
import org.spongepowered.api.util.weighted.ChanceTable;
import org.spongepowered.api.util.weighted.LootTable;
import org.spongepowered.api.util.weighted.VariableAmount;
import org.spongepowered.api.util.weighted.WeightedTable;

import java.util.Map;

public final class Loot {

    // @formatter:off

    public static final LootTable<ItemArchetype> low_level = new LootTable<>();
    public static final LootTable<ItemArchetype> mid_level = new LootTable<>();
    public static final LootTable<ItemArchetype> high_level = new LootTable<>();
    public static final LootTable<ItemArchetype> super_rare = new LootTable<>();

    public static final WeightedTable<ItemArchetype> basic_food = new WeightedTable<>();
    public static final WeightedTable<ItemArchetype> high_food = new WeightedTable<>();

    public static final WeightedTable<ItemArchetype> basic_combat = new WeightedTable<>();
    public static final WeightedTable<ItemArchetype> basic_ranged = new WeightedTable<>();

    public static final WeightedTable<ItemArchetype> mid_combat = new WeightedTable<>();
    public static final WeightedTable<ItemArchetype> mid_ranged = new WeightedTable<>();

    public static final ChanceTable<ItemArchetype> high_combat = new ChanceTable<>();

    public static final WeightedTable<ItemArchetype> super_rare_items = new WeightedTable<>();

    public static final ChanceTable<ItemArchetype> weird_items = new ChanceTable<>();

    public static final WeightedTable<ItemArchetype> basic_potions = new WeightedTable<>();
    public static final ChanceTable<ItemArchetype> high_potions = new ChanceTable<>();

    private static final Map<String, LootTable<ItemArchetype>> loot_tables = Maps.newHashMap();

    // @formatter:on

    static {
        loot_tables.put("low_level", low_level);
        loot_tables.put("mid_level", mid_level);
        loot_tables.put("high_level", high_level);
        loot_tables.put("super_rare", super_rare);

        low_level.addTable(basic_food);
        low_level.addTable(basic_combat);
        low_level.addTable(weird_items);

        mid_level.addTable(basic_food);
        mid_level.addTable(basic_potions);
        mid_level.addTable(mid_combat);
        mid_level.addTable(high_food);
        mid_level.addTable(weird_items);

        high_level.addTable(mid_combat);
        high_level.addTable(high_combat);
        high_level.addTable(high_potions);
        high_level.addTable(high_food);
        high_level.addTable(weird_items);

        high_level.addTable(high_combat);
        high_level.addTable(high_potions);
        high_level.addTable(high_food);
        high_level.addTable(weird_items);
        high_level.addTable(super_rare_items);

        // @formatter:off

        // TODO flesh out these tables more

        // basic_food
        basic_food.add(     item(   ItemTypes.BREAD,            range(1, 4)),   5);

        // high_food
        high_food.add(      item(   ItemTypes.RABBIT_STEW,      fixed(1)),      5);

        // basic_combat
        basic_combat.add(   item(   ItemTypes.STONE_SWORD,      fixed(1)),      5);

        // basic_ranged
        basic_ranged.add(   item(   ItemTypes.BOW,              fixed(1)),      5);

        // mid_combat
        mid_combat.add(     item(   ItemTypes.IRON_SWORD,       fixed(1)),      5);

        // mid_ranged
        mid_ranged.add(     item(   ItemTypes.BOW,              fixed(1)),      5);

        // high_combat
        high_combat.add(    item(   ItemTypes.DIAMOND_SWORD,    fixed(1)),      0.0025);

        // super_rare_items

        // TODO special named weapons and armor

        // weird_items
        weird_items.add(     item(   ItemTypes.SPONGE,           range(1, 6)),   5);

        // basic_potions

        basic_potions.add(  potion(NORMAL,      fixed(1),       PotionEffectTypes.FIRE_RESISTANCE,  fixed(1),   range(200, 400)),   6);
        basic_potions.add(  potion(NORMAL,      fixed(1),       PotionEffectTypes.INSTANT_HEALTH,   fixed(1),   fixed(1)),          1);
        basic_potions.add(  potion(NORMAL,      fixed(1),       PotionEffectTypes.NIGHT_VISION,     fixed(1),   range(200, 400)),   3);
        basic_potions.add(  potion(NORMAL,      fixed(1),       PotionEffectTypes.REGENERATION,     fixed(1),   range(20, 80)),     2);

        basic_potions.add(  potion(SPLASH,      fixed(1),       PotionEffectTypes.HUNGER,           fixed(1),   range(100, 200)),   6);
        basic_potions.add(  potion(SPLASH,      fixed(1),       PotionEffectTypes.INSTANT_DAMAGE,   fixed(1),   fixed(1)),          1);
        basic_potions.add(  potion(SPLASH,      fixed(1),       PotionEffectTypes.SLOWNESS,         fixed(1),   range(60, 200)),    6);

        // high_potions
        high_potions.add(   potion(NORMAL,      fixed(1),       PotionEffectTypes.FIRE_RESISTANCE,  fixed(1),   range(200, 400)),   6   );
        high_potions.add(   potion(NORMAL,      fixed(1),       PotionEffectTypes.HEALTH_BOOST,     fixed(1),   range(200, 400)),   4   );
        high_potions.add(   potion(NORMAL,      fixed(1),       PotionEffectTypes.INSTANT_HEALTH,   fixed(1),   fixed(1)),          4   );
        high_potions.add(   potion(NORMAL,      fixed(1),       PotionEffectTypes.INVISIBILITY,     fixed(1),   range(100, 200)),   1   );
        high_potions.add(   potion(NORMAL,      fixed(1),       PotionEffectTypes.JUMP_BOOST,       fixed(1),   range(100, 200)),   4   );
        high_potions.add(   potion(NORMAL,      fixed(1),       PotionEffectTypes.NIGHT_VISION,     fixed(1),   range(200, 400)),   5   );
        high_potions.add(   potion(NORMAL,      fixed(1),       PotionEffectTypes.REGENERATION,     fixed(1),   range(40, 120)),    2   );
        high_potions.add(   potion(NORMAL,      fixed(1),       PotionEffectTypes.REGENERATION,     fixed(2),   range(20, 100)),    0.1 );
        high_potions.add(   potion(NORMAL,      fixed(1),       PotionEffectTypes.SPEED,            fixed(1),   range(100, 200)),   2   );
        high_potions.add(   potion(NORMAL,      fixed(1),       PotionEffectTypes.SPEED,            fixed(2),   range(50, 100)),    0.3 );

        high_potions.add(   potion(SPLASH,      fixed(1),       PotionEffectTypes.BLINDNESS,        fixed(1),   range(60, 120)),    2   );
        high_potions.add(   potion(SPLASH,      fixed(1),       PotionEffectTypes.HUNGER,           fixed(1),   range(100, 200)),   6   );
        high_potions.add(   potion(SPLASH,      fixed(1),       PotionEffectTypes.INSTANT_DAMAGE,   fixed(1),   fixed(1)),          3   );
        high_potions.add(   potion(SPLASH,      fixed(1),       PotionEffectTypes.NAUSEA,           fixed(1),   range(60, 120)),    2   );
        high_potions.add(   potion(SPLASH,      fixed(1),       PotionEffectTypes.SLOWNESS,         fixed(1),   range(100, 200)),   6   );
        high_potions.add(   potion(SPLASH,      fixed(1),       PotionEffectTypes.WEAKNESS,         fixed(1),   range(60, 120)),    4   );

        high_potions.add(   potion(LINGERING,   fixed(1),       PotionEffectTypes.HUNGER,           fixed(1),   range(60, 120)),    1   );

        // @formatter:on
    }

    private Loot() {
    }

    public static LootTable<ItemArchetype> getTable(String id) {
        LootTable<ItemArchetype> table = loot_tables.get(id);
        if (table == null) {
            throw new IllegalArgumentException("Loot table: " + id + " not found");
        }
        return table;
    }

    private static ItemArchetype item(ItemType type, VariableAmount quantity) {
        return new BasicItemArchetype(type, quantity);
    }

    private static ItemArchetype potion(PotionItemArchetype.Type type, VariableAmount quantity, PotionEffectType effect,
            VariableAmount power, VariableAmount duration) {
        return new PotionItemArchetype(type, quantity, effect, power, duration);
    }
}
