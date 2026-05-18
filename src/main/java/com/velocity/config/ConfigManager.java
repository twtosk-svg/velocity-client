package com.velocity.config;

import com.velocity.core.LightDebugManager;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Saves / loads all mod settings to a single JSON file generically.
 *
 * Config path: .minecraft/config/velocity.json
 *
 * Scans Setting classes via Reflection to automatically serialize parameters.
 */
public class ConfigManager {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("velocity.json");

    private static final Class<?>[] SETTING_CLASSES = {
        EspSettings.class,
        AimAssistSettings.class,
        UtilitySettings.class,
        OreEspSettings.class,
        LightDebugManager.class,
        LightSourceEspSettings.class
    };

    public static void save() {
        JsonObject root = new JsonObject();
        for (Class<?> clazz : SETTING_CLASSES) {
            JsonObject classObj = new JsonObject();
            for (Field field : clazz.getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers())) {
                    try {
                        Object value = field.get(null);
                        if (value instanceof Boolean) classObj.addProperty(field.getName(), (Boolean) value);
                        else if (value instanceof Number) classObj.addProperty(field.getName(), (Number) value);
                        else if (value instanceof String) classObj.addProperty(field.getName(), (String) value);
                        else if (value instanceof float[]) {
                            classObj.add(field.getName(), GSON.toJsonTree(value));
                        }
                    } catch (Exception ignored) {}
                }
            }
            root.add(clazz.getSimpleName(), classObj);
        }
        
        try {
            Files.writeString(CONFIG_PATH, GSON.toJson(root));
        } catch (IOException e) {
            System.err.println("[Velocity] Failed to save config: " + e.getMessage());
        }
    }

    public static void load() {
        if (!Files.exists(CONFIG_PATH)) {
            return; // first launch — use defaults
        }

        try {
            String json = Files.readString(CONFIG_PATH);
            JsonObject root = GSON.fromJson(json, JsonObject.class);
            if (root == null) return;

            for (Class<?> clazz : SETTING_CLASSES) {
                if (root.has(clazz.getSimpleName())) {
                    JsonObject classObj = root.getAsJsonObject(clazz.getSimpleName());
                    for (Field field : clazz.getDeclaredFields()) {
                        if (Modifier.isStatic(field.getModifiers()) && classObj.has(field.getName())) {
                            try {
                                Class<?> type = field.getType();
                                if (type == boolean.class) field.setBoolean(null, classObj.get(field.getName()).getAsBoolean());
                                else if (type == int.class) field.setInt(null, classObj.get(field.getName()).getAsInt());
                                else if (type == float.class) field.setFloat(null, classObj.get(field.getName()).getAsFloat());
                                else if (type == double.class) field.setDouble(null, classObj.get(field.getName()).getAsDouble());
                                else if (type == String.class) field.set(null, classObj.get(field.getName()).getAsString());
                                else if (type == float[].class) {
                                    float[] arr = GSON.fromJson(classObj.get(field.getName()), float[].class);
                                    if (arr != null) field.set(null, arr);
                                }
                            } catch (Exception ignored) {}
                        }
                    }
                }
            }
            System.out.println("[Velocity] Config loaded from " + CONFIG_PATH);
        } catch (Exception e) {
            System.err.println("[Velocity] Failed to load config: " + e.getMessage());
        }
    }
}
