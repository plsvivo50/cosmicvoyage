package com.Ray1101.cosmicvoyage.space;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class SpaceState {

    // 记录“在太空中的玩家”
    private static final Set<UUID> IN_SPACE = new HashSet<>();

    public static void enterSpace(UUID uuid) {
        IN_SPACE.add(uuid);
    }

    public static void exitSpace(UUID uuid) {
        IN_SPACE.remove(uuid);
    }

    public static boolean isInSpace(UUID uuid) {
        return IN_SPACE.contains(uuid);
    }
}