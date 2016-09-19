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

import com.flowpowered.math.vector.Vector3i;
import com.google.common.base.Objects;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import ninja.leaping.configurate.objectmapping.ObjectMappingException;
import org.spongepowered.api.CatalogType;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.item.inventory.ItemStackSnapshot;
import org.spongepowered.api.text.TextTemplate;
import org.spongepowered.api.util.ResettableBuilder;
import org.spongepowered.api.util.annotation.CatalogedBy;
import org.spongepowered.special.Constants;
import org.spongepowered.special.configuration.MappedConfigurationAdapter;
import org.spongepowered.special.instance.configuration.InstanceTypeConfiguration;
import org.spongepowered.special.instance.gen.InstanceMutator;
import org.spongepowered.special.instance.gen.InstanceMutatorPipeline;
import org.spongepowered.special.instance.gen.InstanceMutatorRegistryModule;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@CatalogedBy(InstanceTypes.class)
public final class InstanceType implements CatalogType {

    private String id, name;
    private TextTemplate nameTemplate, roundStartTemplate, roundEndTemplate;
    private List<ItemStackSnapshot> defaultItems;
    private long roundStartLength, roundLength, roundEndLength;
    private int mapWidth, mapLength;
    private int automaticStartPlayerCount;
    private Vector3i min, max, size;
    private InstanceMutatorPipeline mutatorPipeline;

    private InstanceType(String id, Builder builder) {
        this.id = id;
        this.name = builder.name == null ? id : builder.name;
        this.nameTemplate = builder.nameTemplate;
        this.mapWidth = builder.mapWidth;
        this.mapLength = builder.mapLength;
        this.mutatorPipeline = new InstanceMutatorPipeline();
        this.mutatorPipeline.getMutators().addAll(builder.mutators);
        this.defaultItems = builder.defaultItems;
        this.roundStartTemplate = builder.roundStartTemplate;
        this.roundStartLength = builder.roundStartLength;
        this.roundLength = builder.roundLength;
        this.roundEndTemplate = builder.roundEndTemplate;
        this.roundEndLength = builder.roundEndLength;
        this.automaticStartPlayerCount = builder.automaticStartPlayerCount;
        this.size = new Vector3i(builder.mapWidth, 256, builder.mapLength);
        this.min = new Vector3i(-builder.mapWidth / 2, 0, -builder.mapLength / 2);
        this.max = this.min.add(this.size).sub(1, 1, 1);
    }

    public static Builder builder() {
        return Sponge.getRegistry().createBuilder(Builder.class);
    }

