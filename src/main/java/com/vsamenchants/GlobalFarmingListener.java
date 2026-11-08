package com.vsamenchants;

import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.ExperienceOrb;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

/**
 * Обработчик зачарований Фермер (Мотыги) и Дровосек (Топоры).
 */
public class GlobalFarmingListener implements Listener {

    private final VsamEnchants plugin;
    private final EnchantManager manager;
    private final Set<String> processingLogs = new HashSet<>();

    public GlobalFarmingListener(VsamEnchants plugin) {
        this.plugin = plugin;
        this.manager = plugin.getEnchantManager();
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onCropInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (event.getHand() != EquipmentSlot.HAND) return;

        Player player = event.getPlayer();
        Block block = event.getClickedBlock();
        if (block == null) return;

        ItemStack tool = player.getInventory().getItemInMainHand();
        if (tool.getType().isAir() || !tool.getType().name().endsWith("_HOE")) return;

        int farmerLevel = manager.getEnchantLevel(tool, manager.getEnchant("farmer"));
        if (farmerLevel <= 0) return;

        if (block.getBlockData() instanceof Ageable crop) {
            if (crop.getAge() < crop.getMaximumAge()) {
                int newAge = Math.min(crop.getMaximumAge(), crop.getAge() + farmerLevel);
                crop.setAge(newAge);
                block.setBlockData(crop);

                block.getWorld().playSound(block.getLocation(), Sound.ITEM_CROP_PLANT, 1.0f, 1.0f);
                block.getWorld().spawnParticle(org.bukkit.Particle.VILLAGER_HAPPY, block.getLocation().add(0.5, 0.5, 0.5), 5);

                damageTool(player, tool);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onLogBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        if (player.getGameMode() == GameMode.CREATIVE) return;

        Block initialBlock = event.getBlock();
        ItemStack tool = player.getInventory().getItemInMainHand();

        if (tool.getType().isAir() || !tool.getType().name().endsWith("_AXE")) return;
        if (!isLog(initialBlock.getType())) return;

        int lumberjackLevel = manager.getEnchantLevel(tool, manager.getEnchant("lumberjack"));
        if (lumberjackLevel <= 0) return;

        String centerKey = getBlockKey(initialBlock);
        if (processingLogs.contains(centerKey)) return;

        handleDrops(initialBlock, tool, player, event.getExpToDrop());
        event.setDropItems(false);
        event.setExpToDrop(0);

        breakTreeBfs(player, initialBlock, tool);
    }

    private void breakTreeBfs(Player player, Block startBlock, ItemStack tool) {
        Queue<Block> queue = new ArrayDeque<>();
        Set<String> visited = new HashSet<>();

        queue.add(startBlock);
        visited.add(getBlockKey(startBlock));

        int maxLogs = 256;
        int brokenCount = 0;

        while (!queue.isEmpty() && brokenCount < maxLogs) {
            Block current = queue.poll();

            for (int x = -1; x <= 1; x++) {
                for (int y = -1; y <= 1; y++) {
                    for (int z = -1; z <= 1; z++) {
                        if (x == 0 && y == 0 && z == 0) continue;

                        Block neighbor = current.getRelative(x, y, z);
                        String key = getBlockKey(neighbor);

                        if (visited.contains(key)) continue;

                        if (isLog(neighbor.getType())) {
                            visited.add(key);
                            queue.add(neighbor);

                            processingLogs.add(key);
                            try {
                                BlockBreakEvent breakEvent = new BlockBreakEvent(neighbor, player);
                                plugin.getServer().getPluginManager().callEvent(breakEvent);

                                if (!breakEvent.isCancelled()) {
                                    handleDrops(neighbor, tool, player, breakEvent.getExpToDrop());
                                    neighbor.setType(Material.AIR);
                                    damageTool(player, tool);
                                    brokenCount++;
                                }
                            } finally {
                                processingLogs.remove(key);
                            }

                            if (tool.getType().isAir()) {
                                return;
                            }
                        }
                    }
                }
            }
        }
    }

    private void handleDrops(Block block, ItemStack tool, Player player, int exp) {
        Collection<ItemStack> drops = block.getDrops(tool, player);

        boolean autoSmelt = getLvl(tool, "autosmelt") > 0 && checkChance("autosmelt", getLvl(tool, "autosmelt"));
        // Только Телепатия
        boolean telepathy = getLvl(tool, "telepathy") > 0;

        if (exp > 0) {
            if (telepathy) {
                player.giveExp(exp);
            } else {
                ExperienceOrb orb = block.getWorld().spawn(block.getLocation().add(0.5, 0.5, 0.5), ExperienceOrb.class);
                orb.setExperience(exp);
            }
        }

        if (drops.isEmpty()) return;

        for (ItemStack drop : drops) {
            ItemStack finalItem = drop.clone();

            if (autoSmelt && isLog(finalItem.getType())) {
                finalItem = new ItemStack(Material.CHARCOAL, finalItem.getAmount());
            }

            if (telepathy) {
                Map<Integer, ItemStack> leftover = player.getInventory().addItem(finalItem);
                for (ItemStack remain : leftover.values()) {
                    block.getWorld().dropItemNaturally(player.getLocation(), remain);
                }
            } else {
                block.getWorld().dropItemNaturally(block.getLocation(), finalItem);
            }
        }
    }

    private boolean isLog(Material mat) {
        String name = mat.name();
        return name.endsWith("_LOG") || name.endsWith("_STEM") || name.endsWith("_WOOD") || name.endsWith("_HYPHAE");
    }

    private void damageTool(Player player, ItemStack tool) {
        if (player.getGameMode() == GameMode.CREATIVE) return;
        ItemMeta meta = tool.getItemMeta();
        if (meta instanceof Damageable damageable) {
            int unbreaking = tool.getEnchantmentLevel(Enchantment.DURABILITY);
            if (Math.random() * 100.0 < (100.0 / (unbreaking + 1))) {
                damageable.setDamage(damageable.getDamage() + 1);
                tool.setItemMeta(meta);

                if (damageable.getDamage() >= tool.getType().getMaxDurability()) {
                    tool.setAmount(0);
                    player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 1.0f, 1.0f);
                }
            }
        }
    }

    private String getBlockKey(Block block) {
        return block.getWorld().getName() + "_" + block.getX() + "_" + block.getY() + "_" + block.getZ();
    }

    private int getLvl(ItemStack item, String id) {
        CustomEnchant enc = manager.getEnchant(id);
        if (enc == null) return 0;
        return manager.getEnchantLevel(item, enc);
    }

    private boolean checkChance(String id, int level) {
        CustomEnchant enc = manager.getEnchant(id);
        if (enc == null) return false;
        return (Math.random() * 100.0) <= enc.getChance(level);
    }
}