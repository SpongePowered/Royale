package org.spongepowered.royale.instance;

import net.kyori.adventure.identity.Identity;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.plain.PlainComponentSerializer;
import org.spongepowered.api.ResourceKey;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.block.BlockSnapshot;
import org.spongepowered.api.data.Keys;
import org.spongepowered.api.data.Transaction;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.Order;
import org.spongepowered.api.event.block.ChangeBlockEvent;
import org.spongepowered.api.event.block.InteractBlockEvent;
import org.spongepowered.api.event.block.entity.ChangeSignEvent;
import org.spongepowered.api.event.cause.entity.damage.DamageTypes;
import org.spongepowered.api.event.cause.entity.damage.source.DamageSource;
import org.spongepowered.api.event.entity.AttackEntityEvent;
import org.spongepowered.api.event.entity.ChangeEntityWorldEvent;
import org.spongepowered.api.event.entity.DamageEntityEvent;
import org.spongepowered.api.event.entity.DestructEntityEvent;
import org.spongepowered.api.event.entity.MoveEntityEvent;
import org.spongepowered.api.event.entity.living.player.RespawnPlayerEvent;
import org.spongepowered.api.event.filter.Getter;
import org.spongepowered.api.event.filter.cause.First;
import org.spongepowered.api.event.filter.cause.Root;
import org.spongepowered.api.event.network.ServerSideConnectionEvent;
import org.spongepowered.api.scheduler.Task;
import org.spongepowered.api.util.Ticks;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.server.ServerLocation;
import org.spongepowered.api.world.server.ServerWorld;
import org.spongepowered.royale.Constants;
import org.spongepowered.royale.Royale;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public final class EventHandler {

    @Listener(order = Order.LAST)
    public void onServerSideConnectionPlayer(final ServerSideConnectionEvent event, @First final ServerPlayer player) {
        // We only care about these events
        if (!(event instanceof ServerSideConnectionEvent.Join || event instanceof ServerSideConnectionEvent.Disconnect)) {
            return;
        }

        final ServerWorld world = player.world();
        final Instance instance = Royale.instance.getInstanceManager().getInstance(world.key()).orElse(null);

        if (instance != null) {
            if (instance.isPlayerRegistered(player.uniqueId())) {
                if (event instanceof ServerSideConnectionEvent.Join) {
                    // Player has played before, disconnected...let them come back as spectator
                    if (instance.isPlayerDead(player.uniqueId())) {
                        instance.spectate(player);
                    }
                } else {
                    // Player disconnecting instantly means they forfeit
                    instance.disqualifyPlayer(player);
                    if (instance.isRoundOver()) {
                        if (world.players().isEmpty()) {
                            instance.advanceTo(Instance.State.FORCE_STOP);
                        } else {
                            instance.advanceTo(Instance.State.PRE_END);
                        }
                    }
                }
            }
        } else {
            if (world.key().equals(Constants.Map.Lobby.LOBBY_WORLD_KEY)) {
                this.convertToLobbyPlayer(player);
            } else if (!player.hasPlayedBefore()) {
                Sponge.server().scheduler().submit(Task.builder()
                        .delay(Ticks.of(0))
                        .execute(() -> Sponge.server().worldManager().world(Constants.Map.Lobby.LOBBY_WORLD_KEY).ifPresent(w -> {
                            if (player.isOnline()) {
                                player.setLocation(ServerLocation.of(w, w.properties().spawnPosition()));
                                this.convertToLobbyPlayer(player);
                            }
                        }))
                        .plugin(Royale.instance.getPlugin())
                        .build()
                );
            }
        }
    }

    @Listener(order = Order.LAST)
    public void onMoveEntity(final MoveEntityEvent event, @First final ServerPlayer player) {
        final ServerWorld world = player.world();

        final Instance instance = Royale.instance.getInstanceManager().getInstance(world.key()).orElse(null);

        // We only care about inner-instance movement
        if (instance == null) {
            return;
        }

        // We only care about registered players
        if (!instance.isPlayerRegistered(player.uniqueId())) {
            return;
        }

        // If a Player has already spawned, this means they are playing. See if the instance allows movement
        if (instance.isPlayerSpawned(player.uniqueId()) && !instance.isPlayerDead(player.uniqueId()) && !instance.getState().canAnyoneMove()) {
            event.setCancelled(true);
        }
    }


    @Listener(order = Order.LAST)
    public void onChangeEntityWorld(final ChangeEntityWorldEvent event, @First final ServerPlayer player) {
        final ServerWorld fromWorld = event.originalWorld();
        final ServerWorld toWorld = event.destinationWorld();

        final Instance fromInstance = Royale.instance.getInstanceManager().getInstance(fromWorld.key()).orElse(null);
        final Instance toInstance = Royale.instance.getInstanceManager().getInstance(toWorld.key()).orElse(null);

        // We don't care about non-instances from and to.
        if (fromInstance == null && toInstance == null && !toWorld.key().equals(Constants.Map.Lobby.LOBBY_WORLD_KEY)) {
            return;
        }

        if (fromInstance != null) {

            player.setScoreboard(Sponge.server().serverScoreboard().get());

            // Switching out of instance means we kill them in the instance they left
            if (fromInstance.isPlayerRegistered(player.uniqueId())) {
                fromInstance.disqualifyPlayer(player);
                if (fromInstance.isRoundOver()) {
                    fromInstance.advanceTo(Instance.State.PRE_END);
                }

                // Set them back to the default scoreboard
                this.convertToLobbyPlayer(player);
            } else {
                // Switching into an instance
                if (toInstance != null && toInstance.isPlayerRegistered(player.uniqueId())) {
                    // Already dead here? Adjust them as a spectator
                    if (toInstance.isPlayerDead(player.uniqueId())) {
                        toInstance.spectate(player);

                        player.setScoreboard(toInstance.getScoreboard().getHandle());
                    }

                } else {

                    // Going from a non-instance world to lobby
                    this.convertToLobbyPlayer(player);
                }
            }
        }
    }

    @Listener(order = Order.LAST)
    public void onDestructEntity(final DestructEntityEvent.Death event, @Getter("entity") final ServerPlayer player) {
        final ServerWorld world = player.world();
        final Instance instance = Royale.instance.getInstanceManager().getInstance(world.key()).orElse(null);

        if (instance != null) {
            if (instance.isPlayerRegistered(player.uniqueId())) {
                instance.disqualifyPlayer(player);
                if (instance.isRoundOver()) {
                    instance.advanceTo(Instance.State.PRE_END);
                }
            }
        }
    }

    @Listener(order = Order.LAST)
    public void onRespawnPlayerSelectWorld(final RespawnPlayerEvent.SelectWorld event, @Getter("entity") final ServerPlayer player,
                                           @Getter("originalWorld") final ServerWorld fromWorld, @Getter("destinationWorld") final ServerWorld toWorld) {

        final Instance fromInstance = Royale.instance.getInstanceManager().getInstance(fromWorld.key()).orElse(null);
        Instance toInstance = Royale.instance.getInstanceManager().getInstance(toWorld.key()).orElse(null);

        if (fromInstance != null && toInstance == null && fromInstance.isPlayerRegistered(player.uniqueId()) && fromWorld.isLoaded()) {
            event.setDestinationWorld(fromWorld);
        }
    }

    @Listener(order = Order.LAST)
    public void onRespawnPlayer(final RespawnPlayerEvent.Recreate event, @Getter("recreatedPlayer") final ServerPlayer player,
                                @Getter("originalWorld") final ServerWorld fromWorld, @Getter("destinationWorld") final ServerWorld toWorld) {

        final Instance fromInstance = Royale.instance.getInstanceManager().getInstance(fromWorld.key()).orElse(null);
        Instance toInstance = Royale.instance.getInstanceManager().getInstance(toWorld.key()).orElse(null);

        if (fromInstance != null && toInstance == null && fromInstance.isPlayerRegistered(player.uniqueId()) && fromWorld.isLoaded()) {
            event.setDestinationPosition(event.originalPosition());
        }

        toInstance = Royale.instance.getInstanceManager().getInstance(toWorld.key()).orElse(null);

        if (fromInstance != null) {
            if (fromInstance.equals(toInstance)) {
                fromInstance.spectate(player);
            } else if (toInstance != null) {
                player.setScoreboard(toInstance.getScoreboard().getHandle());
            } else {
                player.setScoreboard(Sponge.server().serverScoreboard().orElse(null));
            }
        }
    }

    @Listener(order = Order.LAST)
    public void onAttackPlayer(final AttackEntityEvent event, @Getter("entity") final ServerPlayer player) {
        final Instance instance = Royale.instance.getInstanceManager().getInstance(player.world().key()).orElse(null);

        if (instance != null && !instance.isPlayerRegistered(player.uniqueId())) {
            event.setCancelled(true);
        }
    }

    @Listener
    public void onDamagePlayer(final DamageEntityEvent event, @Root final DamageSource source, @Getter("entity") final ServerPlayer player) {
        if (!(source.type().equals(DamageTypes.FALL.get()) || source.type().equals(DamageTypes.VOID.get())) && player.world().key()
                .equals(Constants.Map.Lobby.LOBBY_WORLD_KEY)) {
            event.setCancelled(true);
        }
    }

    @Listener
    public void onChangeSign(final ChangeSignEvent event, @Root final ServerPlayer player) {
        if (this.isTeleportSign(event.text().get())) {
            if (player.hasPermission(Constants.Permissions.ADMIN + ".create.sign")) {
                player.sendMessage(Identity.nil(), Component.text("Successfully created world teleportation sign!", NamedTextColor.GREEN));
                event.text().set(0, event.text().get(0).colorIfAbsent(NamedTextColor.AQUA));
            } else {
                player.sendMessage(Identity.nil(), Component.text("You do not have permission to create a world teleportation sign!", NamedTextColor.RED));
                event.setCancelled(true);
            }
        }
    }

    @Listener
    //TODO this is wrong. It needs a broader event
    public void onInteractByPlayer(final InteractBlockEvent.Secondary event, @Root final ServerPlayer player) {
        final ServerWorld world = player.world();
        final Instance instance = Royale.instance.getInstanceManager().getInstance(world.key()).orElse(null);

        if (instance != null && !instance.getState().canAnyoneInteract() && instance.isPlayerRegistered(player.uniqueId())) {
            event.setCancelled(true);
        }

        if (event instanceof InteractBlockEvent.Secondary) {
            final BlockSnapshot block = ((InteractBlockEvent.Secondary) event).block();

            block.location().flatMap(Location::blockEntity).flatMap(t -> t.get(Keys.SIGN_LINES)).ifPresent(lines -> {
                if (this.isTeleportSign(lines)) {
                    final String namespace = PlainComponentSerializer.plain().serialize(lines.get(0));
                    final String value = PlainComponentSerializer.plain().serialize(lines.get(1));

                    if (namespace.isEmpty() || value.isEmpty()) {
                        final Collection<Instance> instances = Royale.instance.getInstanceManager().getAll();
                        if (instances.size() != 1) {
                            player.sendMessage(Identity.nil(),
                                    Component.text(String.format("Unable to automatically join select - there are %s to choose from.", instances.size()),
                                            NamedTextColor.RED));
                            return;

                        }
                        final Instance realInstance = instances.stream().findAny().get();
                        if (realInstance != null) {
                            if (realInstance.registerPlayer(player)) {
                                realInstance.spawnPlayer(player);
                            }
                        }
                    } else {
                        final ResourceKey key = ResourceKey.of(namespace, value);
                        final Optional<Instance> optInstance = Royale.instance.getInstanceManager().getInstance(key);
                        if (optInstance.isPresent()) {
                            if (!optInstance.get().canRegisterMorePlayers()) {
                                player.sendMessage(Identity.nil(), Component.text("World is full!", NamedTextColor.RED));
                                return;
                            }
                            player.sendMessage(Identity.nil(), Component.text(String.format("Joining world '%s'", key), NamedTextColor.GREEN));
                            if (optInstance.get().registerPlayer(player)) {
                                optInstance.get().spawnPlayer(player);
                            }
                        } else {
                            player.sendMessage(Identity.nil(), Component.text(String.format("World '%s' isn't up yet!", key), NamedTextColor.RED));
                        }
                    }
                }
            });
        }
    }

    @Listener
    public void onChangeBlock(final ChangeBlockEvent.All event, @First final ServerPlayer player) {
        for (final Transaction<BlockSnapshot> transaction : event.transactions()) {
            final BlockSnapshot snapshot = transaction.finalReplacement();
            if (!player.hasPermission(Constants.Permissions.ADMIN + ".lobby.edit") && snapshot.world()
                    .equals(Constants.Map.Lobby.LOBBY_WORLD_KEY)) {
                transaction.setValid(false);
            }
        }
    }

    private void convertToLobbyPlayer(final ServerPlayer player) {
        player.setScoreboard(Sponge.server().serverScoreboard().orElse(null));
        Utils.resetPlayer(player);
    }

    private boolean isTeleportSign(final List<Component> lines) {
        return lines.size() != 0 && PlainComponentSerializer.plain().serialize(lines.get(0)).equalsIgnoreCase(Constants.Map.Lobby.SIGN_HEADER);
    }
}
