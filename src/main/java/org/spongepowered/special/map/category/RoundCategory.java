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
package org.spongepowered.special.map.category;

import ninja.leaping.configurate.objectmapping.Setting;
import ninja.leaping.configurate.objectmapping.serialize.ConfigSerializable;
import org.spongepowered.api.text.TextTemplate;
import org.spongepowered.special.Constants;
import org.spongepowered.special.configuration.AbstractConfigurationCategory;

@ConfigSerializable
public final class RoundCategory extends AbstractConfigurationCategory {

    @Setting(comment = "Countdown until round starts (in seconds)")
    public long countdown = 5;

    @Setting(value = "countdown-template", comment = "Template used to display the round start.")
    public TextTemplate countdownTemplate = Constants.Map.TEMPLATE_ROUND_START_TITLE;

    @Setting(value = "length", comment = "Length of the round (in seconds)")
    public long length = 300;

    @Setting(value = "end", comment = "Length of the time after round ends until everyone is kicked (in seconds)")
    public long end = 10;

    @Setting(value = "end-template", comment = "Template used to display the winner.")
    public TextTemplate endTemplate = Constants.Map.TEMPLATE_ANNOUNCE_WINNER_TITLE;
}
