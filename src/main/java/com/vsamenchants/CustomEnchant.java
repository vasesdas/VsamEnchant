package com.vsamenchants;

import org.bukkit.configuration.file.FileConfiguration;

/**
 * Базовый класс для всех кастомных зачарований.
 */
public abstract class CustomEnchant {

    private final String id;
    private final EnchantTarget target;

    public CustomEnchant(String id, EnchantTarget target) {
        this.id = id.toLowerCase();
        this.target = target;
    }

    public String getId() {
        return id;
    }

    public EnchantTarget getTarget() {
        return target;
    }

    /**
     * Получает конфигурацию зачарований из главного класса.
     */
    protected FileConfiguration getConfig() {
        return VsamEnchants.getInstance().getEnchantsConfig().getConfig();
    }

    public boolean isEnabled() {
        return getConfig().getBoolean("enchants." + id + ".enabled", true);
    }

    public String getName() {
        return getConfig().getString("enchants." + id + ".name", id);
    }

    public int getMaxLevel() {
        return getConfig().getInt("enchants." + id + ".max-level", 1);
    }

    public double getChance(int level) {
        double baseChance = getConfig().getDouble("enchants." + id + ".chance", 10.0);
        // Можно умножать шанс на уровень, здесь возвращаем базовый
        return baseChance + (level * 2.0); // Пример скейлинга
    }

    public int getCooldown() {
        return getConfig().getInt("enchants." + id + ".cooldown", 0);
    }
    public java.util.List<String> getDescription() {
        return getConfig().getStringList("enchants." + id + ".description");
    }
}