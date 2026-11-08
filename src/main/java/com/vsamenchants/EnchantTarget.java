package com.vsamenchants;

import org.bukkit.Material;

/**
 * Типы предметов, на которые можно нанести зачарование.
 */
public enum EnchantTarget {
    ALL("Все предметы"),
    ARMOR("Броня"),
    SWORD("Мечи"),
    AXE("Топоры"),
    WEAPON("Мечи и Топоры"),
    BOW("Луки и Арбалеты"),
    PICKAXE("Кирки"),
    SHOVEL("Лопаты"),
    FISHING_ROD("Удочки"),
    TOOL("Кирки, Лопаты, Топоры и Мотыги"),
    HOE("Мотыги");

    private final String displayName;

    EnchantTarget(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    /**
     * Проверяет, подходит ли материал под эту цель.
     */
    public boolean includes(Material material) {
        if (material == null || material == Material.AIR) return false;
        String name = material.name();

        return switch (this) {
            case ALL -> true;
            case ARMOR -> name.endsWith("_HELMET") || name.endsWith("_CHESTPLATE") || name.endsWith("_LEGGINGS") || name.endsWith("_BOOTS");
            case SWORD -> name.endsWith("_SWORD");
            case AXE -> name.endsWith("_AXE");
            case WEAPON -> name.endsWith("_SWORD") || name.endsWith("_AXE");
            case BOW -> name.equals("BOW") || name.equals("CROSSBOW");
            case PICKAXE -> name.endsWith("_PICKAXE");
            case SHOVEL -> name.endsWith("_SHOVEL");
            case FISHING_ROD -> name.equals("FISHING_ROD");
            case TOOL -> name.endsWith("_PICKAXE") || name.endsWith("_SHOVEL") || name.endsWith("_AXE") || name.endsWith("_HOE");
            case HOE ->  name.endsWith("_HOE");
        };
    }
}