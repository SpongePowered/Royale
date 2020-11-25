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

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.spongepowered.api.NamedCatalogType;
import org.spongepowered.api.ResourceKey;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.item.inventory.ItemStackSnapshot;
import org.spongepowered.api.util.NamedCatalogBuilder;
import org.spongepowered.math.vector.Vector3i;
import org.spongepowered.royale.Constants;
import org.spongepowered.royale.Royale;
import org.spongepowered.royale.configuration.MappedConfigurationAdapter;
import org.spongepowered.royale.instance.configuration.InstanceTypeConfiguration;
import org.spongepowered.royale.instance.gen.InstanceMutator;
import org.spongepowered.royale.instance.gen.InstanceMutatorPipeline;
import org.spongepowered.royale.template.ComponentTemplate;

import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public final class InstanceType implements NamedCatalogType {

    private final ResourceKey key;
    private final InstanceMutatorPipeline mutatorPipeline;
    private String name;
    private ComponentTemplate nameTemplate, roundStartTemplate, roundEndTemplate;
    private final List<ItemStackSnapshot> defaultItems;
    private long roundStartLength, roundLength, roundEndLength;
    private int centerX, centerZ, minX, minY, minZ, maxX, maxY, maxZ, automaticStartPlayerCount, worldBorderX, worldBorderZ, worldBorderRadius;
    private Vector3i min, max, size;

    private InstanceType(final Builder builder) {
        this.key = builder.key;
        this.name = builder.name;
        this.nameTemplate = builder.nameTemplate;
        this.centerX = builder.centerX;
        this.centerZ = builder.centerZ;
        this.minX = builder.minX;
        this.minY = builder.minY;
        this.minZ = builder.minZ;
        this.maxX = builder.maxX;
        this.maxY = builder.maxY;
        this.maxZ = builder.maxZ;
        this.mutatorPipeline = new InstanceMutatorPipeline();
        this.mutatorPipeline.getMutators().addAll(builder.mutators);
        this.defaultItems = builder.defaultItems;
        this.roundStartTemplate = builder.roundStartTemplate;
        this.roundStartLength = builder.roundStartLength;
        this.roundLength = builder.roundLength;
        this.roundEndTemplate = builder.roundEndTemplate;
        this.roundEndLength = builder.roundEndLength;
        this.automaticStartPlayerCount = builder.automaticStartPlayerCount;
        this.size = new Vector3i(builder.maxX - builder.minX, builder.maxY - builder.minY, builder.maxZ - builder.minZ);
        this.min = new Vector3i(builder.centerX + builder.minX, builder.minY, builder.centerZ + builder.minZ);
        this.max = new Vector3i(builder.centerX + builder.maxX, builder.maxY, builder.centerZ + builder.maxZ).sub(1, 1, 1);
        this.worldBorderX = builder.worldBorderX;
        this.worldBorderZ = builder.worldBorderZ;
        this.worldBorderRadius = builder.worldBorderRadius;
    }

    public static Builder builder() {
        return Sponge.getRegistry().getBuilderRegistry().provideBuilder(Builder.class);
    }

    @Override
    public ResourceKey getKey() {
        return this.key;
    }

    @Override
    public String getName() {
        return this.name;
    }

    public long getRoundStartLength() {
        return this.roundStartLength;
    }

    public long getRoundLength() {
        return this.roundLength;
    }

    public long getRoundEndLength() {
        return this.roundEndLength;
    }

    public ComponentTemplate getNameTemplate() {
        return this.nameTemplate;
    }

    public ComponentTemplate getRoundStartTemplate() {
        return this.roundStartTemplate;
    }

    public ComponentTemplate getRoundEndTemplate() {
        return this.roundEndTemplate;
    }

    public int getAutomaticStartPlayerCount() {
        return this.automaticStartPlayerCount;
    }

    public List<ItemStackSnapshot> getDefaultItems() {
        return this.defaultItems;
    }

    /**
     * Inclusive.
     */
    public Vector3i getBlockMin() {
        return this.min;
    }

    /**
     * Inclusive.
     */
    public Vector3i getBlockMax() {
        return this.max;
    }

    public Vector3i getBlockSize() {
        return this.size;
    }

    public int getWorldBorderX() {
        return this.worldBorderX;
    }

    public int getWorldBorderZ() {
        return this.worldBorderZ;
    }

    public int getWorldBorderRadius() {
        return this.worldBorderRadius;
    }

    public InstanceMutatorPipeline getMutatorPipeline() {
        return this.mutatorPipeline;
    }

    public void injectFromConfig(final InstanceTypeConfiguration value) {
        this.name = value.general.name;
        this.nameTemplate = value.general.nameTemplate;
        this.centerX = value.general.centerX;
        this.centerZ = value.general.centerZ;
        this.minX = value.general.minX;
        this.minY = value.general.minY;
        this.minZ = value.general.minZ;
        this.maxX = value.general.maxX;
        this.maxY = value.general.maxY;
        this.maxZ = value.general.maxZ;
        this.mutatorPipeline.getMutators().clear();
        value.general.mapMutators.stream().map(k -> Sponge.getRegistry().getCatalogRegistry().get(InstanceMutator.class, k).get())
                .forEach(this.mutatorPipeline.getMutators()::add);
        this.defaultItems.clear();
        this.defaultItems.addAll(value.round.defaultItems);
        this.roundStartTemplate = value.round.startTemplate;
        this.roundEndTemplate = value.round.endTemplate;
        this.roundStartLength = value.round.start;
        this.roundLength = value.round.length;
        this.roundEndLength = value.round.end;
        this.automaticStartPlayerCount = value.round.automaticStartPlayerCount;
        this.worldBorderX = value.general.worldBorderCenterX;
        this.worldBorderZ = value.general.worldBorderCenterZ;
        this.worldBorderRadius = value.general.worldBorderRadius;
        this.size = new Vector3i(this.maxX - this.minX, this.maxY - this.minY, this.maxZ - this.minZ);
        this.min = new Vector3i(this.centerX + this.minX, this.minY, this.centerZ + this.minZ);
        this.max = new Vector3i(this.centerX + this.maxX, this.maxY, this.centerZ + this.maxZ).sub(1, 1, 1);
    }

    public void injectIntoConfig(final InstanceTypeConfiguration config) {

        config.general.name = this.name;
        config.general.nameTemplate = this.nameTemplate;
        config.general.centerX = this.centerX;
        config.general.centerZ = this.centerZ;
        config.general.minX = this.minX;
        config.general.minY = this.minY;
        config.general.minZ = this.minZ;
        config.general.maxX = this.maxX;
        config.general.maxY = this.maxY;
        config.general.maxZ = this.maxZ;
        config.general.worldBorderCenterX = this.worldBorderX;
        config.general.worldBorderCenterZ = this.worldBorderZ;
        config.general.worldBorderRadius = this.worldBorderRadius;
        config.general.mapMutators.clear();
        config.general.mapMutators.addAll(this.mutatorPipeline.getMutators().stream().map(InstanceMutator::getKey).collect(Collectors.toList()));

        config.round.defaultItems.clear();
        config.round.defaultItems.addAll(this.defaultItems);
        config.round.start = this.roundStartLength;
        config.round.startTemplate = this.roundStartTemplate;
        config.round.length = this.roundLength;
        config.round.end = this.roundEndLength;
        config.round.endTemplate = this.roundEndTemplate;
        config.round.automaticStartPlayerCount = this.automaticStartPlayerCount;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final InstanceType instanceType = (InstanceType) o;
        return java.util.Objects.equals(this.key, instanceType.key);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(this.key);
    }

    @Override
    public String toString() {
        return "InstanceType{" +
                "key='" + this.key + '\'' +
                ", name='" + this.name + '\'' +
                ", nameTemplate=" + this.nameTemplate +
                ", roundStartTemplate=" + this.roundStartTemplate +
                ", roundEndTemplate=" + this.roundEndTemplate +
                ", defaultItems=" + this.defaultItems +
                ", roundStartLength=" + this.roundStartLength +
                ", roundLength=" + this.roundLength +
                ", roundEndLength=" + this.roundEndLength +
                ", centerX=" + this.centerX +
                ", centerZ=" + this.centerZ +
                ", minX=" + this.minX +
                ", minY=" + this.minY +
                ", minZ=" + this.minZ +
                ", maxX=" + this.maxX +
                ", maxY=" + this.maxY +
                ", maxZ=" + this.maxZ +
                ", automaticStartPlayerCount=" + this.automaticStartPlayerCount +
                ", min=" + this.min +
                ", max=" + this.max +
                ", size=" + this.size +
                ", mutatorPipeline=" + this.mutatorPipeline +
                '}';
    }

    public static final class Builder implements NamedCatalogBuilder<InstanceType, Builder> {

        ResourceKey key;
        String name;
        ComponentTemplate nameTemplate, roundStartTemplate, roundEndTemplate;
        List<ItemStackSnapshot> defaultItems;
        long roundStartLength, roundLength, roundEndLength;
        int automaticStartPlayerCount;
        Set<InstanceMutator> mutators;

        private int centerX;
        private int centerZ;
        private int minX;
        private int minY;
        private int minZ;
        private int maxX;
        private int maxY;
        private int maxZ;
        private int worldBorderX;
        private int worldBorderZ;
        private int worldBorderRadius;

        public Builder() {
            this.reset();
        }

        public Builder from(final InstanceType value) {
            this.key = value.key;
            this.name = value.name;
            this.nameTemplate = value.nameTemplate;
            this.centerX = value.centerX;
            this.centerZ = value.centerZ;
            this.minX = value.minX;
            this.minY = value.minY;
            this.minZ = value.minZ;
            this.maxX = value.maxX;
            this.maxY = value.maxY;
            this.maxZ = value.maxZ;
            this.worldBorderX = value.worldBorderX;
            this.worldBorderZ = value.worldBorderZ;
            this.worldBorderRadius = value.worldBorderRadius;

            this.mutators = Sets.newHashSet(value.mutatorPipeline.getMutators());
            this.defaultItems = Lists.newLinkedList(value.defaultItems);
            this.roundStartTemplate = value.roundStartTemplate;
            this.roundEndTemplate = value.roundEndTemplate;
            this.roundStartLength = value.roundStartLength;
            this.roundLength = value.roundLength;
            this.roundEndLength = value.roundEndLength;
            this.automaticStartPlayerCount = value.automaticStartPlayerCount;
            return this;
        }

        public Builder from(final InstanceTypeConfiguration value) {
            this.name = value.general.name;
            this.nameTemplate = value.general.nameTemplate;
            this.centerX = value.general.centerX;
            this.centerZ = value.general.centerZ;
            this.minX = value.general.minX;
            this.minY = value.general.minY;
            this.minZ = value.general.minZ;
            this.maxX = value.general.maxX;
            this.maxY = value.general.maxY;
            this.maxZ = value.general.maxZ;
            this.worldBorderX = value.general.worldBorderCenterX;
            this.worldBorderZ = value.general.worldBorderCenterZ;
            this.worldBorderRadius = value.general.worldBorderRadius;
            this.mutators = value.general.mapMutators.stream()
                    .map(x -> Sponge.getRegistry().getCatalogRegistry().get(InstanceMutator.class, x)
                            .orElseThrow(() -> new IllegalArgumentException("Unknown mutator " + x)))
                    .collect(Collectors.toSet());
            this.defaultItems = Lists.newLinkedList(value.round.defaultItems);
            this.roundStartTemplate = value.round.startTemplate;
            this.roundEndTemplate = value.round.endTemplate;
            this.roundStartLength = value.round.start;
            this.roundLength = value.round.length;
            this.roundEndLength = value.round.end;
            this.automaticStartPlayerCount = value.round.automaticStartPlayerCount;
            return this;
        }

        @Override
        public Builder reset() {
            this.name = null;
            this.nameTemplate = Constants.Map.DEFAULT_TEXT_TEMPLATE_NAME;
            this.mutators = Constants.Map.DEFAULT_MAP_MUTATORS;
            this.defaultItems = Constants.Map.Round.DEFAULT_ITEMS;
            this.roundStartTemplate = Constants.Map.Round.DEFAULT_TEXT_TEMPLATE_START;
            this.roundEndTemplate = Constants.Map.Round.DEFAULT_TEXT_TEMPLATE_END;
            this.roundStartLength = Constants.Map.Round.DEFAULT_START_LENGTH;
            this.roundLength = Constants.Map.Round.DEFAULT_LENGTH;
            this.roundEndLength = Constants.Map.Round.DEFAULT_END_LENGTH;
            this.automaticStartPlayerCount = Constants.Map.Round.DEFAULT_AUTOMATIC_START_PLAYER_COUNT;
            this.centerX = Constants.Map.DEFAULT_CENTER_X;
            this.centerZ = Constants.Map.DEFAULT_CENTER_Z;
            this.minX = -Constants.Map.DEFAULT_MAP_WIDTH;
            this.minY = Constants.Map.DEFAULT_MAP_MIN_Y;
            this.minZ = -Constants.Map.DEFAULT_MAP_WIDTH;
            this.maxX = Constants.Map.DEFAULT_MAP_WIDTH;
            this.maxY = Constants.Map.DEFAULT_MAP_MAX_Y;
            this.maxZ = Constants.Map.DEFAULT_MAP_WIDTH;
            this.worldBorderX = Constants.Map.DEFAULT_WORLD_BORDER_CENTER_X;
            this.worldBorderZ = Constants.Map.DEFAULT_WORLD_BORDER_CENTER_Z;
            this.worldBorderRadius = Constants.Map.DEFAULT_WORLD_BORDER_RADIUS;
            return this;
        }

        @Override
        public Builder key(final ResourceKey key) {
            this.key = key;
            return this;
        }

        @Override
        public Builder name(final String name) {
            this.name = name;
            return this;
        }

        public Builder nameTemplate(final ComponentTemplate template) {
            this.nameTemplate = template;
            return this;
        }

        public Builder roundStartTemplate(final ComponentTemplate template) {
            this.roundStartTemplate = template;
            return this;
        }

        public Builder roundStartLength(final long length) {
            this.roundStartLength = length;
            return this;
        }

        public Builder roundLength(final long length) {
            this.roundStartLength = length;
            return this;
        }

        public Builder roundEndTemplate(final ComponentTemplate template) {
            this.roundEndTemplate = template;
            return this;
        }

        public Builder roundEndLength(final long length) {
            this.roundEndLength = length;
            return this;
        }

        public Builder item(final ItemStackSnapshot item) {
            Objects.requireNonNull(item);
            this.defaultItems.add(item);
            return this;
        }

        public Builder items(final Collection<ItemStackSnapshot> items) {
            Objects.requireNonNull(items);
            this.defaultItems.addAll(items);
            return this;
        }

        public Builder mutator(final InstanceMutator mutator) {
            Objects.requireNonNull(mutator);
            this.mutators.add(mutator);
            return this;
        }

        public Builder automaticStartPlayerCount(final int playerCount) {
            this.automaticStartPlayerCount = playerCount;
            return this;
        }

        public Builder mutator(final ResourceKey key) {
            Objects.requireNonNull(key);
            final Optional<InstanceMutator> mutator = Sponge.getRegistry().getCatalogRegistry().get(InstanceMutator.class, key);
            mutator.ifPresent(instanceMutator -> this.mutators.add(instanceMutator));
            return this;
        }

        @Override
        public InstanceType build() {
            Objects.requireNonNull(this.key);
            Objects.requireNonNull(this.nameTemplate);
            Objects.requireNonNull(this.roundStartTemplate);
            Objects.requireNonNull(this.roundEndTemplate);

            return new InstanceType(this);
        }
    }
}
