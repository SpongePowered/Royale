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
package org.spongepowered.royale.instance;

import org.spongepowered.api.ResourceKey;
import org.spongepowered.api.block.entity.Sign;
import org.spongepowered.api.data.DataRegistration;
import org.spongepowered.api.data.Key;
import org.spongepowered.api.data.persistence.DataStore;
import org.spongepowered.api.data.value.Value;
import org.spongepowered.api.event.lifecycle.RegisterDataEvent;
import org.spongepowered.api.util.TypeTokens;

public interface RoyaleData {

    Key<Value<String>> WORLD = Key.builder().key(ResourceKey.of("royale", "world")).type(TypeTokens.STRING_VALUE_TOKEN).build();
    Key<Value<String>> TYPE = Key.builder().key(ResourceKey.of("royale", "type")).type(TypeTokens.STRING_VALUE_TOKEN).build();

    static void register(RegisterDataEvent event)
    {
        final ResourceKey datastoreKey = ResourceKey.of("royale", "elevator");

        final DataStore dataStore = DataStore.builder().pluginData(datastoreKey)
                .holder(Sign.class)
                .keys(RoyaleData.WORLD, RoyaleData.TYPE)
                .build();

        final DataRegistration registration = DataRegistration.builder()
                .dataKey(RoyaleData.WORLD, RoyaleData.TYPE)
                .store(dataStore)
                .build();
        event.register(registration);
    }

}
