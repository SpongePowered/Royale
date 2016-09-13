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

import org.spongepowered.api.Server;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.world.SerializationBehaviors;
import org.spongepowered.api.world.World;
import org.spongepowered.special.Constants;
import org.spongepowered.special.Special;
import org.spongepowered.special.instance.exception.InstanceAlreadyExistsException;
import org.spongepowered.special.instance.exception.UnknownInstanceException;
import org.spongepowered.special.instance.gen.MapMutatorPipeline;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class InstanceManager {

    // World Name -> Instance
    private final Map<String, Instance> instances = new HashMap<>();

    // Instance Type -> List(Instances)
    private final Map<InstanceType, List<Instance>> instancesByTypes = new HashMap<>();

    public void createInstance(String instanceName, InstanceType type) throws Exception {
        if (this.instances.containsKey(instanceName)) {
            throw new InstanceAlreadyExistsException(instanceName);
        }

        Instance instance;
        World world = Sponge.getServer().getWorld(instanceName).orElse(null);

        if (world == null) {
            world = Sponge.getServer().loadWorld(instanceName).orElse(null);
            if (world == null) {
                throw new IOException("Failed to load instance [" + instanceName + "]!");
            }

            world.setKeepSpawnLoaded(true);
            world.save();

            world.setSerializationBehavior(SerializationBehaviors.NONE);

            instance = new Instance(instanceName, type, world);

            this.instances.put(world.getName(), instance);
            List<Instance> instances = this.instancesByTypes.get(type);
            if (instances == null) {
                instances = new LinkedList<>();
                this.instancesByTypes.put(type, instances);

            }
            instances.add(instance);

            final MapMutatorPipeline pipeline = type.getMutatorPipeline();
            pipeline.mutate(world, instance);
        } else {
            world.setKeepSpawnLoaded(true);
            world.save();

            world.setSerializationBehavior(SerializationBehaviors.NONE);

            instance = new Instance(instanceName, type, world);

            this.instances.put(world.getName(), instance);
            List<Instance> instances = this.instancesByTypes.get(type);
            if (instances == null) {
                instances = new LinkedList<>();
                this.instancesByTypes.put(type, instances);

            }
            instances.add(instance);

            final MapMutatorPipeline pipeline = type.getMutatorPipeline();
            pipeline.mutate(world, instance);
        }

        Sponge.getEventManager().registerListeners(Special.instance, instance);
    }

    /**
     * Advances the {@link Instance} associated by name to the pre start state.
     *
     * @param instanceName The name of the instance
     * @throws UnknownInstanceException If the instance isn't known
     */
    public void startInstance(String instanceName) throws UnknownInstanceException {
        final Instance instance = this.instances.get(instanceName);
        if (instance == null) {
            throw new UnknownInstanceException(instanceName);
        }

        instance.advanceTo(Instance.State.PRE_START);
    }

    public void endInstance(String instanceName, boolean force) throws UnknownInstanceException {
        final Instance instance = this.instances.get(instanceName);
        if (instance == null) {
            throw new UnknownInstanceException(instanceName);
        }

        if (force) {
            instance.advanceTo(Instance.State.FORCE_STOP);
        } else {
            instance.advanceTo(Instance.State.PRE_END);
        }
    }

    protected void unloadInstance(Instance instance) {
        final Server server = Sponge.getServer();
        final World world = instance.getHandle().orElse(null);

        if (world == null) {
            return;
        }



        final World lobby = server.getWorld(Constants.Map.Lobby.DEFAULT_LOBBY_NAME).orElseThrow(() -> new RuntimeException("Lobby world was not "
                + "found!"));

        // Move everyone out
        for (Player player : world.getPlayers()) {
            player.setLocation(lobby.getSpawnLocation());
        }

        Sponge.getEventManager().unregisterListeners(instance);

        this.instances.remove(instance.getName());
        final List<Instance> instancesForType = this.instancesByTypes.get(instance.getType());
        if (instancesForType != null) {
            instancesForType.remove(instance);

            if (instancesForType.size() == 0) {
                this.instancesByTypes.remove(instance.getType());
            }
        }

        if (!server.unloadWorld(world)) {
            throw new RuntimeException("Failed to unload instance world!"); // TODO Specialized exception
        }
    }

    public Optional<Instance> getInstance(String instanceName) {
        checkNotNull(instanceName);
        return Optional.ofNullable(this.instances.get(instanceName));
    }

    public Collection<Instance> getInstances(InstanceType type) {
        checkNotNull(type);
        List<Instance> instances = this.instancesByTypes.get(type);
        if (instances == null) {
            instances = new LinkedList<>();
        }
        return Collections.unmodifiableCollection(instances);
    }

    public Collection<Instance> getAll() {
        return Collections.unmodifiableCollection(this.instances.values());
    }
}
