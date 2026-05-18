package com.velocity.gui;

import com.velocity.config.EspSettings;
import imgui.ImGui;
import imgui.flag.ImGuiCond;
import imgui.flag.ImGuiWindowFlags;
import imgui.flag.ImGuiTableFlags;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.world.GameMode;

import java.util.ArrayList;
import java.util.List;

public class AdminRadar {
    public static void draw(MinecraftClient client) {
        if (!EspSettings.adminOverlayEnabled) return;
        if (client.getNetworkHandler() == null) return;

        List<String> invisiblePlayers = new ArrayList<>();
        if (client.world != null) {
            for (net.minecraft.entity.player.PlayerEntity player : client.world.getPlayers()) {
                if (player != client.player && player.isInvisible()) {
                    invisiblePlayers.add(player.getName().getString());
                }
            }
        }

        List<PlayerListEntry> admins = new ArrayList<>();
        for (PlayerListEntry entry : client.getNetworkHandler().getPlayerList()) {
            GameMode mode = entry.getGameMode();
            String name = entry.getProfile().name();
            if (mode == GameMode.CREATIVE || mode == GameMode.SPECTATOR || invisiblePlayers.contains(name)) {
                admins.add(entry);
            }
        }

        // Hide window completely if no admins are present AND the menu is closed.
        // We keep it rendering if the menu is open so the user can drag it.
        if (admins.isEmpty() && !OverlayManager.isMenuOpen()) {
            return;
        }

        int flags = ImGuiWindowFlags.AlwaysAutoResize;
        if (!OverlayManager.isMenuOpen()) {
            flags |= ImGuiWindowFlags.NoInputs; // Make it click-through when menu is closed
        }

        ImGui.setNextWindowPos(10, 10, ImGuiCond.FirstUseEver);
        ImGui.setNextWindowBgAlpha(0.7f);
        
        // Push a slight style change for a modern feel
        ImGui.pushStyleVar(imgui.flag.ImGuiStyleVar.WindowRounding, 8.0f);
        
        if (ImGui.begin("Admin Radar", flags)) {
            if (admins.isEmpty()) {
                ImGui.textColored(0.5f, 0.5f, 0.5f, 1.0f, "Waiting for admins...");
            } else {
                int tableFlags = ImGuiTableFlags.BordersInner | ImGuiTableFlags.RowBg;
                if (ImGui.beginTable("admin_table", 3, tableFlags)) {
                    ImGui.tableSetupColumn("Name");
                    ImGui.tableSetupColumn("Mode");
                    ImGui.tableSetupColumn("Ping");
                    ImGui.tableHeadersRow();

                    for (PlayerListEntry admin : admins) {
                        ImGui.tableNextRow();
                        
                        String name = admin.getProfile().name();
                        String modeStr;
                        float r, g, b;

                        if (invisiblePlayers.contains(name)) {
                            modeStr = "Invisible";
                            r = 0.7f;
                            g = 0.4f;
                            b = 1.0f; // Purple
                        } else if (admin.getGameMode() == GameMode.CREATIVE) {
                            modeStr = "Creative";
                            r = 1.0f;
                            g = 0.6f;
                            b = 0.0f; // Orange
                        } else {
                            modeStr = "Spectator";
                            r = 1.0f;
                            g = 0.3f;
                            b = 0.3f; // Red/Spectator
                        }

                        int ping = admin.getLatency();

                        ImGui.tableSetColumnIndex(0);
                        ImGui.textColored(r, g, b, 1.0f, name);
                        
                        ImGui.tableSetColumnIndex(1);
                        ImGui.textColored(r, g, b, 1.0f, modeStr);
                        
                        ImGui.tableSetColumnIndex(2);
                        if (ping < 50) {
                            ImGui.textColored(0.2f, 1.0f, 0.2f, 1.0f, ping + "ms");
                        } else if (ping < 150) {
                            ImGui.textColored(1.0f, 1.0f, 0.0f, 1.0f, ping + "ms");
                        } else {
                            ImGui.textColored(1.0f, 0.2f, 0.2f, 1.0f, ping + "ms");
                        }
                    }
                    ImGui.endTable();
                }
            }
        }
        ImGui.end();
        ImGui.popStyleVar();
    }
}
