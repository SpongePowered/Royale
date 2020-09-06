package org.spongepowered.royale.instance.task;

import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import org.spongepowered.api.scheduler.ScheduledTask;
import org.spongepowered.api.world.server.ServerWorld;
import org.spongepowered.royale.instance.Instance;

import java.time.Duration;
import java.util.LinkedList;
import java.util.List;

public final class StartTask extends InstanceTask {

    private final List<Title> startTitles;

    private int seconds = 0;

    private ScheduledTask handle;

    public StartTask(final Instance instance) {
        super(instance);

        this.startTitles = new LinkedList<>();

        final Title.Times times = Title.Times.of(Duration.ZERO, Duration.ofMillis(600), Duration.ofMillis(400));

        for (long i = instance.getType().getRoundStartLength(); i > 0; i--) {

            this.startTitles.add(Title.of(TextComponent.of(i, NamedTextColor.DARK_RED), TextComponent.empty(), times));

            if (i == 3) {
                this.startTitles.add(Title.of(TextComponent.of("2", NamedTextColor.RED), TextComponent.empty(), times));
                this.startTitles.add(Title.of(TextComponent.of("1", NamedTextColor.GOLD), TextComponent.empty(), times));
                this.startTitles.add(Title.of(instance.getType().getRoundStartTemplate().apply().build(), TextComponent.empty(), times));
                break;
            }
        }
    }

    @Override
    public void accept(final ScheduledTask task) {
        this.handle = task;

        final ServerWorld world = this.getInstance().getWorld().orElse(null);

        // Make sure the world is still around and loaded
        if (world != null && world.isLoaded()) {

            // Make sure a player ref isn't still here
            world.getPlayers().forEach(player -> player.showTitle(this.startTitles.get(this.seconds)));

            this.seconds++;

            if (seconds >= startTitles.size()) {
                this.cancel();
                this.getInstance().advance();
            }
        }
    }

    @Override
    public void cancel() {
        this.handle.cancel();
    }
}
