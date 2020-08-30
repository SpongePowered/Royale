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
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.spongepowered.api.effect.potion.PotionEffectType;
import org.spongepowered.api.effect.potion.PotionEffectTypes;
import org.spongepowered.api.item.ItemType;
import org.spongepowered.api.item.ItemTypes;
import org.spongepowered.api.item.enchantment.EnchantmentType;
import org.spongepowered.api.item.enchantment.EnchantmentTypes;
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
        basic_food.add(     item(   ItemTypes.APPLE.get(),                range(1, 3)),   5);
        basic_food.add(     item(   ItemTypes.BAKED_POTATO.get(),         range(1, 3)),   3);
        basic_food.add(     item(   ItemTypes.BREAD.get(),                range(1, 3)),   5);
        basic_food.add(     item(   ItemTypes.CAKE.get(),                 fixed(1)),      1);
        basic_food.add(     item(   ItemTypes.CARROT.get(),               range(1, 4)),   3);
        basic_food.add(     item(   ItemTypes.COOKED_CHICKEN.get(),       range(1, 3)),   3);
        basic_food.add(     item(   ItemTypes.COOKED_COD.get(),           range(1, 3)),   1);
        basic_food.add(     item(   ItemTypes.COOKED_SALMON.get(),        range(1, 3)),   1);
        basic_food.add(     item(   ItemTypes.COOKED_MUTTON.get(),        range(1, 3)),   3);
        basic_food.add(     item(   ItemTypes.COOKED_RABBIT.get(),        range(1, 3)),   3);
        basic_food.add(     item(   ItemTypes.COOKIE.get(),               range(2, 8)),   5);
        basic_food.add(     item(   ItemTypes.MELON.get(),                range(1, 3)),   5);
        basic_food.add(     item(   ItemTypes.BOWL.get(),                 range(1, 3)),   2);
        basic_food.add(     item(   ItemTypes.RED_MUSHROOM.get(),         range(1, 3)),   3);
        basic_food.add(     item(   ItemTypes.BROWN_MUSHROOM.get(),       range(1, 3)),   4);

        // high_food
        high_food.add(      item(   ItemTypes.COOKED_BEEF.get(),          range(1, 2)),   2);
        high_food.add(      item(   ItemTypes.COOKED_PORKCHOP.get(),      range(1, 2)),   3);
        high_food.add(      item(   ItemTypes.MUSHROOM_STEW.get(),        fixed(1)),      3);
        high_food.add(      item(   ItemTypes.RABBIT_STEW.get(),          range(1, 3)),   2);
        high_food.add(      item(   ItemTypes.PUMPKIN_PIE.get(),          fixed(1)),      3);
        high_food.add(      item(   ItemTypes.GOLDEN_APPLE.get(),         range(1, 3)),   1);
        high_food.add(      item(   ItemTypes.GOLDEN_CARROT.get(),        range(1, 3)),   1);

        // basic_combat
        basic_combat.add(   item(   ItemTypes.WOODEN_AXE.get(),           fixed(1)),      5);
        basic_combat.add(   item(   ItemTypes.WOODEN_SWORD.get(),         fixed(1)),      5);
        basic_combat.add(   item(   ItemTypes.STONE_AXE.get(),            fixed(1)),      4);
        basic_combat.add(   item(   ItemTypes.STONE_SWORD.get(),          fixed(1)),      4);
        basic_combat.add(   item(   ItemTypes.FISHING_ROD.get(),          fixed(1)),      1);
        basic_combat.add(   item(   ItemTypes.FLINT_AND_STEEL.get(),      fixed(1)),      2);
        basic_combat.add(   item(   ItemTypes.SHIELD.get(),               fixed(1)),      1);
        basic_combat.add(   item(   ItemTypes.LEATHER_HELMET.get(),       fixed(1)),      5);
        basic_combat.add(   item(   ItemTypes.LEATHER_CHESTPLATE.get(),   fixed(1)),      1);
        basic_combat.add(   item(   ItemTypes.LEATHER_LEGGINGS.get(),     fixed(1)),      2);
        basic_combat.add(   item(   ItemTypes.LEATHER_BOOTS.get(),        fixed(1)),      3);

        // basic_ranged
        basic_ranged.add(   item(   ItemTypes.BOW.get(),                  fixed(1)),      5);
        basic_ranged.add(   item(   ItemTypes.ARROW.get(),                range(2, 6)),   10);
        basic_ranged.add(   item(   ItemTypes.LEATHER_HELMET.get(),       fixed(1)),      5);
        basic_ranged.add(   item(   ItemTypes.LEATHER_CHESTPLATE.get(),   fixed(1)),      1);
        basic_ranged.add(   item(   ItemTypes.LEATHER_LEGGINGS.get(),     fixed(1)),      2);
        basic_ranged.add(   item(   ItemTypes.LEATHER_BOOTS.get(),        fixed(1)),      3);
        
        // mid_combat
        mid_combat.add(     item(   ItemTypes.GOLDEN_AXE.get(),           fixed(1)),      12);
        mid_combat.add(     item(   ItemTypes.GOLDEN_SWORD.get(),         fixed(1)),      12);
        mid_combat.add(     item(   ItemTypes.IRON_AXE.get(),             fixed(1)),      8);
        mid_combat.add(     item(   ItemTypes.IRON_SWORD.get(),           fixed(1)),      8);
        mid_combat.add(     item(   ItemTypes.SHIELD.get(),               fixed(1)),      12.5);
        mid_combat.add(     item(   ItemTypes.GOLDEN_HELMET.get(),        fixed(1)),      8);
        mid_combat.add(     item(   ItemTypes.GOLDEN_CHESTPLATE.get(),    fixed(1)),      2);
        mid_combat.add(     item(   ItemTypes.GOLDEN_LEGGINGS.get(),      fixed(1)),      6);
        mid_combat.add(     item(   ItemTypes.GOLDEN_BOOTS.get(),         fixed(1)),      8);
        mid_combat.add(     item(   ItemTypes.IRON_HELMET.get(),          fixed(1)),      4);
        mid_combat.add(     item(   ItemTypes.IRON_CHESTPLATE.get(),      fixed(1)),      1);
        mid_combat.add(     item(   ItemTypes.IRON_LEGGINGS.get(),        fixed(1)),      2);
        mid_combat.add(     item(   ItemTypes.IRON_BOOTS.get(),           fixed(1)),      4);

        // mid_ranged
        mid_ranged.add(     item(   ItemTypes.BOW.get(),              fixed(1)),      5);
        mid_ranged.add(     item(   ItemTypes.ARROW.get(),            range(2, 6)),   10);
        mid_ranged.add(item(ItemTypes.LEATHER_CHESTPLATE.get(), fixed(1), false,
                TextComponent.of("Tough", NamedTextColor.RED),
                new HashMap<EnchantmentType, VariableAmount>() {{
                    put(EnchantmentTypes.PROTECTION.get(), fixed(1));
                }}), 1);
        mid_ranged.add(item(ItemTypes.LEATHER_LEGGINGS.get(), fixed(1), false,
                TextComponent.of("Tough", NamedTextColor.RED),
                new HashMap<EnchantmentType, VariableAmount>() {{
                    put(EnchantmentTypes.PROTECTION.get(), fixed(1));
                }}), 1);

        // high_combat
        high_combat.add(    item(   ItemTypes.DIAMOND_AXE.get(),          fixed(1)),      0.004);
        high_combat.add(    item(   ItemTypes.DIAMOND_SWORD.get(),        fixed(1)),      0.0035);
        high_combat.add(    item(   ItemTypes.DIAMOND_HELMET.get(),       fixed(1)),      0.0028);
        high_combat.add(    item(   ItemTypes.DIAMOND_CHESTPLATE.get(),   fixed(1)),      0.0016);
        high_combat.add(    item(   ItemTypes.DIAMOND_LEGGINGS.get(),     fixed(1)),      0.002);
        high_combat.add(    item(   ItemTypes.DIAMOND_BOOTS.get(),        fixed(1)),      0.0024);

        // rare_items
        rare_items.add(item(ItemTypes.DIAMOND_SWORD.get(), fixed(1), false,
                TextComponent.of("Flailing ", NamedTextColor.RED).append(TextComponent.of("Sponge", NamedTextColor.YELLOW)),
                new HashMap<EnchantmentType, VariableAmount>() {{
                    put(EnchantmentTypes.FIRE_ASPECT.get(), range(1, 2));
                    put(EnchantmentTypes.KNOCKBACK.get(), range(1, 2));
                }}), 0.0018);

        rare_items.add(item(ItemTypes.SHIELD.get(), fixed(1), true,
                TextComponent.of("Unbreaking ", NamedTextColor.GRAY).append(TextComponent.of("Sponge", NamedTextColor.YELLOW)),
                new HashMap<EnchantmentType, VariableAmount>() {{
                    put(EnchantmentTypes.UNBREAKING.get(), range(1, 3));
                }}), 0.0018);

        rare_items.add(item(ItemTypes.ELYTRA.get(), fixed(1)), 0.0125);

        // weird_items
        weird_items.add(    item(   ItemTypes.SPONGE.get(),           range(1, 6)),   15);
        weird_items.add(    item(   ItemTypes.ROTTEN_FLESH.get(),     range(1, 9)),   6);
        weird_items.add(    item(   ItemTypes.POISONOUS_POTATO.get(), range(1, 3)),   2.5);

        // basic_potions

        basic_potions.add(  potion(NORMAL,      fixed(1),       PotionEffectTypes.FIRE_RESISTANCE.get(),  fixed(1),   range(200, 400)),   6);
        basic_potions.add(  potion(NORMAL,      fixed(1),       PotionEffectTypes.INSTANT_HEALTH.get(),   fixed(1),   fixed(1)),          1);
        basic_potions.add(  potion(NORMAL,      fixed(1),       PotionEffectTypes.NIGHT_VISION.get(),     fixed(1),   range(200, 400)),   3);
        basic_potions.add(  potion(NORMAL,      fixed(1),       PotionEffectTypes.REGENERATION.get(),     fixed(1),   range(20, 80)),     2);

        basic_potions.add(  potion(SPLASH,      fixed(1),       PotionEffectTypes.HUNGER.get(),           fixed(1),   range(100, 200)),   6);
        basic_potions.add(  potion(SPLASH,      fixed(1),       PotionEffectTypes.INSTANT_DAMAGE.get(),   fixed(1),   fixed(1)),          1);
        basic_potions.add(  potion(SPLASH,      fixed(1),       PotionEffectTypes.SLOWNESS.get(),         fixed(1),   range(60, 200)),    6);

        // high_potions
        high_potions.add(   potion(NORMAL,      fixed(1),       PotionEffectTypes.FIRE_RESISTANCE.get(),  fixed(1),   range(200, 400)),   6   );
        high_potions.add(   potion(NORMAL,      fixed(1),       PotionEffectTypes.HEALTH_BOOST.get(),     fixed(1),   range(200, 400)),   4   );
        high_potions.add(   potion(NORMAL,      fixed(1),       PotionEffectTypes.INSTANT_HEALTH.get(),   fixed(1),   fixed(1)),          4   );
        high_potions.add(   potion(NORMAL,      fixed(1),       PotionEffectTypes.INVISIBILITY.get(),     fixed(1),   range(100, 200)),   1   );
        high_potions.add(   potion(NORMAL,      fixed(1),       PotionEffectTypes.JUMP_BOOST.get(),       fixed(1),   range(100, 200)),   4   );
        high_potions.add(   potion(NORMAL,      fixed(1),       PotionEffectTypes.NIGHT_VISION.get(),     fixed(1),   range(200, 400)),   5   );
        high_potions.add(   potion(NORMAL,      fixed(1),       PotionEffectTypes.REGENERATION.get(),     fixed(1),   range(40, 120)),    2   );
        high_potions.add(   potion(NORMAL,      fixed(1),       PotionEffectTypes.REGENERATION.get(),     fixed(2),   range(20, 100)),    0.1 );
        high_potions.add(   potion(NORMAL,      fixed(1),       PotionEffectTypes.SPEED.get(),            fixed(1),   range(100, 200)),   2   );
        high_potions.add(   potion(NORMAL,      fixed(1),       PotionEffectTypes.SPEED.get(),            fixed(2),   range(50, 100)),    0.3 );

        high_potions.add(   potion(SPLASH,      fixed(1),       PotionEffectTypes.BLINDNESS.get(),        fixed(1),   range(60, 120)),    2   );
        high_potions.add(   potion(SPLASH,      fixed(1),       PotionEffectTypes.HUNGER.get(),           fixed(1),   range(100, 200)),   6   );
        high_potions.add(   potion(SPLASH,      fixed(1),       PotionEffectTypes.INSTANT_DAMAGE.get(),   fixed(1),   fixed(1)),          3   );
        high_potions.add(   potion(SPLASH,      fixed(1),       PotionEffectTypes.NAUSEA.get(),           fixed(1),   range(60, 120)),    2   );
        high_potions.add(   potion(SPLASH,      fixed(1),       PotionEffectTypes.SLOWNESS.get(),         fixed(1),   range(100, 200)),   6   );
        high_potions.add(   potion(SPLASH,      fixed(1),       PotionEffectTypes.WEAKNESS.get(),         fixed(1),   range(60, 120)),    4   );

        high_potions.add(   potion(LINGERING,   fixed(1),       PotionEffectTypes.HUNGER.get(),           fixed(1),   range(60, 120)),    1   );

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

    private static ItemArchetype item(ItemType type, VariableAmount quantity, boolean unbreakable, Component name,
            Map<EnchantmentType, VariableAmount> enchantments) {
        return new EnchantedItemArchetype(type, quantity, unbreakable, name, enchantments);
    }

    private static ItemArchetype potion(PotionItemArchetype.Type type, VariableAmount quantity, PotionEffectType effect, VariableAmount power,
            VariableAmount duration) {
        return new PotionItemArchetype(type, quantity, effect, power, duration);
    }
}
