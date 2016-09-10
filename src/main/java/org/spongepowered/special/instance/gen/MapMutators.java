package org.spongepowered.special.instance.gen;

import org.spongepowered.api.Sponge;
import org.spongepowered.api.block.tileentity.Sign;

/**
 * {@link MapMutator}s available to run against instances.
 */
public final class MapMutators {

    // SORTFIELDS:ON

    /**
     * A map mutator that populate possible player spawns by detecting {@link Sign}s with "player_spawn" one line 1.
     */
    public static final MapMutator PLAYER_SPAWN = Sponge.getRegistry().getType(MapMutator.class, "player_spawn").orElse(null);

    /**
     * A map mutator that populate loot chests with varying levels of rarity by detecting {@link Sign}s with "chest" one line 1
     * and a loot id on line 2.
     */
    public static final MapMutator CHEST_MUTATOR = Sponge.getRegistry().getType(MapMutator.class, "chest").orElse(null);

    // SORTFIELDS:OFF
}
