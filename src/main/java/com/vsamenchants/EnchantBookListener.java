package com.vsamenchants;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

/**
 * Слушатель для применения книг зачарований на предметы в инвентаре.
 */
public class EnchantBookListener implements Listener {

    private final VsamEnchants plugin;
    private final EnchantManager manager;

    public EnchantBookListener(VsamEnchants plugin) {
        this.plugin = plugin;
        this.manager = plugin.getEnchantManager();
    }

    public ItemStack createEnchantBook(CustomEnchant enchant, int level) {
        ItemStack book = new ItemStack(Material.ENCHANTED_BOOK);
        ItemMeta meta = book.getItemMeta();
        if (meta == null) return book;

        meta.setDisplayName(color("&d&lКнига Зачарования: &r" + enchant.getName() + " " + toRoman(level)));

        List<String> lore = new ArrayList<>();
        lore.add("");

        List<String> description = enchant.getDescription();
        if (description != null && !description.isEmpty()) {
            for (String line : description) {
                lore.add(color(line));
            }
        }
        lore.add("");
        lore.add(color("&fШанс срабатывания: &a" + enchant.getChance(level) + "%"));

        if (enchant.getCooldown() > 0) {
            lore.add(color("&fПерезарядка: &c" + enchant.getCooldown() + " сек."));
        }

        lore.add(color("&fПрименяется к: &b" + enchant.getTarget().getDisplayName()));
        lore.add("");
        lore.add(color("&8(Перетащите эту книгу на нужный"));
        lore.add(color("&8предмет в инвентаре, чтобы применить)"));

        meta.setLore(lore);

        NamespacedKey enchantKey = new NamespacedKey(plugin, "stored_enchant");
        NamespacedKey levelKey = new NamespacedKey(plugin, "stored_level");
        meta.getPersistentDataContainer().set(enchantKey, PersistentDataType.STRING, enchant.getId());
        meta.getPersistentDataContainer().set(levelKey, PersistentDataType.INTEGER, level);

        book.setItemMeta(meta);
        return book;
    }

    private String toRoman(int number) {
        return switch (number) {
            case 1 -> "I"; case 2 -> "II"; case 3 -> "III"; case 4 -> "IV";
            case 5 -> "V"; case 6 -> "VI"; case 7 -> "VII"; case 8 -> "VIII";
            case 9 -> "IX"; case 10 -> "X";
            default -> String.valueOf(number);
        };
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        // Защита от дюпа: запрещаем кликать на слоты результатов (верстаки, наковальни, жители)
        if (event.getSlotType() == InventoryType.SlotType.RESULT) return;

        ItemStack cursor = event.getCursor();
        ItemStack current = event.getCurrentItem();

        if (cursor == null || current == null || cursor.getType() != Material.ENCHANTED_BOOK || current.getType() == Material.AIR) return;

        ItemMeta cursorMeta = cursor.getItemMeta();
        if (cursorMeta == null) return;

        PersistentDataContainer pdc = cursorMeta.getPersistentDataContainer();
        NamespacedKey enchantKey = new NamespacedKey(plugin, "stored_enchant");
        NamespacedKey levelKey = new NamespacedKey(plugin, "stored_level");

        if (!pdc.has(enchantKey, PersistentDataType.STRING)) return;

        event.setCancelled(true);

        String enchantId = pdc.get(enchantKey, PersistentDataType.STRING);
        int level = pdc.getOrDefault(levelKey, PersistentDataType.INTEGER, 1);

        CustomEnchant enchant = manager.getEnchant(enchantId);
        if (enchant == null || !enchant.getTarget().includes(current.getType())) {
            player.sendMessage(color("&cЭто зачарование нельзя применить на этот предмет!"));
            return;
        }

        int currentLevel = manager.getEnchantLevel(current, enchant);
        if (currentLevel >= level) {
            player.sendMessage(color("&cНа этом предмете уже есть это зачарование такого же или большего уровня!"));
            return;
        }

        // Проверка и списание стоимости через экономику Vault (если включено в config.yml)
        if (plugin.getConfig().getBoolean("economy.enabled", false) && plugin.getEconomyManager().isEnabled()) {
            double cost = plugin.getConfig().getDouble("economy.apply-cost", 0.0);
            if (cost > 0) {
                if (!plugin.getEconomyManager().hasMoney(player, cost)) {
                    player.sendMessage(color("&cНедостаточно средств для наложения зачарования! Требуется: &e$" + cost));
                    return;
                }
                plugin.getEconomyManager().takeMoney(player, cost);
                player.sendMessage(color("&7С вашего счета списано: &c-$" + cost));
            }
        }

        // Наносим зачарование
        manager.applyEnchant(current, enchant, level);

        // Снимаем ровно 1 книгу с курсора и обновляем инвентарь
        if (cursor.getAmount() > 1) {
            cursor.setAmount(cursor.getAmount() - 1);
            event.getView().setCursor(cursor);
        } else {
            event.getView().setCursor(null);
        }
        player.updateInventory();

        player.playSound(player.getLocation(), Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1.0f, 1.0f);
        player.sendMessage(color("&aУспешно применено зачарование &e" + enchant.getName() + " " + toRoman(level) + "&a!"));
    }

    private String color(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }
}