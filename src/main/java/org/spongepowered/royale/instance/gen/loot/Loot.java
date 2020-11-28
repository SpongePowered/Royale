/*
 * This file is part of Royale, licensed under the MIT License (MIT).
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
package org.spongepowered.royale.instance.gen.loot;

import static org.spongepowered.api.util.weighted.VariableAmount.fixed;
import static org.spongepowered.api.util.weighted.VariableAmount.range;
import static org.spongepowered.royale.instance.gen.loot.PotionItemArchetype.Type.LINGERING;
import static org.spongepowered.royale.instance.gen.loot.PotionItemArchetype.Type.NORMAL;
import static org.spongepowered.royale.instance.gen.loot.PotionItemArchetype.Type.SPLASH;

import net.kyori.adventure.text.Component;
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

    private static final LootTable<ItemArchetype> lowLevel = new LootTable<>();
    private static final LootTable<ItemArchetype> midLevel = new LootTable<>();
    private static final LootTable<ItemArchetype> highLevel = new LootTable<>();
    private static final LootTable<ItemArchetype> rareLevel = new LootTable<>();

    private static final WeightedTable<ItemArchetype> basicFood = new WeightedTable<>();
    private static final WeightedTable<ItemArchetype> highFood = new WeightedTable<>();

    private static final WeightedTable<ItemArchetype> basicCombat = new WeightedTable<>();
    private static final WeightedTable<ItemArchetype> basicRanged = new WeightedTable<>();

    private static final WeightedTable<ItemArchetype> midCombat = new WeightedTable<>();
    private static final WeightedTable<ItemArchetype> midRanged = new WeightedTable<>();

    private static final ChanceTable<ItemArchetype> highCombat = new ChanceTable<>();

    private static final WeightedTable<ItemArchetype> rareItems = new WeightedTable<>();

    private static final ChanceTable<ItemArchetype> weirdItems = new ChanceTable<>();

    private static final WeightedTable<ItemArchetype> basicPotions = new WeightedTable<>();
    private static final WeightedTable<ItemArchetype> highPotions = new WeightedTable<>();

    private static final Map<String, LootTable<ItemArchetype>> lootTables = new HashMap<>();

    // @formatter:on

    static {
        lootTables.put("lowlevel", Loot.lowLevel);
        lootTables.put("midlevel", Loot.midLevel);
        lootTables.put("highlevel", Loot.highLevel);
        lootTables.put("rarelevel", Loot.rareLevel);

        Loot.lowLevel.addTable(Loot.basicFood);
        Loot.lowLevel.addTable(Loot.basicCombat);
        Loot.lowLevel.addTable(Loot.basicRanged);
        Loot.lowLevel.addTable(Loot.weirdItems);

        Loot.midLevel.addTable(Loot.basicFood);
        Loot.midLevel.addTable(Loot.basicPotions);
        Loot.midLevel.addTable(Loot.midCombat);
        Loot.midLevel.addTable(Loot.midRanged);
        Loot.midLevel.addTable(Loot.highFood);
        Loot.midLevel.addTable(Loot.weirdItems);

        Loot.highLevel.addTable(Loot.midCombat);
        Loot.highLevel.addTable(Loot.highCombat);
        Loot.highLevel.addTable(Loot.highPotions);
        Loot.highLevel.addTable(Loot.highFood);
        Loot.highLevel.addTable(Loot.weirdItems);

        Loot.rareLevel.addTable(Loot.highCombat);
        Loot.rareLevel.addTable(Loot.highPotions);
        Loot.rareLevel.addTable(Loot.highFood);
        Loot.rareLevel.addTable(Loot.weirdItems);
        Loot.rareLevel.addTable(Loot.rareItems);

        // @formatter:off

        // basic_food
        Loot.basicFood.add(     item(   ItemTypes.APPLE.get(),                range(1, 3)),   5);
        Loot.basicFood.add(     item(   ItemTypes.BAKED_POTATO.get(),         range(1, 3)),   3);
        Loot.basicFood.add(     item(   ItemTypes.BREAD.get(),                range(1, 3)),   5);
        Loot.basicFood.add(     item(   ItemTypes.CAKE.get(),                 fixed(1)),      1);
        Loot.basicFood.add(     item(   ItemTypes.CARROT.get(),               range(1, 4)),   3);
        Loot.basicFood.add(     item(   ItemTypes.COOKED_CHICKEN.get(),       range(1, 3)),   3);
        Loot.basicFood.add(     item(   ItemTypes.COOKED_COD.get(),           range(1, 3)),   1);
        Loot.basicFood.add(     item(   ItemTypes.COOKED_SALMON.get(),        range(1, 3)),   1);
        Loot.basicFood.add(     item(   ItemTypes.COOKED_MUTTON.get(),        range(1, 3)),   3);
        Loot.basicFood.add(     item(   ItemTypes.COOKED_RABBIT.get(),        range(1, 3)),   3);
        Loot.basicFood.add(     item(   ItemTypes.COOKIE.get(),               range(2, 8)),   5);
        Loot.basicFood.add(     item(   ItemTypes.MELON.get(),                range(1, 3)),   5);
        Loot.basicFood.add(     item(   ItemTypes.BOWL.get(),                 range(1, 3)),   2);
        Loot.basicFood.add(     item(   ItemTypes.RED_MUSHROOM.get(),         range(1, 3)),   3);
        Loot.basicFood.add(     item(   ItemTypes.BROWN_MUSHROOM.get(),       range(1, 3)),   4);

        // high_food
        Loot.highFood.add(      item(   ItemTypes.COOKED_BEEF.get(),          range(1, 2)),   2);
        Loot.highFood.add(      item(   ItemTypes.COOKED_PORKCHOP.get(),      range(1, 2)),   3);
        Loot.highFood.add(      item(   ItemTypes.MUSHROOM_STEW.get(),        fixed(1)),      3);
        Loot.highFood.add(      item(   ItemTypes.RABBIT_STEW.get(),          range(1, 3)),   2);
        Loot.highFood.add(      item(   ItemTypes.PUMPKIN_PIE.get(),          fixed(1)),      3);
        Loot.highFood.add(      item(   ItemTypes.GOLDEN_APPLE.get(),         range(1, 3)),   1);
        Loot.highFood.add(      item(   ItemTypes.GOLDEN_CARROT.get(),        range(1, 3)),   1);

        // basic_combat
        Loot.basicCombat.add(   item(   ItemTypes.WOODEN_AXE.get(),           fixed(1)),      5);
        Loot.basicCombat.add(   item(   ItemTypes.WOODEN_SWORD.get(),         fixed(1)),      5);
        Loot.basicCombat.add(   item(   ItemTypes.STONE_AXE.get(),            fixed(1)),      4);
        Loot.basicCombat.add(   item(   ItemTypes.STONE_SWORD.get(),          fixed(1)),      4);
        Loot.basicCombat.add(   item(   ItemTypes.FISHING_ROD.get(),          fixed(1)),      1);
        Loot.basicCombat.add(   item(   ItemTypes.FLINT_AND_STEEL.get(),      fixed(1)),      2);
        Loot.basicCombat.add(   item(   ItemTypes.SHIELD.get(),               fixed(1)),      1);
        Loot.basicCombat.add(   item(   ItemTypes.LEATHER_HELMET.get(),       fixed(1)),      5);
        Loot.basicCombat.add(   item(   ItemTypes.LEATHER_CHESTPLATE.get(),   fixed(1)),      1);
        Loot.basicCombat.add(   item(   ItemTypes.LEATHER_LEGGINGS.get(),     fixed(1)),      2);
        Loot.basicCombat.add(   item(   ItemTypes.LEATHER_BOOTS.get(),        fixed(1)),      3);

        // basic_ranged
        Loot.basicRanged.add(   item(   ItemTypes.BOW.get(),                  fixed(1)),      5);
        Loot.basicRanged.add(   item(   ItemTypes.ARROW.get(),                range(2, 6)),   10);
        Loot.basicRanged.add(   item(   ItemTypes.LEATHER_HELMET.get(),       fixed(1)),      5);
        Loot.basicRanged.add(   item(   ItemTypes.LEATHER_CHESTPLATE.get(),   fixed(1)),      1);
        Loot.basicRanged.add(   item(   ItemTypes.LEATHER_LEGGINGS.get(),     fixed(1)),      2);
        Loot.basicRanged.add(   item(   ItemTypes.LEATHER_BOOTS.get(),        fixed(1)),      3);
        
        // mid_combat
        Loot.midCombat.add(     item(   ItemTypes.GOLDEN_AXE.get(),           fixed(1)),      12);
        Loot.midCombat.add(     item(   ItemTypes.GOLDEN_SWORD.get(),         fixed(1)),      12);
        Loot.midCombat.add(     item(   ItemTypes.IRON_AXE.get(),             fixed(1)),      8);
        Loot.midCombat.add(     item(   ItemTypes.IRON_SWORD.get(),           fixed(1)),      8);
        Loot.midCombat.add(     item(   ItemTypes.SHIELD.get(),               fixed(1)),      12.5);
        Loot.midCombat.add(     item(   ItemTypes.GOLDEN_HELMET.get(),        fixed(1)),      8);
        Loot.midCombat.add(     item(   ItemTypes.GOLDEN_CHESTPLATE.get(),    fixed(1)),      2);
        Loot.midCombat.add(     item(   ItemTypes.GOLDEN_LEGGINGS.get(),      fixed(1)),      6);
        Loot.midCombat.add(     item(   ItemTypes.GOLDEN_BOOTS.get(),         fixed(1)),      8);
        Loot.midCombat.add(     item(   ItemTypes.IRON_HELMET.get(),          fixed(1)),      4);
        Loot.midCombat.add(     item(   ItemTypes.IRON_CHESTPLATE.get(),      fixed(1)),      1);
        Loot.midCombat.add(     item(   ItemTypes.IRON_LEGGINGS.get(),        fixed(1)),      2);
        Loot.midCombat.add(     item(   ItemTypes.IRON_BOOTS.get(),           fixed(1)),      4);

        // mid_ranged
        Loot.midRanged.add(     item(   ItemTypes.BOW.get(),              fixed(1)),      5);
        Loot.midRanged.add(     item(   ItemTypes.ARROW.get(),            range(2, 6)),   10);
        Loot.midRanged.add(item(ItemTypes.LEATHER_CHESTPLATE.get(), fixed(1), false,
                Component.text("Tough", NamedTextColor.RED),
                new HashMap<EnchantmentType, VariableAmount>() {{
                    put(EnchantmentTypes.PROTECTION.get(), fixed(1));
                }}), 1);
        Loot.midRanged.add(item(ItemTypes.LEATHER_LEGGINGS.get(), fixed(1), false,
                Component.text("Tough", NamedTextColor.RED),
                new HashMap<EnchantmentType, VariableAmount>() {{
                    put(EnchantmentTypes.PROTECTION.get(), fixed(1));
                }}), 1);

        // high_combat
        Loot.highCombat.add(    item(   ItemTypes.DIAMOND_AXE.get(),          fixed(1)),      0.004);
        Loot.highCombat.add(    item(   ItemTypes.DIAMOND_SWORD.get(),        fixed(1)),      0.0035);
        Loot.highCombat.add(    item(   ItemTypes.DIAMOND_HELMET.get(),       fixed(1)),      0.0028);
        Loot.highCombat.add(    item(   ItemTypes.DIAMOND_CHESTPLATE.get(),   fixed(1)),      0.0016);
        Loot.highCombat.add(    item(   ItemTypes.DIAMOND_LEGGINGS.get(),     fixed(1)),      0.002);
        Loot.highCombat.add(    item(   ItemTypes.DIAMOND_BOOTS.get(),        fixed(1)),      0.0024);

        // rare_items
        Loot.rareItems.add(item(ItemTypes.DIAMOND_SWORD.get(), fixed(1), false,
                Component.text("Flailing ", NamedTextColor.RED).append(Component.text("Sponge", NamedTextColor.YELLOW)),
                new HashMap<EnchantmentType, VariableAmount>() {{
                    put(EnchantmentTypes.FIRE_ASPECT.get(), range(1, 2));
                    put(EnchantmentTypes.KNOCKBACK.get(), range(1, 2));
                }}), 0.0018);

        Loot.rareItems.add(item(ItemTypes.SHIELD.get(), fixed(1), true,
                Component.text("Unbreaking ", NamedTextColor.GRAY).append(Component.text("Sponge", NamedTextColor.YELLOW)),
                new HashMap<EnchantmentType, VariableAmount>() {{
                    put(EnchantmentTypes.UNBREAKING.get(), range(1, 3));
                }}), 0.0018);

        Loot.rareItems.add(item(ItemTypes.ELYTRA.get(), fixed(1)), 0.0125);

        // weird_items
        Loot.weirdItems.add(    item(   ItemTypes.SPONGE.get(),           range(1, 6)),   15);
        Loot.weirdItems.add(    item(   ItemTypes.ROTTEN_FLESH.get(),     range(1, 9)),   6);
        Loot.weirdItems.add(    item(   ItemTypes.POISONOUS_POTATO.get(), range(1, 3)),   2.5);

        // basic_potions

        Loot.basicPotions.add(  potion(NORMAL,      fixed(1),       PotionEffectTypes.FIRE_RESISTANCE.get(),  fixed(1),   range(200, 400)),   6);
        Loot.basicPotions.add(  potion(NORMAL,      fixed(1),       PotionEffectTypes.INSTANT_HEALTH.get(),   fixed(1),   fixed(1)),          1);
        Loot.basicPotions.add(  potion(NORMAL,      fixed(1),       PotionEffectTypes.NIGHT_VISION.get(),     fixed(1),   range(200, 400)),   3);
        Loot.basicPotions.add(  potion(NORMAL,      fixed(1),       PotionEffectTypes.REGENERATION.get(),     fixed(1),   range(20, 80)),     2);

        Loot.basicPotions.add(  potion(SPLASH,      fixed(1),       PotionEffectTypes.HUNGER.get(),           fixed(1),   range(100, 200)),   6);
        Loot.basicPotions.add(  potion(SPLASH,      fixed(1),       PotionEffectTypes.INSTANT_DAMAGE.get(),   fixed(1),   fixed(1)),          1);
        Loot.basicPotions.add(  potion(SPLASH,      fixed(1),       PotionEffectTypes.SLOWNESS.get(),         fixed(1),   range(60, 200)),    6);

        // high_potions
        Loot.highPotions.add(   potion(NORMAL,      fixed(1),       PotionEffectTypes.FIRE_RESISTANCE.get(),  fixed(1),   range(200, 400)),   6   );
        Loot.highPotions.add(   potion(NORMAL,      fixed(1),       PotionEffectTypes.HEALTH_BOOST.get(),     fixed(1),   range(200, 400)),   4   );
        Loot.highPotions.add(   potion(NORMAL,      fixed(1),       PotionEffectTypes.INSTANT_HEALTH.get(),   fixed(1),   fixed(1)),          4   );
        Loot.highPotions.add(   potion(NORMAL,      fixed(1),       PotionEffectTypes.INVISIBILITY.get(),     fixed(1),   range(100, 200)),   1   );
        Loot.highPotions.add(   potion(NORMAL,      fixed(1),       PotionEffectTypes.JUMP_BOOST.get(),       fixed(1),   range(100, 200)),   4   );
        Loot.highPotions.add(   potion(NORMAL,      fixed(1),       PotionEffectTypes.NIGHT_VISION.get(),     fixed(1),   range(200, 400)),   5   );
        Loot.highPotions.add(   potion(NORMAL,      fixed(1),       PotionEffectTypes.REGENERATION.get(),     fixed(1),   range(40, 120)),    2   );
        Loot.highPotions.add(   potion(NORMAL,      fixed(1),       PotionEffectTypes.REGENERATION.get(),     fixed(2),   range(20, 100)),    0.1 );
        Loot.highPotions.add(   potion(NORMAL,      fixed(1),       PotionEffectTypes.SPEED.get(),            fixed(1),   range(100, 200)),   2   );
        Loot.highPotions.add(   potion(NORMAL,      fixed(1),       PotionEffectTypes.SPEED.get(),            fixed(2),   range(50, 100)),    0.3 );

        Loot.highPotions.add(   potion(SPLASH,      fixed(1),       PotionEffectTypes.BLINDNESS.get(),        fixed(1),   range(60, 120)),    2   );
        Loot.highPotions.add(   potion(SPLASH,      fixed(1),       PotionEffectTypes.HUNGER.get(),           fixed(1),   range(100, 200)),   6   );
        Loot.highPotions.add(   potion(SPLASH,      fixed(1),       PotionEffectTypes.INSTANT_DAMAGE.get(),   fixed(1),   fixed(1)),          3   );
        Loot.highPotions.add(   potion(SPLASH,      fixed(1),       PotionEffectTypes.NAUSEA.get(),           fixed(1),   range(60, 120)),    2   );
        Loot.highPotions.add(   potion(SPLASH,      fixed(1),       PotionEffectTypes.SLOWNESS.get(),         fixed(1),   range(100, 200)),   6   );
        Loot.highPotions.add(   potion(SPLASH,      fixed(1),       PotionEffectTypes.WEAKNESS.get(),         fixed(1),   range(60, 120)),    4   );

        Loot.highPotions.add(   potion(LINGERING,   fixed(1),       PotionEffectTypes.HUNGER.get(),           fixed(1),   range(60, 120)),    1   );

        // @formatter:on
    }

    private Loot() {
    }

    public static LootTable<ItemArchetype> getTable(final String id) {
        final LootTable<ItemArchetype> table = Loot.lootTables.get(id);
        if (table == null) {
            throw new IllegalArgumentException(String.format("Loot table: %s not found!", id));
        }
        return table;
    }

    private static ItemArchetype item(final ItemType type, final VariableAmount quantity) {
        return new BasicItemArchetype(type, quantity);
    }

    private static ItemArchetype item(final ItemType type, final VariableAmount quantity, final boolean unbreakable, final Component name,
            final Map<EnchantmentType, VariableAmount> enchantments) {
        return new EnchantedItemArchetype(type, quantity, unbreakable, name, enchantments);
    }

    private static ItemArchetype potion(final PotionItemArchetype.Type type, final VariableAmount quantity, final PotionEffectType effect,
            final VariableAmount power, final VariableAmount duration) {
        return new PotionItemArchetype(type, quantity, effect, power, duration);
    }
}
