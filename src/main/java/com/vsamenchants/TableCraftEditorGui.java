package com.vsamenchants;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.*;

/**
 * Меню настройки и просмотра кастомного крафта стола прямо в игре.
 */
public class TableCraftEditorGui {

    public static final String TITLE = ChatColor.translateAlternateColorCodes('&', "&8Настройка крафта Алтаря");
    public static final int[] CRAFT_SLOTS = {11, 12, 13, 20, 21, 22, 29, 30, 31};
    public static final int RESULT_SLOT = 24;
    public static final int SAVE_SLOT = 49;

    public static void open(Player player, VsamEnchants plugin, boolean isEditor) {
        Inventory inv = Bukkit.createInventory(null, 54, TITLE);

        ItemStack bg = CustomTableGui.createGuiItem(Material.BLACK_STAINED_GLASS_PANE, " ", null);
        for (int i = 0; i < 54; i++) {
            inv.setItem(i, bg);
        }

        // Очищаем слоты сетки 3х3 под крафт
        for (int slot : CRAFT_SLOTS) {
            inv.setItem(slot, null);
        }

        // Заполняем текущими ингредиентами из конфига
        fillCurrentRecipe(inv, plugin);

        // Результат крафта
        inv.setItem(RESULT_SLOT, plugin.getCustomTableManager().createTableItem(1));

        if (isEditor) {
            inv.setItem(SAVE_SLOT, CustomTableGui.createGuiItem(Material.EMERALD_BLOCK, "&a&lСохранить рецепт", Arrays.asList(
                    "&7Выложите ингредиенты в сетку 3x3 слева",
                    "&7и нажмите сюда для сохранения в config.yml"
            )));
        } else {
            inv.setItem(SAVE_SLOT, CustomTableGui.createGuiItem(Material.BARRIER, "&c&lРежим просмотра", Arrays.asList(
                    "&7Этот рецепт используется на сервере."
            )));
        }

        player.openInventory(inv);
    }

    private static void fillCurrentRecipe(Inventory inv, VsamEnchants plugin) {
        List<String> shape = plugin.getConfig().getStringList("custom-table.recipe.shape");
        if (shape.size() != 3) return;

        Map<Character, Material> map = new HashMap<>();
        var sec = plugin.getConfig().getConfigurationSection("custom-table.recipe.ingredients");
        if (sec != null) {
            for (String key : sec.getKeys(false)) {
                Material m = Material.matchMaterial(sec.getString(key, ""));
                if (m != null && !key.isEmpty()) {
                    map.put(key.charAt(0), m);
                }
            }
        }

        int index = 0;
        for (int row = 0; row < 3; row++) {
            String line = shape.get(row);
            for (int col = 0; col < 3; col++) {
                char ch = col < line.length() ? line.charAt(col) : ' ';
                Material mat = map.get(ch);
                if (mat != null && mat != Material.AIR) {
                    inv.setItem(CRAFT_SLOTS[index], new ItemStack(mat));
                }
                index++;
            }
        }
    }

    public static void saveRecipeFromGui(Player player, Inventory inv, VsamEnchants plugin) {
        List<String> shape = new ArrayList<>();
        Map<Character, String> ingredients = new HashMap<>();

        char currentLetter = 'A';
        int slotIndex = 0;

        for (int row = 0; row < 3; row++) {
            StringBuilder line = new StringBuilder();
            for (int col = 0; col < 3; col++) {
                ItemStack item = inv.getItem(CRAFT_SLOTS[slotIndex++]);
                if (item == null || item.getType().isAir()) {
                    line.append(" ");
                } else {
                    Material mat = item.getType();
                    char foundChar = ' ';
                    for (Map.Entry<Character, String> entry : ingredients.entrySet()) {
                        if (entry.getValue().equals(mat.name())) {
                            foundChar = entry.getKey();
                            break;
                        }
                    }
                    if (foundChar == ' ') {
                        foundChar = currentLetter++;
                        ingredients.put(foundChar, mat.name());
                    }
                    line.append(foundChar);
                }
            }
            shape.add(line.toString());
        }

        plugin.getConfig().set("custom-table.recipe.shape", shape);
        plugin.getConfig().set("custom-table.recipe.ingredients", null);
        for (Map.Entry<Character, String> entry : ingredients.entrySet()) {
            plugin.getConfig().set("custom-table.recipe.ingredients." + entry.getKey(), entry.getValue());
        }

        plugin.saveConfig();
        plugin.getCustomTableManager().registerRecipe();

        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&8[&4VE&8] &aКастомный рецепт успешно сохранен и применен!"));
        player.closeInventory();
    }
}