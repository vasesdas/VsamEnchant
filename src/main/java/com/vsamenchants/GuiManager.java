package com.vsamenchants;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Менеджер графического интерфейса (GUI).
 */
public class GuiManager implements Listener {

    private final VsamEnchants plugin;
    private final String menuTitle;
    private final NamespacedKey guiKey;

    public GuiManager(VsamEnchants plugin) {
        this.plugin = plugin;
        this.menuTitle = color(plugin.getConfig().getString("settings.gui-title", "&8Кастомные Зачарования"));
        this.guiKey = new NamespacedKey(plugin, "gui_enchant_id");
    }

    /**
     * Открывает главное меню со списком зачарований.
     */
    public void openMainMenu(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, menuTitle);

        int slot = 0;
        for (String id : plugin.getEnchantManager().getRegisteredIds()) {
            if (slot >= 54) break;

            CustomEnchant enchant = plugin.getEnchantManager().getEnchant(id);
            if (enchant == null || !enchant.isEnabled()) continue;

            ItemStack item = new ItemStack(Material.ENCHANTED_BOOK);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(color(enchant.getName()));

                List<String> lore = new ArrayList<>();
                lore.add("");

                List<String> description = enchant.getDescription();
                if (description != null && !description.isEmpty()) {
                    for (String line : description) {
                        lore.add(color(line));
                    }
                }

                lore.add("");
                lore.add(color("&fМакс. уровень: &e" + enchant.getMaxLevel()));
                lore.add(color("&fБазовый шанс: &a" + enchant.getChance(1) + "% &8(растет от уровня)"));
                if (enchant.getCooldown() > 0) {
                    lore.add(color("&fПерезарядка: &c" + enchant.getCooldown() + " сек."));
                }
                lore.add(color("&fПрименяется к: &b" + enchant.getTarget().getDisplayName()));
                lore.add("");
                lore.add(color("&aЛКМ &8чтобы получить книгу 1-го уровня!"));

                // Сохраняем ID зачарования в PDC предмета для меню
                meta.getPersistentDataContainer().set(guiKey, PersistentDataType.STRING, enchant.getId());

                meta.setLore(lore);
                item.setItemMeta(meta);
            }
            inv.setItem(slot, item);
            slot++;
        }

        player.openInventory(inv);
    }

    /**
     * Обработка кликов в инвентаре меню.
     */
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!event.getView().getTitle().equals(menuTitle)) return;

        // Запрещаем любые перемещения предметов внутри меню
        event.setCancelled(true);

        // Проверяем, что клик совершен именно по верхнему инвентарю (меню)
        if (event.getClickedInventory() == null || !event.getClickedInventory().equals(event.getView().getTopInventory())) {
            return;
        }

        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null || !clickedItem.hasItemMeta()) return;
        if (!(event.getWhoClicked() instanceof Player player)) return;

        ItemMeta meta = clickedItem.getItemMeta();
        if (!meta.getPersistentDataContainer().has(guiKey, PersistentDataType.STRING)) return;

        String enchantId = meta.getPersistentDataContainer().get(guiKey, PersistentDataType.STRING);
        CustomEnchant enchant = plugin.getEnchantManager().getEnchant(enchantId);
        if (enchant == null) return;

        // Выдача книги 1 уровня
        ItemStack book = plugin.getEnchantBookListener().createEnchantBook(enchant, 1);
        Map<Integer, ItemStack> leftover = player.getInventory().addItem(book);

        for (ItemStack remain : leftover.values()) {
            player.getWorld().dropItemNaturally(player.getLocation(), remain);
        }

        player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 1.0f, 1.2f);
        player.sendMessage(color("&8[&4VE&8] &aВы получили книгу зачарования: &e" + enchant.getName() + " I&a!"));
    }

    private String color(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }
}