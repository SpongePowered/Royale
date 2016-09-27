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
import org.spongepowered.api.item.Enchantment;
import org.spongepowered.api.item.Enchantments;
import org.spongepowered.api.item.ItemType;
import org.spongepowered.api.item.ItemTypes;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.util.weighted.ChanceTable;
import org.spongepowered.api.util.weighted.LootTable;
import org.spongepowered.api.util.weighted.VariableAmount;
import org.spongepowered.api.util.weighted.WeightedTable;

import java.util.HashMap;
import java.util.Map;

public final class Loot {

    // @formatter:off

    private static final LootTable<ItemArchetype> low_level = new LootTable<>();
    private static final LootTable<ItemArchetype> mid_level = new LootTable<>();
    private static final LootTable<ItemArchetype> high_level = new LootTable<>();
    private static final LootTable<ItemArchetype> rare_level = new LootTable<>();

    private static final WeightedTable<ItemArchetype> basic_food = new WeightedTable<>();
    private static final WeightedTable<ItemArchetype> high_food = new WeightedTable<>();

    private static final WeightedTable<ItemArchetype> basic_combat = new WeightedTable<>();
    private static final WeightedTable<ItemArchetype> basic_ranged = new WeightedTable<>();

    private static final WeightedTable<ItemArchetype> mid_combat = new WeightedTable<>();
    private static final WeightedTable<ItemArchetype> mid_ranged = new WeightedTable<>();

    private static final ChanceTable<ItemArchetype> high_combat = new ChanceTable<>();

    private static final WeightedTable<ItemArchetype> rare_items = new WeightedTable<>();

    private static final ChanceTable<ItemArchetype> weird_items = new ChanceTable<>();

    private static final WeightedTable<ItemArchetype> basic_potions = new WeightedTable<>();
    private static final WeightedTable<ItemArchetype> high_potions = new WeightedTable<>();

    private static final Map<String, LootTable<ItemArchetype>> loot_tables = Maps.newHashMap();

    // @formatter:on

