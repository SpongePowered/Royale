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

import org.spongepowered.api.scheduler.Task;
import org.spongepowered.api.world.World;
import org.spongepowered.special.Constants;
import org.spongepowered.special.Special;
import org.spongepowered.special.task.StartCountdown;

import java.lang.ref.WeakReference;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * Represents a running instance
 */
public final class Map {
    final String worldName;
    final MapType mapType;
    final WeakReference<World> worldRef;

    boolean isRunning = false;
    Task startTask, roundTask;

    public Map(String worldName, MapType mapType, World world) {
        this.worldName = worldName;
        this.mapType = mapType;
        this.worldRef = new WeakReference<>(world);
    }

    public void start() {

        final World world = worldRef.get();

        if (world == null) {
            throw new RuntimeException("Attempt to start an instance whose world no longer exists!");
        }

        this.startTask = Task.builder()
                .execute(new StartCountdown(mapType, world))
                .interval(1, TimeUnit.SECONDS)
                .name(Constants.Meta.ID + " - Start Countdown - " + world.getName())
                .submit(Special.instance);

        isRunning = true;

        // Kickup round task here
    }

    public void stop() {
        // TODO Kickup stop code here
    }

    public boolean isInstanceRunning() {
        return this.isRunning;
    }

    public Optional<Task> getStartTask() {
        return Optional.ofNullable(this.startTask);
    }

    public Optional<Task> getRoundTask() {
        return Optional.ofNullable(this.roundTask);
    }
}
