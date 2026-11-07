package de.vectorflare.skyboxengine.manager;

import de.vectorflare.skyboxengine.SkyboxEngine;
import de.vectorflare.skyboxengine.config.ConfigManager;
import de.vectorflare.skyboxengine.config.Settings;
import de.vectorflare.skyboxengine.skybox.ActiveSkybox;
import de.vectorflare.skyboxengine.skybox.PlayerSkybox;
import de.vectorflare.skyboxengine.skybox.SkyboxReason;
import org.bukkit.entity.Player;

import java.util.PriorityQueue;

public class PlayerSkyboxData {

    public final PriorityQueue<ActiveSkybox> playerSkyboxes;
    public Player player;
    public PlayerSkybox renderedPlayerSkybox;

    // While true the client is still loading into a world (join / world change / far teleport),
    // so no skybox may be rendered - packets sent during the switch land in the wrong world.
    private volatile boolean worldLoading;
    private volatile int loadEpoch;

    public PlayerSkyboxData(Player player) {
        this.player = player;
        this.playerSkyboxes = new PriorityQueue<>();
        this.worldLoading = true;
        if (SkyboxEngine.getConfigInstance().getDefaultSkybox() != null && !SkyboxEngine.getConfigInstance().getDefaultSkybox().isBlank()) {
            addDefaultSkybox();
        }
    }

    public boolean isWorldLoading() {
        return worldLoading;
    }

    public int getLoadEpoch() {
        return loadEpoch;
    }

    /**
     * Marks the player as mid world-load and despawns the rendered skybox.
     * Returns the new load epoch, used to invalidate stale fallback tasks
     * when teleports chain (e.g. lobby -> dungeon instance).
     */
    public int beginWorldLoading() {
        worldLoading = true;
        loadEpoch++;
        if (renderedPlayerSkybox != null) {
            renderedPlayerSkybox.removeSkybox();
            renderedPlayerSkybox = null;
        }
        return loadEpoch;
    }

    public void endWorldLoading() {
        worldLoading = false;
    }

    public void tick() {
        if (worldLoading) {
            return;
        }
        if (renderedPlayerSkybox != null) {
            renderedPlayerSkybox.tickSkybox();
        }

    }


    public void remove() {
        if (renderedPlayerSkybox != null) {
            renderedPlayerSkybox.removeSkybox();
        }

        playerSkyboxes.clear();
    }

    public void addActivePlayerSkybox(ActiveSkybox skybox) {
        playerSkyboxes.add(skybox);
        changeRenderedPlayerSkybox();
    }

    public void removeActiveSkybox(ActiveSkybox skybox) {
        playerSkyboxes.remove(skybox);
        changeRenderedPlayerSkybox();
    }

    public void removeSkybox(Settings.SkyboxSettings settings) {
        PriorityQueue<ActiveSkybox> duplicate = new PriorityQueue<>(playerSkyboxes);
        duplicate.forEach((skybox -> {
            if (skybox.skybox.equals(settings)) {
                playerSkyboxes.remove(skybox);
            }
        }));
        changeRenderedPlayerSkybox();
    }

    public void clearActiveSkyboxes() {
        playerSkyboxes.clear();
        changeRenderedPlayerSkybox();
    }

    public void addDefaultSkybox() {
        for (ActiveSkybox activeSkybox : playerSkyboxes) {
            if (activeSkybox.reason == SkyboxReason.DEFAULT) {
                removeActiveSkybox(activeSkybox);
                break;
            }
        }
        if (ConfigManager.getDefaultSkybox() == null) {
            return;
        }
        addActivePlayerSkybox(new ActiveSkybox(ConfigManager.getDefaultSkybox(), SkyboxReason.DEFAULT,10000));

    }

    public void changeRenderedPlayerSkybox() {
        changeRenderedPlayerSkybox(false);
    }

    public void changeRenderedPlayerSkybox(boolean force) {
        // Never spawn while the client is still loading in - the skybox would
        // render in the world the player came from. WorldSkyboxListener re-renders
        // once the destination is confirmed loaded.
        if (worldLoading) {
            if (renderedPlayerSkybox != null) {
                renderedPlayerSkybox.removeSkybox();
                renderedPlayerSkybox = null;
            }
            return;
        }
        ActiveSkybox skybox = playerSkyboxes.peek();


        if (skybox == null) {
            // Remove Rendered Skybox if none is active now
            if ( renderedPlayerSkybox != null) {
                renderedPlayerSkybox.removeSkybox(0);
                renderedPlayerSkybox = null;
            }
            return;
        }
        // nothing to do rendered Skybox and new Skybox and new skybox have the same settings
        if (!force && renderedPlayerSkybox != null && skybox.skybox == renderedPlayerSkybox.getSettings()) {
            return;
        }
        // Clear Old Skybox if it exists
        if (renderedPlayerSkybox != null) {
            renderedPlayerSkybox.removeSkybox(1);
        }
        // Check if player has skyboxes disabled
        if (SkyboxEngine.getData().disabledSkyboxes.contains(player.getUniqueId())) {
            return;
        }
        // render the new skybox
        renderedPlayerSkybox = new PlayerSkybox(player,skybox.skybox);
        renderedPlayerSkybox.createSkybox();
    }

}
