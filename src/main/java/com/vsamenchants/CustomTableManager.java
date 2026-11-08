package com.vsamenchants;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

/**
 * Менеджер кастомного чародейского стола и его динамического крафта.
 */
public class CustomTableManager {

    private final VsamEnchants plugin;
    private final NamespacedKey tableItemKey;
    private final NamespacedKey recipeKey;

    public CustomTableManager(VsamEnchants plugin) {
        this.plugin = plugin;
        this.tableItemKey = new NamespacedKey(plugin, "custom_enchant_table");
        this.recipeKey = new NamespacedKey(plugin, "custom_table_recipe");
        registerRecipe();
    }

    public NamespacedKey getTableItemKey() {
        return tableItemKey;
    }

    /**
     * Создает предмет кастомного чародейского стола.
     */
    public ItemStack createTableItem(int amount) {
        ItemStack item = new ItemStack(Material.ENCHANTING_TABLE, amount);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        String name = plugin.getConfig().getString("custom-table.item.name", "&5&lАлтарь Зачарований");
        meta.setDisplayName(color(name));

        List<String> rawLore = plugin.getConfig().getStringList("custom-table.item.lore");
        List<String> lore = new ArrayList<>();
        for (String line : rawLore) {
            lore.add(color(line));
        }
        meta.setLore(lore);

        // NBT/PDC метка стола
        meta.getPersistentDataContainer().set(tableItemKey, PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);
        return item;
    }

    public boolean isTableItem(ItemStack item) {
        if (item == null || item.getType() != Material.ENCHANTING_TABLE || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        return meta != null && meta.getPersistentDataContainer().has(tableItemKey, PersistentDataType.BYTE);
    }

    /**
     * Регистрирует или обновляет ShapedRecipe в Bukkit на основе config.yml.
     */
    public void registerRecipe() {
        Bukkit.removeRecipe(recipeKey);

        FileConfiguration config = plugin.getConfig();
        if (!config.getBoolean("custom-table.recipe.enabled", true)) return;

        ShapedRecipe recipe = new ShapedRecipe(recipeKey, createTableItem(1));
        List<String> shape = config.getStringList("custom-table.recipe.shape");
        if (shape.size() != 3) {
            plugin.getLogger().warning("Форма крафта custom-table.recipe.shape должна содержать ровно 3 строки!");
            return;
        }

        recipe.shape(shape.get(0), shape.get(1), shape.get(2));

        ConfigurationSection ingredients = config.getConfigurationSection("custom-table.recipe.ingredients");
        if (ingredients == null) return;

        for (String key : ingredients.getKeys(false)) {
            String matName = ingredients.getString(key);
            Material mat = Material.matchMaterial(matName != null ? matName : "");
            if (mat != null && !key.isEmpty()) {
                recipe.setIngredient(key.charAt(0), mat);
            }
        }

        Bukkit.addRecipe(recipe);
        plugin.getLogger().info("Рецепт Алтаря Зачарований успешно зарегистрирован!");
    }

    private String color(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }
}