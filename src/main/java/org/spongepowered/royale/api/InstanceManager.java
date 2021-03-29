package org.spongepowered.royale.api;

import org.spongepowered.api.ResourceKey;
import org.spongepowered.royale.instance.InstanceType;
import org.spongepowered.royale.instance.exception.UnknownInstanceException;

import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public interface InstanceManager {

    //TODO type is impl
    CompletableFuture<Instance> createInstance(final ResourceKey key, final InstanceType type, final boolean force);

    CompletableFuture<Boolean> unloadInstance(final ResourceKey key);

    void startInstance(final ResourceKey key) throws UnknownInstanceException;

    void endInstance(final ResourceKey key, final boolean force) throws UnknownInstanceException;

    Optional<Instance> getInstance(final ResourceKey key);

    Collection<Instance> getAll();

}
