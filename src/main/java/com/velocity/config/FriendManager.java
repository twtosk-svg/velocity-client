package com.velocity.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

public class FriendManager {
    private static final Set<String> friends = new CopyOnWriteArraySet<>();
    private static final Path FRIENDS_FILE = Paths.get("velocity", "friends.txt");

    public static void init() {
        load();
    }

    public static boolean isFriend(String name) {
        if (name == null) return false;
        for (String f : friends) {
            if (f.equalsIgnoreCase(name)) return true;
        }
        return false;
    }

    public static void addFriend(String name) {
        if (name == null || name.trim().isEmpty()) return;
        friends.add(name.trim());
        save();
    }

    public static void removeFriend(String name) {
        if (name == null) return;
        friends.removeIf(f -> f.equalsIgnoreCase(name));
        save();
    }

    public static void toggleFriend(String name) {
        if (isFriend(name)) removeFriend(name);
        else addFriend(name);
    }

    public static Set<String> getFriends() {
        return friends;
    }

    private static void load() {
        friends.clear();
        if (Files.exists(FRIENDS_FILE)) {
            try {
                for (String line : Files.readAllLines(FRIENDS_FILE)) {
                    if (!line.trim().isEmpty()) friends.add(line.trim());
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private static void save() {
        try {
            Files.createDirectories(FRIENDS_FILE.getParent());
            Files.write(FRIENDS_FILE, friends);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
