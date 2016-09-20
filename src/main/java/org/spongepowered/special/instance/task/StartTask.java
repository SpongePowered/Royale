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
package org.spongepowered.special.instance.task;

import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.scheduler.Task;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.text.title.Title;
import org.spongepowered.api.world.World;
import org.spongepowered.special.instance.Instance;

import java.util.LinkedList;
import java.util.List;

public final class StartTask extends InstanceTask {

    private final List<Title> startTitles = new LinkedList<>();

    private int seconds = 0;

    private Task handle;

    public StartTask(Instance instance) {
        super(instance);

        final Title.Builder builder = Title.builder()
                .stay(12)
                .fadeIn(0)
                .fadeOut(8);

        for (long i = instance.getType().getRoundStartLength(); i > 0; i--) {

            startTitles.add(builder.title(Text.of(TextColors.DARK_RED, i)).build());

            if (i == 3) {
                startTitles.add(builder.title(Text.of(TextColors.RED, "2")).build());
                startTitles.add(builder.title(Text.of(TextColors.GOLD, "1")).build());
                startTitles.add(builder.title(instance.getType().getRoundStartTemplate().apply().build()).build());
                break;
            }
        }
    }

    @Override
    public void cancel() {
        this.handle.cancel();
    }

    @Override
    public void accept(Task task) {
        this.handle = task;

        final World world = this.getInstance().getHandle().orElse(null);

        // TODO If world is null or not loaded, shut this handle down and log it.

        // Make sure the world is still around and loaded
        if (world != null && world.isLoaded()) {

            // Make sure a player ref isn't still here
            world.getPlayers().stream().filter(User::isOnline).forEach(player -> player.sendTitle(startTitles.get(seconds)));

            seconds++;

            if (seconds >= startTitles.size()) {
                this.cancel();
                this.getInstance().advance();
            }
        }
    }
}
