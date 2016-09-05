package org.spongepowered.special.task;

import org.spongepowered.api.effect.particle.ParticleEffect;
import org.spongepowered.api.effect.particle.ParticleTypes;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.text.title.Title;
import org.spongepowered.api.world.World;
import org.spongepowered.special.map.MapType;

public final class EndCountdown {

    private final Title title;

    public EndCountdown(MapType mapType, World world, Player player) {
        final long seconds = mapType.getRoundEndLength();
        this.title = Title.builder()
                .stay((int) ((seconds - 1) * 20))
                .fadeIn(0)
                .fadeOut(20)
                .title(Text.of(TextColors.YELLOW, player.getDisplayNameData().displayName().get(), TextColors.WHITE, " is the winner!"))
                .build();

        player.spawnParticles(ParticleEffect.builder()
                        .type(ParticleTypes.FIREWORKS_SPARK)
                        .count(30)
                        .build(),
                player.getLocation().getPosition());

        world.getPlayers().stream().filter(User::isOnline).forEach(onlinePlayer -> {
            onlinePlayer.sendTitle(title);
        });
    }
}
