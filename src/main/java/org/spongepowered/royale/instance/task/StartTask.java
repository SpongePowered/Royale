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
package org.spongepowered.royale.instance.task;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import org.spongepowered.api.scheduler.ScheduledTask;
import org.spongepowered.api.world.server.ServerWorld;
import org.spongepowered.royale.instance.InstanceImpl;

import java.time.Duration;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public final class StartTask extends InstanceTask {

    private final List<Title> startTitles;

    private int seconds = 0;

    private ScheduledTask handle;

    public StartTask(final InstanceImpl instance) {
        super(instance);

        this.startTitles = new LinkedList<>();

        final Title.Times times = Title.Times.of(Duration.ZERO, Duration.ofMillis(600), Duration.ofMillis(400));

        for (long i = instance.getType().getRoundStartLength(); i > 0; i--) {

            this.startTitles.add(Title.title(Component.text(i, NamedTextColor.DARK_RED), Component.empty(), times));

            if (i == 3) {
                this.startTitles.add(Title.title(Component.text("2", NamedTextColor.RED), Component.empty(), times));
                this.startTitles.add(Title.title(Component.text("1", NamedTextColor.GOLD), Component.empty(), times));
                this.startTitles.add(Title.title(instance.getType().getRoundStartTemplate().parse(null, Collections.emptyMap()),
                        Component.empty(),
                        times));
                break;
            }
        }
    }

    @Override
    public void accept(final ScheduledTask task) {
        this.handle = task;

        final ServerWorld world = this.getInstance().world();

        // Make sure the world is still around and loaded
        if (world != null && world.isLoaded()) {

            if (this.seconds >= this.startTitles.size()) {
                if (this.cancel()) {
                    this.getInstance().advance();
                }
                return;
            }

            // Make sure a player ref isn't still here
            world.players().forEach(player -> player.showTitle(this.startTitles.get(this.seconds)));

            this.seconds++;
        }
    }

    @Override
    public boolean cancel() {
        return this.handle.cancel();
    }
}
