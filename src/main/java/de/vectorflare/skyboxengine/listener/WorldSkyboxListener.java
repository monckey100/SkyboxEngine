package de.vectorflare.skyboxengine.listener;

import de.vectorflare.skyboxengine.SkyboxEngine;
import de.vectorflare.skyboxengine.config.ConfigManager;
import de.vectorflare.skyboxengine.manager.PlayerSkyboxData;
import de.vectorflare.skyboxengine.skybox.ActiveSkybox;
import de.vectorflare.skyboxengine.skybox.SkyboxReason;
import io.papermc.paper.event.packet.PlayerChunkLoadEvent;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

import java.util.List;

public class WorldSkyboxListener implements Listener {

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTeleport(PlayerTeleportEvent event) {
        if (!isLoadingTeleport(event.getPlayer(), event.getFrom(), event.getTo())) {
            return;
        }
        beginWorldLoad(event.getPlayer());
    }

    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent event) {
        // Update the active skybox queue right away (rendering stays suppressed
        // until the client has actually loaded into the destination world).
        checkWorldSkyboxChange(event.getPlayer(), event.getPlayer().getWorld());
        beginWorldLoad(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onJoin(PlayerJoinEvent event) {
        checkWorldSkyboxChange(event.getPlayer(), event.getPlayer().getWorld());
        beginWorldLoad(event.getPlayer());
    }

    @EventHandler
    public void onChunkSent(PlayerChunkLoadEvent event) {
        Player player = event.getPlayer();
        PlayerSkyboxData data = SkyboxEngine.getPlayerSkyboxManager().getSkyboxData(player);
        if (data == null || !data.isWorldLoading()) {
            return;
        }
        Location loc = player.getLocation();
        if (!event.getChunk().getWorld().equals(loc.getWorld())) {
            return;
        }
        // Only the chunk the player is standing in confirms the destination is loading client-side
        if (event.getChunk().getX() != loc.getBlockX() >> 4 || event.getChunk().getZ() != loc.getBlockZ() >> 4) {
            return;
        }
        if (Bukkit.isPrimaryThread()) {
            finishWorldLoad(player);
        } else {
            Bukkit.getScheduler().runTask(SkyboxEngine.getInstance(), () -> finishWorldLoad(player));
        }
    }

    /**
     * A teleport needs the load-gate when it crosses worlds or moves further
     * than the player's render distance (e.g. into a dungeon area of the same world).
     */
    private static boolean isLoadingTeleport(Player player, Location from, Location to) {
        if (to == null) {
            return false;
        }
        if (from.getWorld() != to.getWorld()) {
            return true;
        }
        double renderDistance = Math.min(player.getClientViewDistance(), player.getViewDistance()) * 16.0;
        return from.distanceSquared(to) > renderDistance * renderDistance;
    }

    private static void beginWorldLoad(Player player) {
        PlayerSkyboxData data = SkyboxEngine.getPlayerSkyboxManager().getSkyboxData(player);
        if (data == null) {
            return;
        }
        int epoch = data.beginWorldLoading();
        // Fallback in case the chunk-load confirmation never arrives
        // (e.g. chunk was already sent). A newer teleport bumps the epoch
        // and invalidates this task.
        Bukkit.getScheduler().runTaskLater(SkyboxEngine.getInstance(), () -> {
            if (player.isOnline() && data.getLoadEpoch() == epoch) {
                finishWorldLoad(player);
            }
        }, SkyboxEngine.getConfigInstance().getWorldLoadTimeoutTicks());
    }

    private static void finishWorldLoad(Player player) {
        PlayerSkyboxData data = SkyboxEngine.getPlayerSkyboxManager().getSkyboxData(player);
        if (data == null || !data.isWorldLoading()) {
            return;
        }
        data.endWorldLoading();
        // Re-evaluate against the world the player is in NOW - chained teleports
        // (lobby -> dungeon) may have moved them since the gate was opened.
        checkWorldSkyboxChange(player, player.getWorld());
        reconstructCustomSkyboxes(player);
    }

    public static void checkWorldChanges() {
        Bukkit.getOnlinePlayers().forEach(p -> {
            checkWorldSkyboxChange(p,p.getWorld());
        });
    }

    private static void reconstructCustomSkyboxes(Player player) {
        PlayerSkyboxData data = SkyboxEngine.getPlayerSkyboxManager().getSkyboxData(player);
        List<ActiveSkybox> skyboxes = data.playerSkyboxes.stream().toList();
        for (ActiveSkybox activeSkybox : skyboxes) {
            if (activeSkybox.reason == SkyboxReason.CUSTOM || activeSkybox.reason == SkyboxReason.PLUGIN) {
                data.removeActiveSkybox(activeSkybox);
                data.addActivePlayerSkybox(activeSkybox);
            }
        }
        data.changeRenderedPlayerSkybox(true);
    }


    private static void checkWorldSkyboxChange(Player player, World world) {
        PlayerSkyboxData data = SkyboxEngine.getPlayerSkyboxManager().getSkyboxData(player);
        List<ActiveSkybox> skyboxes = data.playerSkyboxes.stream().toList();

        for (ActiveSkybox activeSkybox : skyboxes) {
            if (activeSkybox.reason == SkyboxReason.DIMENSION) {
                data.removeActiveSkybox(activeSkybox);
                break;
            }
        }
        if (SkyboxEngine.getConfigInstance().getDimensionSkyboxes().containsKey(world.getKey().toString())) {
            String name = SkyboxEngine.getConfigInstance().getDimensionSkyboxes().get(world.getKey().toString());
            if (ConfigManager.getSkyboxSettings(name) == null) {
                return;
            }
            data.addActivePlayerSkybox(new ActiveSkybox(ConfigManager.getSkyboxSettings(name),SkyboxReason.DIMENSION,SkyboxEngine.getConfigInstance().getDimensionSkyboxPriority()));
        }
    }


}
