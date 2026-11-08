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
 * Слушатель событий Алтаря Зачарований: строгий контроль пустых книг и интерфейса.
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

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        if (!manager.isCustomTableBlock(block)) return;

        event.setCancelled(true);
        block.setType(Material.AIR);

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
        if (!(event.getWhoClicked() instanceof Player player)) return;

        CustomTableGui.AltarSession session = CustomTableGui.getSession(player);

        // Обработка клика в инвентаре игрока
        if (slot >= top.getSize()) {
            if (event.isShiftClick()) {
                ItemStack clicked = event.getCurrentItem();
                if (clicked != null) {
                    // Разрешаем shift-клик только для книг и лазурита
                    if (clicked.getType() != Material.BOOK && clicked.getType() != Material.LAPIS_LAZULI) {
                        event.setCancelled(true);
                        player.sendMessage(ChatColor.RED + "В алтарь можно помещать только обычные книги и лазурит!");
                    }
                }
            }
            return;
        }

        // Защита от помещения неподходящих предметов курсором
        ItemStack cursor = event.getCursor();
        if (slot == CustomTableGui.BOOK_SLOT) {
            if (cursor != null && !cursor.getType().isAir() && cursor.getType() != Material.BOOK) {
                event.setCancelled(true);
                player.sendMessage(ChatColor.RED + "В этот слот можно поместить только обычную пустую книгу!");
                return;
            }
        } else if (slot == CustomTableGui.LAPIS_SLOT) {
            if (cursor != null && !cursor.getType().isAir() && cursor.getType() != Material.LAPIS_LAZULI) {
                event.setCancelled(true);
                player.sendMessage(ChatColor.RED + "В этот слот можно поместить только лазурит!");
                return;
            }
        } else {
            // Любые другие слоты внутри меню защищены от перемещения
            event.setCancelled(true);
        }

        // --- ОБРАБОТКА ИНТЕРАКТИВНЫХ КНОПОК ---
        switch (slot) {
            // Выбор категорий
            case CustomTableGui.CATEGORY_COMBAT_SLOT -> {
                session.setCategory(CustomTableGui.EnchantCategory.COMBAT);
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.7f, 1.2f);
                CustomTableGui.renderControls(top, session, plugin);
            }
            case CustomTableGui.CATEGORY_DEFENSE_SLOT -> {
                session.setCategory(CustomTableGui.EnchantCategory.DEFENSE);
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.7f, 1.2f);
                CustomTableGui.renderControls(top, session, plugin);
            }
            case CustomTableGui.CATEGORY_MINING_SLOT -> {
                session.setCategory(CustomTableGui.EnchantCategory.MINING);
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.7f, 1.2f);
                CustomTableGui.renderControls(top, session, plugin);
            }

            // Регулировка ставки опыта
            case CustomTableGui.BET_MINUS_5_SLOT -> {
                session.addBet(-5);
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.6f, 0.8f);
                CustomTableGui.renderControls(top, session, plugin);
            }
            case CustomTableGui.BET_MINUS_1_SLOT -> {
                session.addBet(-1);
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.6f, 0.9f);
                CustomTableGui.renderControls(top, session, plugin);
            }
            case CustomTableGui.BET_PLUS_1_SLOT -> {
                session.addBet(1);
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.6f, 1.1f);
                CustomTableGui.renderControls(top, session, plugin);
            }
            case CustomTableGui.BET_PLUS_5_SLOT -> {
                session.addBet(5);
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.6f, 1.3f);
                CustomTableGui.renderControls(top, session, plugin);
            }

            // Кнопка сотворения
            case CustomTableGui.ENCHANT_BUTTON_SLOT -> {
                CustomTableGui.processEnchant(player, top, plugin);
            }
        }
    }

    @EventHandler
    public void onTableGuiClose(InventoryCloseEvent event) {
        if (!event.getView().getTitle().equals(CustomTableGui.TITLE)) return;

        Inventory inv = event.getInventory();
        Player player = (Player) event.getPlayer();

        returnItem(player, inv.getItem(CustomTableGui.BOOK_SLOT));
        returnItem(player, inv.getItem(CustomTableGui.LAPIS_SLOT));

        inv.setItem(CustomTableGui.BOOK_SLOT, null);
        inv.setItem(CustomTableGui.LAPIS_SLOT, null);

        CustomTableGui.removeSession(player);
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