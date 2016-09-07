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

import com.flowpowered.math.vector.Vector3d;
import com.google.common.base.Objects;
import org.spongepowered.api.CatalogType;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.item.inventory.ItemStackSnapshot;
import org.spongepowered.api.text.TextTemplate;
import org.spongepowered.api.util.ResettableBuilder;
import org.spongepowered.special.Constants;
import org.spongepowered.special.instance.configuration.InstanceConfiguration;

import java.util.Collection;
import java.util.List;

public final class InstanceType implements CatalogType {

    private final String id, name, template;
    private final TextTemplate nameTemplate, roundStartTemplate, roundEndTemplate;
    private final List<ItemStackSnapshot> defaultItems;
    private final long roundStartLength, roundLength, roundEndLength;

    private InstanceType(String id, Builder builder) {
        this.id = id;
        this.name = builder.name;
        this.template = builder.template;
        this.nameTemplate = builder.nameTemplate;
        this.roundStartTemplate = builder.roundStartTemplate;
        this.roundStartLength = builder.roundStartLength;
        this.roundLength = builder.roundLength;
        this.roundEndTemplate = builder.roundEndTemplate;
        this.roundEndLength = builder.roundEndLength;
        this.defaultItems = builder.defaultItems;
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

    public String getTemplate() {
        return template;
    }

    public long getRoundStartLength() {
        return roundStartLength;
    }

    public long getRoundLength() {
        return roundLength;
    }

    public long getRoundEndLength() {
        return roundEndLength;
    }

    public TextTemplate getNameTemplate() {
        return nameTemplate;
    }

    public TextTemplate getRoundStartTemplate() {
        return roundStartTemplate;
    }

    public TextTemplate getRoundEndTemplate() {
        return roundEndTemplate;
    }

    public List<ItemStackSnapshot> getDefaultItems() {
        return defaultItems;
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
                .add("template", this.template)
                .add("nameTemplate", this.nameTemplate)
                .add("roundStartLength", this.roundStartLength)
                .add("roundStartTemplate", this.roundStartTemplate)
                .add("roundLength", this.roundLength)
                .add("roundEndLength", this.roundEndLength)
                .add("roundEndTemplate", this.roundEndTemplate)
                .add("defaultItems", this.defaultItems)
                .toString();
    }

    public static final class Builder implements ResettableBuilder<InstanceType, Builder> {

        String name, template;
        TextTemplate nameTemplate, roundStartTemplate, roundEndTemplate;
        List<ItemStackSnapshot> defaultItems;
        List<Vector3d> possibleSpawns;
        int mapLength, mapWidth;
        long roundStartLength, roundLength, roundEndLength;

        public Builder() {
            reset();
        }

        @Override
        public Builder from(InstanceType value) {
            this.name = value.name;
            this.template = value.template;
            this.nameTemplate = value.nameTemplate;
            this.roundStartTemplate = value.roundStartTemplate;
            this.roundEndTemplate = value.roundEndTemplate;
            this.roundStartLength = value.roundStartLength;
            this.roundLength = value.roundLength;
            this.roundEndLength = value.roundEndLength;
            return this;
        }

        public Builder from(InstanceConfiguration value) {
            this.name = value.general.name;
            this.template = value.general.template;
            this.nameTemplate = value.general.nameTemplate;
            this.roundStartTemplate = value.round.startTemplate;
            this.roundEndTemplate = value.round.endTemplate;
            this.roundStartLength = value.round.start;
            this.roundLength = value.round.length;
            this.roundEndLength = value.round.end;
            return this;
        }

        @Override
        public Builder reset() {
            this.name = null;
            this.template = null;
            this.nameTemplate = Constants.Map.DEFAULT_TEXT_TEMPLATE_NAME;
            this.roundStartTemplate = Constants.Map.Round.DEFAULT_TEXT_TEMPLATE_START;
            this.roundEndTemplate = Constants.Map.Round.DEFAULT_TEXT_TEMPLATE_END;
            this.roundStartLength = Constants.Map.Round.DEFAULT_START_LENGTH;
            this.roundLength = Constants.Map.Round.DEFAULT_LENGTH;
            this.roundEndLength = Constants.Map.Round.DEFAULT_END_LENGTH;
            this.defaultItems = Constants.Map.Round.defaultItems;
            return this;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder template(String name) {
            this.template = name;
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

        public InstanceType build(String id) {
            checkNotNull(id);
            checkNotNull(this.name);
            checkNotNull(this.template);
            checkNotNull(this.nameTemplate);
            checkNotNull(this.roundStartTemplate);
            checkNotNull(this.roundEndTemplate);

            return new InstanceType(id, this);
        }
    }
}
