package com.example.popa;

import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public final class PopaConfig {
    public static boolean enabled = true;
    public static boolean place = true;    // ставить кристаллы
    public static boolean explode = true;  // взрывать кристаллы
    public static int interval = 3;        // 1..20 тиков

    private PopaConfig() {}

    private static Path file() {
        return FabricLoader.getInstance().getConfigDir().resolve("popa.properties");
    }

    public static void load() {
        try {
            Path f = file();
            if (!Files.exists(f)) return;
            Properties p = new Properties();
            try (var in = Files.newInputStream(f)) { p.load(in); }
            enabled = Boolean.parseBoolean(p.getProperty("enabled", "true"));
            place = Boolean.parseBoolean(p.getProperty("place", "true"));
            explode = Boolean.parseBoolean(p.getProperty("explode", "true"));
            interval = Math.max(1, Math.min(20, Integer.parseInt(p.getProperty("interval", "3"))));
        } catch (Exception ignored) {
        }
    }

    public static void save() {
        try {
            Properties p = new Properties();
            p.setProperty("enabled", String.valueOf(enabled));
            p.setProperty("place", String.valueOf(place));
            p.setProperty("explode", String.valueOf(explode));
            p.setProperty("interval", String.valueOf(interval));
            try (var out = Files.newOutputStream(file())) { p.store(out, "Popa"); }
        } catch (Exception ignored) {
        }
    }
}
