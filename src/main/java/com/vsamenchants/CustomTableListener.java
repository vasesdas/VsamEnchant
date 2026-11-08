package com.vsamenchants;

import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.TileState;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.util.Map;

/**
 * Слушатель событий размещения, взаимодействия и работы меню кастомного стола зачарований.
 */
public class CustomTableListener implements Listener {

    private final VsamEnchants plugin;
    private final CustomTableManager manager;

    public CustomTableListener(VsamEnchants plugin) {
        this.plugin = plugin;
        this.manager = plugin.getCustomTableManager();
    }

    /**
     * Установка кастомного стола: помечаем TileState блока в мире.
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        ItemStack item = event.getItemInHand();
        if (!manager.isTableItem(item)) return;

        Block block = event.getBlockPlaced();
        if (block.getState() instanceof TileState tileState) {
            tileState.getPersistentDataContainer().set(manager.getTableItemKey(), PersistentDataType.BYTE, (byte) 1);
            tileState.update();
        }
    }

    /**
     * Разрушение стола: сохраняем кастомный предмет с NBT и лором.
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        if (block.getType() != Material.ENCHANTING_TABLE) return;

        if (block.getState() instanceof TileState tileState) {
            if (tileState.getPersistentDataContainer().has(manager.getTableItemKey(), PersistentDataType.BYTE)) {
                event.setDropItems(false);
                if (event.getPlayer().getGameMode() != GameMode.CREATIVE) {
                    block.getWorld().dropItemNaturally(block.getLocation(), manager.createTableItem(1));
                }
            }
        }
    }

    /**
     * Открытие меню по ПКМ по блоку кастомного стола.
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (event.getHand() != EquipmentSlot.HAND) return;

        Block block = event.getClickedBlock();
        if (block == null || block.getType() != Material.ENCHANTING_TABLE) return;

        if (block.getState() instanceof TileState tileState) {
            if (tileState.getPersistentDataContainer().has(manager.getTableItemKey(), PersistentDataType.BYTE)) {
                event.setCancelled(true); // Блокируем ванильный интерфейс стола зачарований
                CustomTableGui.open(event.getPlayer(), plugin);
            }
        }
    }

    /**
     * Обработка действий внутри интерфейса Алтаря Зачарований.
     */
    @EventHandler
    public void onTableGuiClick(InventoryClickEvent event) {
        if (!event.getView().getTitle().equals(CustomTableGui.TITLE)) return;

        int slot = event.getRawSlot();
        Inventory top = event.getView().getTopInventory();

        // Клик в инвентаре игрока: обновляем кнопки при shift-click
        if (slot >= top.getSize()) {
            if (event.isShiftClick()) {
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    CustomTableGui.updateTierButtons(top, top.getItem(CustomTableGui.ITEM_SLOT), plugin);
                });
            }
            return;
        }

        // Разрешаем класть/забирать только в слоты предмета и лазурита
        if (slot != CustomTableGui.ITEM_SLOT && slot != CustomTableGui.LAPIS_SLOT) {
            event.setCancelled(true);
        }

        if (!(event.getWhoClicked() instanceof Player player)) return;

        if (slot == CustomTableGui.TIER_1_SLOT) {
            CustomTableGui.processEnchant(player, top, 1, plugin);
        } else if (slot == CustomTableGui.TIER_2_SLOT) {
            CustomTableGui.processEnchant(player, top, 2, plugin);
        } else if (slot == CustomTableGui.TIER_3_SLOT) {
            CustomTableGui.processEnchant(player, top, 3, plugin);
        }

        plugin.getServer().getScheduler().runTask(plugin, () -> {
            CustomTableGui.updateTierButtons(top, top.getItem(CustomTableGui.ITEM_SLOT), plugin);
        });
    }

    /**
     * Безопасный возврат предметов игроку при закрытии меню алтаря.
     */
    @EventHandler
    public void onTableGuiClose(InventoryCloseEvent event) {
        if (!event.getView().getTitle().equals(CustomTableGui.TITLE)) return;

        Inventory inv = event.getInventory();
        Player player = (Player) event.getPlayer();

        returnItem(player, inv.getItem(CustomTableGui.ITEM_SLOT));
        returnItem(player, inv.getItem(CustomTableGui.LAPIS_SLOT));

        inv.setItem(CustomTableGui.ITEM_SLOT, null);
        inv.setItem(CustomTableGui.LAPIS_SLOT, null);
    }

    /**
     * Обработка кликов в меню настройки крафта стола.
     */
    @EventHandler
    public void onCraftEditorClick(InventoryClickEvent event) {
        if (!event.getView().getTitle().equals(TableCraftEditorGui.TITLE)) return;

        int slot = event.getRawSlot();
        Inventory top = event.getView().getTopInventory();
        Player player = (Player) event.getWhoClicked();

        boolean isCraftSlot = false;
        for (int craftSlot : TableCraftEditorGui.CRAFT_SLOTS) {
            if (slot == craftSlot) {
                isCraftSlot = true;
                break;
            }
        }

        // Разрешаем менять только слоты сетки 3х3 (если есть права)
        if (slot < top.getSize() && !isCraftSlot) {
            event.setCancelled(true);
            if (slot == TableCraftEditorGui.SAVE_SLOT && player.hasPermission("vsamenchants.admin")) {
                TableCraftEditorGui.saveRecipeFromGui(player, top, plugin);
            }
        }
    }

    /**
     * Безопасный возврат ингредиентов при выходе из редактора крафта.
     */
    @EventHandler
    public void onCraftEditorClose(InventoryCloseEvent event) {
        if (!event.getView().getTitle().equals(TableCraftEditorGui.TITLE)) return;
        // Не возвращаем предметы принудительно, так как они подтягиваются из конфига как шаблон
    }

    private void returnItem(Player player, ItemStack item) {
        if (item == null || item.getType().isAir()) return;
        Map<Integer, ItemStack> leftover = player.getInventory().addItem(item);
        for (ItemStack drop : leftover.values()) {
            player.getWorld().dropItemNaturally(player.getLocation(), drop);
        }
    }
}