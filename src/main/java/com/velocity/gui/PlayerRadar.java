package com.velocity.gui;

import com.velocity.config.EspSettings;
import com.velocity.config.FriendManager;
import imgui.ImGui;
import imgui.flag.ImGuiCond;
import imgui.flag.ImGuiWindowFlags;
import imgui.flag.ImGuiTableFlags;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class PlayerRadar {
    public static void draw(MinecraftClient client) {
        if (!EspSettings.playerRadarEnabled) return;
        if (client.world == null || client.player == null) return;

        List<PlayerEntity> players = new ArrayList<>();
        for (PlayerEntity player : client.world.getPlayers()) {
            if (player == client.player) continue;
            players.add(player);
        }

        if (players.isEmpty() && !OverlayManager.isMenuOpen()) {
            return;
        }

        // Sort by distance ascending
        players.sort(Comparator.comparingDouble(p -> p.distanceTo(client.player)));

        int flags = ImGuiWindowFlags.AlwaysAutoResize;
        if (!OverlayManager.isMenuOpen()) {
            flags |= ImGuiWindowFlags.NoInputs;
        }

        ImGui.setNextWindowPos(10, 200, ImGuiCond.FirstUseEver); // Below Admin Radar
        ImGui.setNextWindowBgAlpha(0.7f);
        
        ImGui.pushStyleVar(imgui.flag.ImGuiStyleVar.WindowRounding, 8.0f);
        
        if (ImGui.begin("Player Radar", flags)) {
            if (players.isEmpty()) {
                ImGui.textColored(0.5f, 0.5f, 0.5f, 1.0f, "No players nearby...");
            } else {
                int tableFlags = ImGuiTableFlags.BordersInner | ImGuiTableFlags.RowBg;
                if (ImGui.beginTable("player_table", 3, tableFlags)) {
                    ImGui.tableSetupColumn("Name");
                    ImGui.tableSetupColumn("Health");
                    ImGui.tableSetupColumn("Dist");
                    ImGui.tableHeadersRow();

                    for (PlayerEntity player : players) {
                        ImGui.tableNextRow();
                        
                        String name = player.getName().getString();
                        boolean isInvisible = player.isInvisible();
                        String displayName = isInvisible ? "[INV] " + name : name;
                        
                        float health = player.getHealth() + player.getAbsorptionAmount();
                        float maxHealth = player.getMaxHealth();
                        int dist = (int) player.distanceTo(client.player);
                        
                        boolean isFriend = EspSettings.friendsSystemEnabled && 
                                           EspSettings.friendEspOverride && 
                                           FriendManager.isFriend(name);
                                           
                        float r = 1.0f, g = 1.0f, b = 1.0f;
                        if (isInvisible) {
                            r = 0.7f;
                            g = 0.4f;
                            b = 1.0f; // Purple for invisible
                        } else if (isFriend) {
                            r = EspSettings.friendColor[0];
                            g = EspSettings.friendColor[1];
                            b = EspSettings.friendColor[2];
                        }

                        ImGui.tableSetColumnIndex(0);
                        ImGui.textColored(r, g, b, 1.0f, displayName);
                        
                        ImGui.tableSetColumnIndex(1);
                        float hpRatio = health / maxHealth;
                        if (hpRatio > 0.5f) {
                            ImGui.textColored(0.2f, 1.0f, 0.2f, 1.0f, String.format("%.1f", health));
                        } else if (hpRatio > 0.2f) {
                            ImGui.textColored(1.0f, 1.0f, 0.0f, 1.0f, String.format("%.1f", health));
                        } else {
                            ImGui.textColored(1.0f, 0.2f, 0.2f, 1.0f, String.format("%.1f", health));
                        }
                        
                        ImGui.tableSetColumnIndex(2);
                        ImGui.textColored(0.7f, 0.7f, 0.7f, 1.0f, dist + "m");
                    }
                    ImGui.endTable();
                }
            }
        }
        ImGui.end();
        ImGui.popStyleVar();
    }
}
