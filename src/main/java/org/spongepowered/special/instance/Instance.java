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

import org.spongepowered.api.scheduler.Task;
import org.spongepowered.api.world.World;
import org.spongepowered.special.Constants;
import org.spongepowered.special.Special;
import org.spongepowered.special.instance.gen.MapMutatorPipeline;
import org.spongepowered.special.instance.task.StartCountdown;

import java.lang.ref.WeakReference;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

public final class Instance {

    private final String worldName;
    private final InstanceType instanceType;
    private final WeakReference<World> worldRef;

    private boolean isRunning = false;
    private Task startTask, roundTask;

    public Instance(String worldName, InstanceType instanceType, World world) {
        this.worldName = worldName;
        this.instanceType = instanceType;
        this.worldRef = new WeakReference<>(world);
    }

    public void start() {

        final World world = worldRef.get();

        if (world == null) {
            throw new RuntimeException("Attempt to start an instance whose world no longer exists!");
        }
        
        MapMutatorPipeline pipeline = this.instanceType.getMutatorPipeline();
        pipeline.mutate(world, this);

        this.startTask = Task.builder()
                .execute(new StartCountdown(this))
                .interval(1, TimeUnit.SECONDS)
                .name(Constants.Meta.ID + " - Start Countdown - " + world.getName())
                .submit(Special.instance);

        isRunning = true;

        // Kickup round task here
    }

    public void stop() {
        // TODO Kickup stop code here
    }

    public InstanceType getType() {
        return this.instanceType;
    }

    public Optional<World> getHandle() {
        return Optional.ofNullable(this.worldRef.get());
    }

    public boolean isInstanceRunning() {
        return this.isRunning;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final Instance instance = (Instance) o;
        return Objects.equals(this.worldName, instance.worldName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.worldName);
    }

    @Override
    public String toString() {
        return com.google.common.base.Objects.toStringHelper(this)
                .add("name", this.worldName)
                .add("type", this.instanceType)
                .toString();
    }
}
