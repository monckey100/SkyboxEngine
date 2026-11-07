package de.vectorflare.skyboxengine.tintcolor.premade;

import de.vectorflare.skyboxengine.config.Settings;
import de.vectorflare.skyboxengine.tintcolor.TintProvider;
import org.bukkit.Color;
import org.bukkit.entity.Player;

public class CombinedTimeHeightProvider implements TintProvider {
    @Override
    public String getName() {
        return "combined_time_height";
    }

    @Override
    public Color getTintColor(Player player, Settings.SkyboxSettings skyboxSettings) {
        // The player's own clock (= world time unless setPlayerTime / a time-lock plugin
        // overrides it), so the skybox sun matches the sun this player actually sees.
        long time = Math.floorMod(player.getPlayerTime(), 24000L);
        int red = (int) (((time % 24000f) / 24000f) * 255f);


        double precision = 10;


        double playerY = player.getY();
        int center = (int) (255 * 255 * 0.5);
        int value = center + (int) (playerY * precision);

        value = Math.clamp(value,0,255 * 255);

        int blue = value % 255;
        int green = (value / 255) % 255;

        return Color.fromRGB(red,green,blue);

    }
}