    static {
        loot_tables.put("low_level", low_level);
        loot_tables.put("mid_level", mid_level);
        loot_tables.put("high_level", high_level);
        loot_tables.put("rare_level", rare_level);

        low_level.addTable(basic_food);
        low_level.addTable(basic_combat);
        low_level.addTable(basic_ranged);
        low_level.addTable(weird_items);

        mid_level.addTable(basic_food);
        mid_level.addTable(basic_potions);
        mid_level.addTable(mid_combat);
        mid_level.addTable(mid_ranged);
        mid_level.addTable(high_food);
        mid_level.addTable(weird_items);

        high_level.addTable(mid_combat);
        high_level.addTable(high_combat);
        high_level.addTable(high_potions);
        high_level.addTable(high_food);
        high_level.addTable(weird_items);

        rare_level.addTable(high_combat);
        rare_level.addTable(high_potions);
        rare_level.addTable(high_food);
        rare_level.addTable(weird_items);
        rare_level.addTable(rare_items);

        // @formatter:off

        // TODO flesh out these tables more

        // basic_food
        basic_food.add(     item(   ItemTypes.APPLE,                range(1, 3)),   5);
        basic_food.add(     item(   ItemTypes.BAKED_POTATO,         range(1, 3)),   3);
        basic_food.add(     item(   ItemTypes.BREAD,                range(1, 3)),   5);
        basic_food.add(     item(   ItemTypes.CAKE,                 fixed(1)),      1);
        basic_food.add(     item(   ItemTypes.CARROT,               range(1, 4)),   3);
        basic_food.add(     item(   ItemTypes.COOKED_CHICKEN,       range(1, 3)),   3);
        basic_food.add(     item(   ItemTypes.COOKED_FISH,          range(1, 3)),   2);
        basic_food.add(     item(   ItemTypes.COOKED_MUTTON,        range(1, 3)),   3);
        basic_food.add(     item(   ItemTypes.COOKED_RABBIT,        range(1, 3)),   3);
        basic_food.add(     item(   ItemTypes.COOKIE,               range(2, 8)),   5);
        basic_food.add(     item(   ItemTypes.MELON,                range(1, 3)),   5);
        basic_food.add(     item(   ItemTypes.BOWL,                 range(1, 3)),   2);
        basic_food.add(     item(   ItemTypes.RED_MUSHROOM,         range(1, 3)),   3);
        basic_food.add(     item(   ItemTypes.BROWN_MUSHROOM,       range(1, 3)),   4);

        // high_food
        high_food.add(      item(   ItemTypes.COOKED_BEEF,          range(1, 2)),   2);
        high_food.add(      item(   ItemTypes.COOKED_PORKCHOP,      range(1, 2)),   3);
        high_food.add(      item(   ItemTypes.MUSHROOM_STEW,        fixed(1)),      3);
        high_food.add(      item(   ItemTypes.RABBIT_STEW,          range(1, 3)),   2);
        high_food.add(      item(   ItemTypes.PUMPKIN_PIE,          fixed(1)),      3);
        high_food.add(      item(   ItemTypes.GOLDEN_APPLE,         range(1, 3)),   1);
        high_food.add(      item(   ItemTypes.GOLDEN_CARROT,        range(1, 3)),   1);

        // basic_combat
        basic_combat.add(   item(   ItemTypes.WOODEN_AXE,           fixed(1)),      5);
        basic_combat.add(   item(   ItemTypes.WOODEN_SWORD,         fixed(1)),      5);
        basic_combat.add(   item(   ItemTypes.STONE_AXE,            fixed(1)),      4);
        basic_combat.add(   item(   ItemTypes.STONE_SWORD,          fixed(1)),      4);
        basic_combat.add(   item(   ItemTypes.FISHING_ROD,          fixed(1)),      1);
        basic_combat.add(   item(   ItemTypes.FLINT_AND_STEEL,      fixed(1)),      2);
        basic_combat.add(   item(   ItemTypes.SHIELD,               fixed(1)),      1);
        basic_combat.add(   item(   ItemTypes.LEATHER_HELMET,       fixed(1)),      5);
        basic_combat.add(   item(   ItemTypes.LEATHER_CHESTPLATE,   fixed(1)),      1);
        basic_combat.add(   item(   ItemTypes.LEATHER_LEGGINGS,     fixed(1)),      2);
        basic_combat.add(   item(   ItemTypes.LEATHER_BOOTS,        fixed(1)),      3);

        // basic_ranged
        basic_ranged.add(   item(   ItemTypes.BOW,                  fixed(1)),      5);
        basic_ranged.add(   item(   ItemTypes.ARROW,                range(2, 6)),   10);
        basic_ranged.add(   item(   ItemTypes.LEATHER_HELMET,       fixed(1)),      5);
        basic_ranged.add(   item(   ItemTypes.LEATHER_CHESTPLATE,   fixed(1)),      1);
        basic_ranged.add(   item(   ItemTypes.LEATHER_LEGGINGS,     fixed(1)),      2);
        basic_ranged.add(   item(   ItemTypes.LEATHER_BOOTS,        fixed(1)),      3);
        
        // mid_combat
        mid_combat.add(     item(   ItemTypes.GOLDEN_AXE,           fixed(1)),      12);
        mid_combat.add(     item(   ItemTypes.GOLDEN_SWORD,         fixed(1)),      12);
        mid_combat.add(     item(   ItemTypes.IRON_AXE,             fixed(1)),      8);
        mid_combat.add(     item(   ItemTypes.IRON_SWORD,           fixed(1)),      8);
        mid_combat.add(     item(   ItemTypes.SHIELD,               fixed(1)),      12.5);
        mid_combat.add(     item(   ItemTypes.GOLDEN_HELMET,        fixed(1)),      8);
        mid_combat.add(     item(   ItemTypes.GOLDEN_CHESTPLATE,    fixed(1)),      2);
        mid_combat.add(     item(   ItemTypes.GOLDEN_LEGGINGS,      fixed(1)),      6);
        mid_combat.add(     item(   ItemTypes.GOLDEN_BOOTS,         fixed(1)),      8);
        mid_combat.add(     item(   ItemTypes.IRON_HELMET,          fixed(1)),      4);
        mid_combat.add(     item(   ItemTypes.IRON_CHESTPLATE,      fixed(1)),      1);
        mid_combat.add(     item(   ItemTypes.IRON_LEGGINGS,        fixed(1)),      2);
        mid_combat.add(     item(   ItemTypes.IRON_BOOTS,           fixed(1)),      4);

        // mid_ranged
        mid_ranged.add(     item(   ItemTypes.BOW,              fixed(1)),      5);
        mid_ranged.add(     item(   ItemTypes.ARROW,            range(2, 6)),   10);
        mid_ranged.add(item(ItemTypes.LEATHER_CHESTPLATE, fixed(1), false,
                Text.of(TextColors.RED, "Tough"),
                new HashMap<Enchantment, VariableAmount>() {{
                    put(Enchantments.PROTECTION, fixed(1));
                }}), 1);
        mid_ranged.add(item(ItemTypes.LEATHER_LEGGINGS, fixed(1), false,
                Text.of(TextColors.RED, "Tough"),
                new HashMap<Enchantment, VariableAmount>() {{
                    put(Enchantments.PROTECTION, fixed(1));
                }}), 1);

        // high_combat
        high_combat.add(    item(   ItemTypes.DIAMOND_AXE,          fixed(1)),      0.004);
        high_combat.add(    item(   ItemTypes.DIAMOND_SWORD,        fixed(1)),      0.0035);
        high_combat.add(    item(   ItemTypes.DIAMOND_HELMET,       fixed(1)),      0.0028);
        high_combat.add(    item(   ItemTypes.DIAMOND_CHESTPLATE,   fixed(1)),      0.0016);
        high_combat.add(    item(   ItemTypes.DIAMOND_LEGGINGS,     fixed(1)),      0.002);
        high_combat.add(    item(   ItemTypes.DIAMOND_BOOTS,        fixed(1)),      0.0024);

        // rare_items
        rare_items.add(item(ItemTypes.DIAMOND_SWORD, fixed(1), false,
                Text.of(TextColors.RED, "Flailing ", TextColors.YELLOW, "Sponge"),
                new HashMap<Enchantment, VariableAmount>() {{
                    put(Enchantments.FIRE_ASPECT, range(1, 2));
                    put(Enchantments.KNOCKBACK, range(1, 2));
                }}), 0.0018);

        rare_items.add(item(ItemTypes.SHIELD, fixed(1), true,
                Text.of(TextColors.GRAY, "Unbreaking ", TextColors.YELLOW, "Sponge"),
                new HashMap<Enchantment, VariableAmount>() {{
                    put(Enchantments.UNBREAKING, range(1, 3));
                }}), 0.0018);

        rare_items.add(item(ItemTypes.ELYTRA, fixed(1)), 0.0125);

        // weird_items
        weird_items.add(    item(   ItemTypes.SPONGE,           range(1, 6)),   15);
        weird_items.add(    item(   ItemTypes.ROTTEN_FLESH,     range(1, 9)),   6);
        weird_items.add(    item(   ItemTypes.POISONOUS_POTATO, range(1, 3)),   2.5);

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

    private static ItemArchetype item(ItemType type, VariableAmount quantity, boolean unbreakable, Text name,
            Map<Enchantment, VariableAmount> enchantments) {
        return new EnchantedItemArchetype(type, quantity, unbreakable, name, enchantments);
    }

    private static ItemArchetype potion(PotionItemArchetype.Type type, VariableAmount quantity, PotionEffectType effect, VariableAmount power,
            VariableAmount duration) {
        return new PotionItemArchetype(type, quantity, effect, power, duration);
    }
}
