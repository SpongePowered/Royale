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
package org.spongepowered.special.map;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.Objects;
import org.spongepowered.api.registry.AdditionalCatalogRegistryModule;
import org.spongepowered.api.registry.CatalogTypeAlreadyRegisteredException;
import org.spongepowered.special.Special;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public final class MapRegistryModule implements AdditionalCatalogRegistryModule<MapType> {

    public static MapRegistryModule getInstance() {
        return Holder.INSTANCE;
    }

    final Map<String, MapType> maps = new HashMap<>();

    private MapRegistryModule() {}

    @Override
    public Optional<MapType> getById(String id) {
        checkNotNull(id);
        return Optional.ofNullable(this.maps.get(id));
    }

    @Override
    public Collection<MapType> getAll() {
        return Collections.unmodifiableCollection(this.maps.values());
    }

    @Override
    public void registerAdditionalCatalog(MapType extraCatalog) {
        checkNotNull(extraCatalog);
        if (this.maps.containsKey(extraCatalog.getId())) {
            throw new CatalogTypeAlreadyRegisteredException(extraCatalog.getId());
        }

        this.maps.put(extraCatalog.getId(), extraCatalog);
        Special.instance.getLogger().info("Registered map [{}}.", extraCatalog);
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(this)
                .add("maps", this.getAll())
                .toString();
    }

    private static final class Holder {

        static final MapRegistryModule INSTANCE = new MapRegistryModule();
    }
}
