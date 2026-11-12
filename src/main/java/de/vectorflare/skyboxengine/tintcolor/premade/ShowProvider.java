package de.vectorflare.skyboxengine.tintcolor.premade;

import de.vectorflare.skyboxengine.config.Settings;
import de.vectorflare.skyboxengine.tintcolor.TintProvider;
import org.bukkit.Color;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * "show": the player's clock in red + green (same packing as "daytime") and a per-player show value 0..255 in blue,
 * set by other plugins through {@code SkyboxAPI.setShowValue}. A conducted skybox (DepartedMobs weather: aurora,
 * starfall, eclipse) reads blue as its intensity / finale progress, so the server times the whole show.
 */
public class ShowProvider implements TintProvider {

    private static final Map<UUID, Integer> VALUES = new ConcurrentHashMap<>();

    public static void set(Player player, int value) {
        VALUES.put(player.getUniqueId(), Math.max(0, Math.min(255, value)));
    }

    public static void clear(UUID player) {
        VALUES.remove(player);
    }

    @Override
    public String getName() {
        return "show";
    }

    @Override
    public Color getTintColor(Player player, Settings.SkyboxSettings skyboxSettings) {
        int compressed = (int) Math.floorMod(player.getPlayerTime(), 24000L);
        return Color.fromRGB(compressed / 255, compressed % 255, VALUES.getOrDefault(player.getUniqueId(), 0));
    }
}
