package com.vsamenchants;

import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.TileState;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Слушатель событий кастомного стола с абсолютной защитой от дюпа при поломке и взрывах.
 */
public class CustomTableListener implements Listener {

    private final VsamEnchants plugin;
    private final CustomTableManager manager;

    public CustomTableListener(VsamEnchants plugin) {
        this.plugin = plugin;
        this.manager = plugin.getCustomTableManager();
    }

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
     * ПРИОРИТЕТ LOWEST: Срабатывает САМЫМ ПЕРВЫМ до любых плагинов и слушателей.
     * Полностью отменяет событие, пресекая ванильный дроп и сторонние обработчики.
     */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        if (!manager.isCustomTableBlock(block)) return;

        // 1. Отменяем событие, запрещая ванильный дроп
        event.setCancelled(true);

        // 2. Превращаем блок в воздух
        block.setType(Material.AIR);

        // 3. Выпадаем строго ОДИН кастомный стол
        if (event.getPlayer().getGameMode() != GameMode.CREATIVE) {
            block.getWorld().dropItemNaturally(block.getLocation(), manager.createTableItem(1));
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent event) {
        handleExplosion(event.blockList());
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent event) {
        handleExplosion(event.blockList());
    }

    private void handleExplosion(List<Block> blocks) {
        Iterator<Block> iterator = blocks.iterator();
        while (iterator.hasNext()) {
            Block block = iterator.next();
            if (manager.isCustomTableBlock(block)) {
                iterator.remove();
                block.setType(Material.AIR);
                block.getWorld().dropItemNaturally(block.getLocation(), manager.createTableItem(1));
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (event.getHand() != EquipmentSlot.HAND) return;

        Block block = event.getClickedBlock();
        if (block == null || block.getType() != Material.ENCHANTING_TABLE) return;

        if (manager.isCustomTableBlock(block)) {
            event.setCancelled(true);
            CustomTableGui.open(event.getPlayer(), plugin);
        }
    }

    @EventHandler
    public void onTableGuiClick(InventoryClickEvent event) {
        if (!event.getView().getTitle().equals(CustomTableGui.TITLE)) return;

        int slot = event.getRawSlot();
        Inventory top = event.getView().getTopInventory();

        if (slot >= top.getSize()) {
            if (event.isShiftClick()) {
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    CustomTableGui.updateEnchantButton(top, top.getItem(CustomTableGui.ITEM_SLOT), plugin);
                });
            }
            return;
        }

        if (slot != CustomTableGui.ITEM_SLOT && slot != CustomTableGui.LAPIS_SLOT) {
            event.setCancelled(true);
        }

        if (!(event.getWhoClicked() instanceof Player player)) return;

        // Клик по центральной кнопке зачарования
        if (slot == CustomTableGui.ENCHANT_BUTTON_SLOT) {
            CustomTableGui.processEnchant(player, top, plugin);
        }

        plugin.getServer().getScheduler().runTask(plugin, () -> {
            CustomTableGui.updateEnchantButton(top, top.getItem(CustomTableGui.ITEM_SLOT), plugin);
        });
    }

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

        if (slot < top.getSize() && !isCraftSlot) {
            event.setCancelled(true);
            if (slot == TableCraftEditorGui.SAVE_SLOT && player.hasPermission("vsamenchants.admin")) {
                TableCraftEditorGui.saveRecipeFromGui(player, top, plugin);
            }
        }
    }

    private void returnItem(Player player, ItemStack item) {
        if (item == null || item.getType().isAir()) return;
        Map<Integer, ItemStack> leftover = player.getInventory().addItem(item);
        for (ItemStack drop : leftover.values()) {
            player.getWorld().dropItemNaturally(player.getLocation(), drop);
        }
    }
}