package org.abyssmc.antifastmath;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;

import java.util.HashMap;

public final class AntiFastMath extends JavaPlugin implements Listener {
    HashMap<Player, Double> vanillaPrecision = new HashMap<>();
    HashMap<Player, Double> fastMathPrecision = new HashMap<>();
    HashMap<Player, Integer> playerSamples = new HashMap<>();

    int multiplier;
    String kickMessage;

    public static Vector getVanillaMathMovement(Vector wantedMovement, float f, float f2) {
        float f3 = VanillaMath.sin(f2 * 0.017453292f);
        float f4 = VanillaMath.cos(f2 * 0.017453292f);

        float bestTheoreticalX = (float) (f3 * wantedMovement.getZ() + f4 * wantedMovement.getX()) / (f3 * f3 + f4 * f4) / f;
        float bestTheoreticalZ = (float) (-f3 * wantedMovement.getX() + f4 * wantedMovement.getZ()) / (f3 * f3 + f4 * f4) / f;

        return new Vector(bestTheoreticalX, 0, bestTheoreticalZ);
    }

    public static Vector getFastMathMovement(Vector wantedMovement, float f, float f2) {
        float f3 = OptifineMath.sin(f2 * 0.017453292f);
        float f4 = OptifineMath.cos(f2 * 0.017453292f);

        float bestTheoreticalX = (float) (f3 * wantedMovement.getZ() + f4 * wantedMovement.getX()) / (f3 * f3 + f4 * f4) / f;
        float bestTheoreticalZ = (float) (-f3 * wantedMovement.getX() + f4 * wantedMovement.getZ()) / (f3 * f3 + f4 * f4) / f;

        return new Vector(bestTheoreticalX, 0, bestTheoreticalZ);
    }

    @Override
    public void onEnable() {
        // Plugin startup logic
        Bukkit.getPluginManager().registerEvents(this, this);

        if (!getConfig().isSet("multiplierRequired")) {
            getConfig().set("multiplierRequired", 10000);
            getConfig().set("kickMessage", "Optifine fastmath is not allowed!");
            saveConfig();
        }

        multiplier = getConfig().getInt("multiplierRequired", 10000);
        kickMessage = getConfig().getString("kickMessage", "Optifine fastmath is not allowed!");

        for (Player player : Bukkit.getOnlinePlayers()) {
            vanillaPrecision.put(player, 0D);
            fastMathPrecision.put(player, 0D);
            playerSamples.put(player, 0);
        }
    }

    @EventHandler
    public void onPlayerJoinEvent(PlayerJoinEvent event) {
        vanillaPrecision.put(event.getPlayer(), 0D);
        fastMathPrecision.put(event.getPlayer(), 0D);
        playerSamples.put(event.getPlayer(), 0);
    }

    @EventHandler
    public void onPlayerQuitEvent(PlayerQuitEvent event) {
        vanillaPrecision.remove(event.getPlayer());
        fastMathPrecision.remove(event.getPlayer());
        playerSamples.remove(event.getPlayer());
    }

    @EventHandler
    public void onPlayerMoveEvent(PlayerMoveEvent event) {
        Vector movement = event.getTo().toVector().subtract(event.getFrom().toVector());
        Vector vanillaMovement = getVanillaMathMovement(movement, (float) (0.1), event.getFrom().getYaw());
        Vector fastMathMovement = getFastMathMovement(movement, (float) (0.1), event.getFrom().getYaw());

        double lowVanilla = Math.min(Math.abs(vanillaMovement.getX()), Math.abs(vanillaMovement.getZ()));
        double lowOptifine = Math.min(Math.abs(fastMathMovement.getX()), Math.abs(fastMathMovement.getZ()));

        double vanillaRunning = vanillaPrecision.get(event.getPlayer());
        double optifineRunning = fastMathPrecision.get(event.getPlayer());

        if (lowVanilla < 1e-5 || lowOptifine < 1e-5 && movement.length() > 0.1) {
            vanillaRunning = vanillaRunning * 15 / 16 + lowVanilla;
            optifineRunning = optifineRunning * 15 / 16 + lowOptifine;

            vanillaPrecision.put(event.getPlayer(), vanillaRunning);
            fastMathPrecision.put(event.getPlayer(), optifineRunning);

            int count = playerSamples.get(event.getPlayer());
            playerSamples.put(event.getPlayer(), count + 1);

            if (count > 16 && optifineRunning * multiplier < vanillaRunning) {
                event.getPlayer().kickPlayer(kickMessage);
            }
        }
    }
}
