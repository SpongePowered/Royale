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
package org.spongepowered.special;

import org.slf4j.Logger;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.config.ConfigDir;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.game.state.GameConstructionEvent;
import org.spongepowered.api.event.game.state.GamePreInitializationEvent;
import org.spongepowered.api.event.game.state.GameStartingServerEvent;
import org.spongepowered.api.plugin.Plugin;
import org.spongepowered.api.plugin.PluginContainer;
import org.spongepowered.special.map.MapManager;
import org.spongepowered.special.map.MapType;
import org.spongepowered.special.map.MapTypeRegistryModule;

import java.io.IOException;
import java.nio.file.Path;

import javax.inject.Inject;

@Plugin(
        id = Constants.Meta.ID,
        name = Constants.Meta.NAME,
        version = Constants.Meta.VERSION,
        authors = Constants.Meta.AUTHORS,
        url = Constants.Meta.URL,
        description = Constants.Meta.DESCRIPTION

)
public final class Special {

    public static Special instance;

    @Inject private Logger logger;
    @Inject private PluginContainer container;
    @Inject @ConfigDir(sharedRoot = false) private Path configPath;

    private final MapManager mapManager = new MapManager();

    @Listener
    public void onGameConstruction(GameConstructionEvent event) {
        instance = this;
    }

    @Listener
    public void onGamePreinitialization(GamePreInitializationEvent event) {
        Sponge.getRegistry().registerModule(MapTypeRegistryModule.getInstance());
        Sponge.getRegistry().registerBuilderSupplier(MapType.Builder.class, MapType.Builder::new);

        Sponge.getCommandManager().register(this.container, Commands.rootCommand, Constants.Meta.ID, Constants.Meta.ID.substring(0, 1));

        Sponge.getEventManager().registerListeners(this.container, this.mapManager);
    }

    @Listener
    public void onGameStartingServer(GameStartingServerEvent event) throws IOException {
        Sponge.getServer().loadWorld(Sponge.getServer().createWorldProperties(Constants.Map.Lobby.DEFAULT_LOBBY_NAME, Constants.Map.Lobby.lobbyArchetype));
    }

    public Logger getLogger() {
        return this.logger;
    }

    public Path getConfigPath() {
        return this.configPath;
    }

    public MapManager getMapManager() {
        return this.mapManager;
    }
}
