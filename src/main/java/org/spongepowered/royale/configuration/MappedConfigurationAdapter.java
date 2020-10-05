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
package org.spongepowered.royale.configuration;

import com.typesafe.config.ConfigRenderOptions;
import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.ConfigurationOptions;
import ninja.leaping.configurate.commented.CommentedConfigurationNode;
import ninja.leaping.configurate.commented.SimpleCommentedConfigurationNode;
import ninja.leaping.configurate.hocon.HoconConfigurationLoader;
import ninja.leaping.configurate.objectmapping.ObjectMapper;
import ninja.leaping.configurate.objectmapping.ObjectMappingException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class MappedConfigurationAdapter<T extends AbstractConfiguration> {

    private final Class<T> configClass;
    private final Path configFile;
    private final HoconConfigurationLoader loader;
    private final ObjectMapper<T>.BoundInstance mapper;

    private ConfigurationNode root;
    private T config;

    public MappedConfigurationAdapter(final Class<T> configClass, final ConfigurationOptions options, final Path configFile) {
        this.configClass = configClass;
        this.configFile = configFile;
        this.loader = HoconConfigurationLoader.builder()
                .setRenderOptions(ConfigRenderOptions.defaults().setFormatted(true).setComments(true).setOriginComments(false))
                .setDefaultOptions(options)
                .setPath(configFile).build();
        try {
            this.mapper = ObjectMapper.forClass(configClass).bindToNew();
        } catch (final ObjectMappingException e) {
            throw new RuntimeException("Failed to construct mapper for config class [" + configClass + "]!");
        }
        this.root = CommentedConfigurationNode.root(options);
        if (Files.notExists(configFile)) {
            try {
                this.save();
            } catch (final IOException | ObjectMappingException e) {
                throw new RuntimeException("Failed to save config for class [" + configClass + "] from [" + configFile + "]!", e);
            }
        }
    }

    public Class<T> getConfigClass() {
        return configClass;
    }

    public Path getConfigFile() {
        return this.configFile;
    }

    public T getConfig() {
        return this.config;
    }

    public void load() throws IOException, ObjectMappingException {
        this.root = this.loader.load();
        this.config = this.mapper.populate(this.root);
    }

    public void save() throws IOException, ObjectMappingException {
        this.mapper.serialize(this.root);
        this.loader.save(this.root);
    }
}
