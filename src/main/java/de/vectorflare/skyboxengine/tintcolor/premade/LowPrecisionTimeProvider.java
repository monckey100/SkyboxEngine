package de.vectorflare.skyboxengine.tintcolor.premade;

import de.vectorflare.skyboxengine.config.Settings;
import de.vectorflare.skyboxengine.tintcolor.TintProvider;
import org.bukkit.Color;
import org.bukkit.entity.Player;

public class LowPrecisionTimeProvider implements TintProvider {


    @Override
    public String getName() {
        return "basic_daytime";
    }

    @Override
    public Color getTintColor(Player player, Settings.SkyboxSettings skyboxSettings) {
        // The player's own clock (= world time unless setPlayerTime / a time-lock plugin
        // overrides it), so the skybox sun matches the sun this player actually sees.
        long time = Math.floorMod(player.getPlayerTime(), 24000L);
        int compressed = (int) (((time % 24000f) / 24000f) * 255f);
        return Color.fromRGB(compressed, 0, 0);
    }
}