    @Override
    public String getId() {
        return this.id;
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

    public TextTemplate getNameTemplate() {
        return this.nameTemplate;
    }

    public TextTemplate getRoundStartTemplate() {
        return this.roundStartTemplate;
    }

    public TextTemplate getRoundEndTemplate() {
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

    public InstanceMutatorPipeline getMutatorPipeline() {
        return this.mutatorPipeline;
    }

    public void injectFromConfig(InstanceTypeConfiguration value) {
        this.name = value.general.name;
        this.nameTemplate = value.general.nameTemplate;
        this.mapWidth = value.general.mapWidth;
        this.mapLength = value.general.mapLength;
        this.mutatorPipeline.getMutators().clear();
        this.mutatorPipeline.getMutators().addAll(InstanceMutatorRegistryModule.getInstance().mapStrings(value.general.mapMutators));
        this.defaultItems.clear();
        this.defaultItems.addAll(value.round.defaultItems);
        this.roundStartTemplate = value.round.startTemplate;
        this.roundEndTemplate = value.round.endTemplate;
        this.roundStartLength = value.round.start;
        this.roundLength = value.round.length;
        this.roundEndLength = value.round.end;
        this.size = new Vector3i(this.mapWidth, 256, this.mapLength);
        this.min = new Vector3i(-this.mapWidth / 2, 0, -this.mapLength / 2);
        this.max = this.min.add(this.size).sub(1, 1, 1);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        InstanceType instanceType = (InstanceType) o;
        return java.util.Objects.equals(this.id, instanceType.id);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(this.id);
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(this)
                .add("id", this.id)
                .add("name", this.name)
                .add("nameTemplate", this.nameTemplate)
                .add("mapWidth", this.mapWidth)
                .add("mapLength", this.mapLength)
                .add("mutatorPipeline", this.mutatorPipeline)
                .add("defaultItems", this.defaultItems)
                .add("roundStartLength", this.roundStartLength)
                .add("roundStartTemplate", this.roundStartTemplate)
                .add("roundLength", this.roundLength)
                .add("roundEndLength", this.roundEndLength)
                .add("roundEndTemplate", this.roundEndTemplate)
                .toString();
    }

    public static final class Builder implements ResettableBuilder<InstanceType, Builder> {

        String name;
        TextTemplate nameTemplate, roundStartTemplate, roundEndTemplate;
        List<ItemStackSnapshot> defaultItems;
        int mapLength, mapWidth;
        long roundStartLength, roundLength, roundEndLength;
        int automaticStartPlayerCount;
        Set<InstanceMutator> mutators;

        public Builder() {
            reset();
        }

        @Override
        public Builder from(InstanceType value) {
            this.name = value.name;
            this.nameTemplate = value.nameTemplate;
            this.mapWidth = value.size.getX();
            this.mapLength = value.size.getZ();
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

        public Builder from(InstanceTypeConfiguration value) {
            this.name = value.general.name;
            this.nameTemplate = value.general.nameTemplate;
            this.mapWidth = value.general.mapWidth;
            this.mapLength = value.general.mapLength;
            this.mutators = InstanceMutatorRegistryModule.getInstance().mapStrings(value.general.mapMutators);
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
            this.mapWidth = Constants.Map.DEFAULT_MAP_WIDTH;
            this.mapLength = Constants.Map.DEFAULT_MAP_LENGTH;
            this.mutators = Constants.Map.DEFAULT_MAP_MUTATORS;
            this.defaultItems = Constants.Map.Round.DEFAULT_ITEMS;
            this.roundStartTemplate = Constants.Map.Round.DEFAULT_TEXT_TEMPLATE_START;
            this.roundEndTemplate = Constants.Map.Round.DEFAULT_TEXT_TEMPLATE_END;
            this.roundStartLength = Constants.Map.Round.DEFAULT_START_LENGTH;
            this.roundLength = Constants.Map.Round.DEFAULT_LENGTH;
            this.roundEndLength = Constants.Map.Round.DEFAULT_END_LENGTH;
            this.automaticStartPlayerCount = Constants.Map.Round.DEFAULT_AUTOMATIC_START_PLAYER_COUNT;
            return this;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder nameTemplate(TextTemplate template) {
            this.nameTemplate = template;
            return this;
        }

        public Builder roundStartTemplate(TextTemplate template) {
            this.roundStartTemplate = template;
            return this;
        }

        public Builder roundStartLength(long length) {
            this.roundStartLength = length;
            return this;
        }

        public Builder roundLength(long length) {
            this.roundStartLength = length;
            return this;
        }

        public Builder roundEndTemplate(TextTemplate template) {
            this.roundEndTemplate = template;
            return this;
        }

        public Builder roundEndLength(long length) {
            this.roundEndLength = length;
            return this;
        }

        public Builder item(ItemStackSnapshot item) {
            checkNotNull(item);
            this.defaultItems.add(item);
            return this;
        }

        public Builder items(Collection<ItemStackSnapshot> items) {
            checkNotNull(items);
            this.defaultItems.addAll(items);
            return this;
        }

        public Builder width(int width) {
            if (width < 0) {
                width = Constants.Map.DEFAULT_MAP_WIDTH;
            }
            this.mapWidth = width;
            return this;
        }

        public Builder length(int length) {
            if (length < 0) {
                length = Constants.Map.DEFAULT_MAP_LENGTH;
            }
            this.mapLength = length;
            return this;
        }

        public Builder mutator(InstanceMutator mutator) {
            this.mutators.add(checkNotNull(mutator));
            return this;
        }

        public Builder automaticStartPlayerCount(int playerCount) {
            this.automaticStartPlayerCount = playerCount;
            return this;
        }

        public Builder mutator(String mutator_id) {
            Optional<InstanceMutator> mutator = InstanceMutatorRegistryModule.getInstance().getById(mutator_id);
            if (mutator.isPresent()) {
                this.mutators.add(mutator.get());
            }
            return this;
        }

        public InstanceType build(String id, String name) throws IOException, ObjectMappingException {
            checkNotNull(id);
            checkNotNull(this.nameTemplate);
            checkNotNull(this.roundStartTemplate);
            checkNotNull(this.roundEndTemplate);
            this.name(name);

            return this.build(id);
        }

        public InstanceType build(String id) throws IOException, ObjectMappingException {
            checkNotNull(id);
            checkNotNull(this.nameTemplate);
            checkNotNull(this.roundStartTemplate);
            checkNotNull(this.roundEndTemplate);

            final Path configPath = Constants.Map.PATH_CONFIG_INSTANCE_TYPES.resolve(id + ".conf");
            final MappedConfigurationAdapter<InstanceTypeConfiguration> adapter = new MappedConfigurationAdapter<>(InstanceTypeConfiguration
                    .class, Constants.Map.DEFAULT_OPTIONS, configPath);

            adapter.load();
            final InstanceTypeConfiguration config = adapter.getConfig();
            config.general.name = this.name;
            config.general.nameTemplate = this.nameTemplate;
            config.general.mapWidth = this.mapWidth;
            config.general.mapLength = this.mapLength;
            config.general.mapMutators.clear();
            config.general.mapMutators.addAll(this.mutators.stream().map(InstanceMutator::getId).collect(Collectors.toList()));

            config.round.defaultItems.clear();
            config.round.defaultItems.addAll(this.defaultItems);
            config.round.start = this.roundStartLength;
            config.round.startTemplate = this.roundStartTemplate;
            config.round.length = this.roundLength;
            config.round.end = this.roundEndLength;
            config.round.endTemplate = this.roundEndTemplate;
            config.round.automaticStartPlayerCount = this.automaticStartPlayerCount;
            adapter.save();

            return new InstanceType(id, this);
        }
    }
}
