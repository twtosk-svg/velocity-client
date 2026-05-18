package com.velocity.core;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.Vec3d;

import java.util.*;

/**
 * Robust Player Logout Coordinate and Equipment Tracker.
 *
 * Scans active world players and matches departures against the network player list.
 * If a player leaves the world and is also removed from the network player list, they logged out.
 * If they are still in the player list, they simply walked out of render distance.
 */
public class LogoutTracker {

    public static class LoggedPlayer {
        public final String name;
        public final UUID uuid;
        public final Vec3d pos;
        public final float health;
        public final float maxHealth;
        public final String dimension;
        public final long timestamp;
        
        // Equipment cached at the moment of logout
        public final ItemStack helmet;
        public final ItemStack chestplate;
        public final ItemStack leggings;
        public final ItemStack boots;
        public final ItemStack mainHand;
        public final ItemStack offHand;

        public LoggedPlayer(PlayerEntity player, String dimension) {
            this.name = player.getName().getString();
            this.uuid = player.getUuid();
            this.pos = new Vec3d(player.getX(), player.getY(), player.getZ());
            this.health = player.getHealth() + player.getAbsorptionAmount();
            this.maxHealth = player.getMaxHealth();
            this.dimension = dimension;
            this.timestamp = System.currentTimeMillis();

            this.helmet = player.getEquippedStack(EquipmentSlot.HEAD).copy();
            this.chestplate = player.getEquippedStack(EquipmentSlot.CHEST).copy();
            this.leggings = player.getEquippedStack(EquipmentSlot.LEGS).copy();
            this.boots = player.getEquippedStack(EquipmentSlot.FEET).copy();
            this.mainHand = player.getEquippedStack(EquipmentSlot.MAINHAND).copy();
            this.offHand = player.getEquippedStack(EquipmentSlot.OFFHAND).copy();
        }
    }

    private static final Map<UUID, LoggedPlayer> lastKnownState = new HashMap<>();
    private static final List<LoggedPlayer> loggedOutPlayers = new ArrayList<>();
    private static final Set<UUID> activePlayersLastTick = new HashSet<>();

    /**
     * Ticks every frame on the client thread to track players and catch logouts.
     */
    public static void tick(MinecraftClient client) {
        if (client.world == null || client.player == null) {
            activePlayersLastTick.clear();
            lastKnownState.clear();
            return;
        }

        Set<UUID> currentActive = new HashSet<>();
        String currentDim = client.world.getRegistryKey().getValue().toString();

        for (PlayerEntity player : client.world.getPlayers()) {
            if (player == client.player) continue;
            UUID uuid = player.getUuid();
            currentActive.add(uuid);

            // Continually update latest known state while they are in render distance
            lastKnownState.put(uuid, new LoggedPlayer(player, currentDim));
        }

        // Detect who departed this tick
        for (UUID prevUuid : activePlayersLastTick) {
            if (!currentActive.contains(prevUuid)) {
                // Player was removed from client world. Did they log out or just walk away?
                boolean stillInPlayerList = false;
                if (client.getNetworkHandler() != null) {
                    stillInPlayerList = client.getNetworkHandler().getPlayerListEntry(prevUuid) != null;
                }

                if (!stillInPlayerList) {
                    // Flawless detection: they are completely gone from the server player list.
                    LoggedPlayer logged = lastKnownState.get(prevUuid);
                    if (logged != null) {
                        // Prevent duplicates and add to the top of list
                        loggedOutPlayers.removeIf(p -> p.uuid.equals(prevUuid));
                        loggedOutPlayers.add(0, logged);
                    }
                }
            }
        }

        activePlayersLastTick.clear();
        activePlayersLastTick.addAll(currentActive);
    }

    public static List<LoggedPlayer> getLoggedOutPlayers() {
        return loggedOutPlayers;
    }

    public static void clear() {
        loggedOutPlayers.clear();
        lastKnownState.clear();
        activePlayersLastTick.clear();
    }
}
