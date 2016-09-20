package org.spongepowered.special.instance;

import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.entity.living.player.Player;

import java.util.ArrayList;

public class Utils {

    public static void resetHungerAndPotions(Player player) {
        player.offer(Keys.FOOD_LEVEL, player.getValue(Keys.FOOD_LEVEL).get().getMaxValue());
        player.offer(Keys.SATURATION, player.getValue(Keys.SATURATION).get().getMaxValue());
        player.offer(Keys.EXHAUSTION, player.getValue(Keys.EXHAUSTION).get().getMaxValue());
        player.offer(Keys.POTION_EFFECTS, new ArrayList<>());
    }

}
