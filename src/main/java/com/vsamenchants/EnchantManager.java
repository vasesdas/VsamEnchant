package com.vsamenchants;

import org.bukkit.ChatColor;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Менеджер регистрации и работы с NBT (PDC).
 */
public class EnchantManager {

    private final VsamEnchants plugin;
    private final Map<String, CustomEnchant> registry = new HashMap<>();

    public EnchantManager(VsamEnchants plugin) {
        this.plugin = plugin;
    }

    /**
     * Регистрация нового зачарования.
     */
    public void registerEnchant(CustomEnchant enchant) {
        registry.put(enchant.getId(), enchant);
    }

    public CustomEnchant getEnchant(String id) {
        return registry.get(id.toLowerCase());
    }
    /**
     * Получает список ID всех зарегистрированных зачарований.
     */
    public java.util.Set<String> getRegisteredIds() {
        return registry.keySet();
    }

    private NamespacedKey getKey(String id) {
        return new NamespacedKey(plugin, id);
    }

    /**
     * Проверяет, есть ли на предмете зачарование.
     */
    public boolean hasEnchant(ItemStack item, CustomEnchant enchant) {
        if (item == null || !item.hasItemMeta() || enchant == null) return false;
        return item.getItemMeta().getPersistentDataContainer().has(getKey(enchant.getId()), PersistentDataType.INTEGER);
    }

    /**
     * Получает уровень зачарования на предмете.
     */
    public int getEnchantLevel(ItemStack item, CustomEnchant enchant) {
        if (!hasEnchant(item, enchant)) return 0;
        return item.getItemMeta().getPersistentDataContainer().getOrDefault(getKey(enchant.getId()), PersistentDataType.INTEGER, 0);
    }

    /**
     * Наносит зачарование на предмет.
     */
    public void applyEnchant(ItemStack item, CustomEnchant enchant, int level) {
        if (item == null || item.getType().isAir() || enchant == null) return;
        if (!enchant.getTarget().includes(item.getType())) return;

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(getKey(enchant.getId()), PersistentDataType.INTEGER, level);

        // Обновление лора (визуал зачарования)
        String displayName = ChatColor.translateAlternateColorCodes('&', enchant.getName() + " " + toRoman(level));
        String rawName = ChatColor.stripColor(ChatColor.translateAlternateColorCodes('&', enchant.getName()));

        List<String> lore = meta.hasLore() ? meta.getLore() : new ArrayList<>();
        lore.removeIf(line -> ChatColor.stripColor(line).startsWith(rawName));
        lore.add(0, displayName); // Добавляем на самый верх

        meta.setLore(lore);
        item.setItemMeta(meta);
    }

    /**
     * Перевод числа в римское.
     */
    private String toRoman(int number) {
        return switch (number) {
            case 1 -> "I";
            case 2 -> "II";
            case 3 -> "III";
            case 4 -> "IV";
            case 5 -> "V";
            case 6 -> "VI";
            case 7 -> "VII";
            case 8 -> "VIII";
            case 9 -> "IX";
            case 10 -> "X";
            default -> String.valueOf(number);
        };
    }
}