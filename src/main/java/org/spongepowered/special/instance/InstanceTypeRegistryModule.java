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
package org.spongepowered.special.instance;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.Objects;
import ninja.leaping.configurate.objectmapping.ObjectMappingException;
import org.spongepowered.api.registry.AdditionalCatalogRegistryModule;
import org.spongepowered.api.registry.CatalogTypeAlreadyRegisteredException;
import org.spongepowered.api.registry.RegistrationPhase;
import org.spongepowered.api.registry.util.DelayedRegistration;
import org.spongepowered.api.registry.util.RegistrationDependency;
import org.spongepowered.special.Constants;
import org.spongepowered.special.Special;
import org.spongepowered.special.configuration.MappedConfigurationAdapter;
import org.spongepowered.special.instance.configuration.InstanceTypeConfiguration;
import org.spongepowered.special.instance.gen.InstanceMutatorRegistryModule;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RegistrationDependency(InstanceMutatorRegistryModule.class)
public final class InstanceTypeRegistryModule implements AdditionalCatalogRegistryModule<InstanceType> {

    private final Map<String, InstanceType> types = new HashMap<>();

    private InstanceTypeRegistryModule() {
    }

    public static InstanceTypeRegistryModule getInstance() {
        return Holder.INSTANCE;
    }

    @Override
    public Optional<InstanceType> getById(String id) {
        checkNotNull(id);
        return Optional.ofNullable(this.types.get(id));
    }

    @Override
    public Collection<InstanceType> getAll() {
        return Collections.unmodifiableCollection(this.types.values());
    }

    @Override
    public void registerAdditionalCatalog(InstanceType extraCatalog) {
        checkNotNull(extraCatalog);
        if (this.types.containsKey(extraCatalog.getId())) {
            throw new CatalogTypeAlreadyRegisteredException(extraCatalog.getId());
        }

        this.types.put(extraCatalog.getId(), extraCatalog);

        Special.instance.getLogger().info("Registered instance type [{}].", extraCatalog.getId());
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(this)
                .add("types", this.getAll())
                .toString();
    }

    @Override
    @DelayedRegistration(value = RegistrationPhase.POST_INIT)
    public void registerDefaults() {

        // Load all from config
        try (DirectoryStream<Path> stream = Files
                .newDirectoryStream(Constants.Map.PATH_CONFIG_INSTANCE_TYPES, entry -> entry.getFileName().toString()
                        .endsWith(".conf"))) {
            for (Path path : stream) {
                final MappedConfigurationAdapter<InstanceTypeConfiguration> adapter =
                        new MappedConfigurationAdapter<>(InstanceTypeConfiguration.class,
                                Constants.Map.DEFAULT_OPTIONS, path);

                try {
                    adapter.load();
                } catch (IOException | ObjectMappingException e) {
                    Special.instance.getLogger().error("Failed to load configuration for path [{}]!", path, e);
                    continue;
                }

                final String instanceId = path.getFileName().toString().split("\\.")[0];

                this.registerAdditionalCatalog(
                        InstanceType.builder().from(adapter.getConfig()).build(instanceId));
            }

        } catch (IOException | ObjectMappingException e) {
            throw new RuntimeException("Failed to iterate over the instance type configuration files!");
        }
    }

    private static final class Holder {

        static final InstanceTypeRegistryModule INSTANCE = new InstanceTypeRegistryModule();
    }
}
