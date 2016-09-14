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
package org.spongepowered.special.instance.gen;

import org.spongepowered.api.Sponge;
import org.spongepowered.api.block.tileentity.Sign;

/**
 * {@link InstanceMutator}s available to run against instances.
 */
public final class InstanceMutators {

    // SORTFIELDS:ON

    /**
     * An instance mutator that populate possible player spawns by detecting {@link Sign}s with "player_spawn" one line 1.
     */
    public static final InstanceMutator PLAYER_SPAWN = Sponge.getRegistry().getType(InstanceMutator.class, "player_spawn").orElse(null);

    /**
     * An instance mutator that populate loot chests with varying levels of rarity by detecting {@link Sign}s with "chest" one line 1
     * and a loot id on line 2.
     */
    public static final InstanceMutator CHEST_MUTATOR = Sponge.getRegistry().getType(InstanceMutator.class, "chest").orElse(null);

    // SORTFIELDS:OFF
}
