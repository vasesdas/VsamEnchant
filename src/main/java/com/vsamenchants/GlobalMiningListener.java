package com.vsamenchants;

import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.ExperienceOrb;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Оптимизированный слушатель зачарований на добычу (Кирки, Лопаты, Топоры).
 */
public class GlobalMiningListener implements Listener {

    private final VsamEnchants plugin;
    private final EnchantManager manager;
    private final Set<String> processingBlocks = new HashSet<>();

    public GlobalMiningListener(VsamEnchants plugin) {
        this.plugin = plugin;
        this.manager = plugin.getEnchantManager();
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        if (player.getGameMode() == GameMode.CREATIVE) return;

        Block centerBlock = event.getBlock();

        // Защита от дюпа: не трогаем кастомный алтарь
        if (plugin.getCustomTableManager().isCustomTableBlock(centerBlock)) {
            return;
        }

        ItemStack tool = player.getInventory().getItemInMainHand();
        if (tool.getType().isAir()) return;

        String centerKey = getBlockKey(centerBlock);
        if (processingBlocks.contains(centerKey)) return;

        // --- 1. ПАССИВНЫЕ ЭФФЕКТЫ ---
        int hasteLevel = getLvl(tool, "haste");
        if (hasteLevel > 0) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.FAST_DIGGING, 60, hasteLevel - 1, true, false));
        }

        int nightVisionLevel = getLvl(tool, "nightminer");
        if (nightVisionLevel > 0) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, 240, 0, true, false));
        }

        // --- 2. ОБРАБОТКА ЦЕНТРАЛЬНОГО БЛОКА ---
        handleDrops(centerBlock, tool, player, event.getExpToDrop());
        event.setDropItems(false);
        event.setExpToDrop(0);

        // --- 3. МАССОВАЯ ДОБЫЧА (БУР 3x3) ---
        int drillLevel = getLvl(tool, "drill");
        if (drillLevel > 0 && checkChance("drill", drillLevel)) {
            breakDrillArea(player, centerBlock, tool);
        }
    }

    private void breakDrillArea(Player player, Block centerBlock, ItemStack tool) {
        BlockFace face = getPlayerDirection(player);

        int minX = 0, maxX = 0;
        int minY = 0, maxY = 0;
        int minZ = 0, maxZ = 0;

        switch (face) {
            case UP -> {
                minX = -1; maxX = 1;
                minZ = -1; maxZ = 1;
                minY = 0;  maxY = 1;
            }
            case DOWN -> {
                minX = -1; maxX = 1;
                minZ = -1; maxZ = 1;
                minY = -1; maxY = 0;
            }
            case NORTH -> {
                minX = -1; maxX = 1;
                minY = -1; maxY = 1;
                minZ = -1; maxZ = 0;
            }
            case SOUTH -> {
                minX = -1; maxX = 1;
                minY = -1; maxY = 1;
                minZ = 0;  maxZ = 1;
            }
            case WEST -> {
                minY = -1; maxY = 1;
                minZ = -1; maxZ = 1;
                minX = -1; maxX = 0;
            }
            case EAST -> {
                minY = -1; maxY = 1;
                minZ = -1; maxZ = 1;
                minX = 0;  maxX = 1;
            }
            default -> {
                minX = -1; maxX = 1;
                minY = -1; maxY = 1;
            }
        }

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    if (x == 0 && y == 0 && z == 0) continue;

                    Block target = centerBlock.getRelative(x, y, z);
                    Material targetType = target.getType();

                    if (targetType.isAir() || targetType == Material.BEDROCK || targetType == Material.BARRIER || target.getBlockData().getMaterial().isAir()) {
                        continue;
                    }

                    if (plugin.getCustomTableManager().isCustomTableBlock(target)) {
                        continue;
                    }

                    String locKey = getBlockKey(target);
                    if (processingBlocks.contains(locKey)) continue;

                    processingBlocks.add(locKey);

                    BlockBreakEvent breakEvent = new BlockBreakEvent(target, player);
                    plugin.getServer().getPluginManager().callEvent(breakEvent);

                    if (!breakEvent.isCancelled()) {
                        handleDrops(target, tool, player, breakEvent.getExpToDrop());
                        target.setType(Material.AIR);
                        damageTool(player, tool);
                    }

                    processingBlocks.remove(locKey);

                    if (tool.getType().isAir()) {
                        return;
                    }
                }
            }
        }
    }

    private void handleDrops(Block block, ItemStack tool, Player player, int expToDrop) {
        Collection<ItemStack> drops = block.getDrops(tool, player);

        boolean autoSmelt = getLvl(tool, "autosmelt") > 0 && checkChance("autosmelt", getLvl(tool, "autosmelt"));
        // Только Телепатия
        boolean telepathy = getLvl(tool, "telepathy") > 0;
        int doubleOreLevel = getLvl(tool, "doubleore");

        int expLevel = getLvl(tool, "experienced");
        if (expLevel > 0 && checkChance("experienced", expLevel)) {
            expToDrop += (expLevel * 2);
        }

        if (expToDrop > 0) {
            if (telepathy) {
                player.giveExp(expToDrop);
            } else {
                ExperienceOrb orb = block.getWorld().spawn(block.getLocation().add(0.5, 0.5, 0.5), ExperienceOrb.class);
                orb.setExperience(expToDrop);
            }
        }

        if (drops.isEmpty()) return;

        for (ItemStack drop : drops) {
            ItemStack finalItem = drop.clone();

            if (autoSmelt) {
                finalItem = getSmeltedItem(finalItem);
            }

            if (doubleOreLevel > 0 && checkChance("doubleore", doubleOreLevel)) {
                if (isOreDrop(finalItem.getType())) {
                    finalItem.setAmount(finalItem.getAmount() * 2);
                }
            }

            // Телепатия: сразу в инвентарь игрока
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

    private ItemStack getSmeltedItem(ItemStack item) {
        Material type = item.getType();
        Material result = switch (type) {
            case IRON_ORE -> Material.IRON_INGOT;
            case GOLD_ORE, NETHER_GOLD_ORE -> Material.GOLD_INGOT;
            case ANCIENT_DEBRIS -> Material.NETHERITE_SCRAP;
            case COBBLESTONE -> Material.STONE;
            case SAND, RED_SAND -> Material.GLASS;
            case CLAY_BALL -> Material.BRICK;
            case NETHERRACK -> Material.NETHER_BRICK;
            case WET_SPONGE -> Material.SPONGE;
            case OAK_LOG, SPRUCE_LOG, BIRCH_LOG, JUNGLE_LOG, ACACIA_LOG, DARK_OAK_LOG,
                 STRIPPED_OAK_LOG, STRIPPED_SPRUCE_LOG, STRIPPED_BIRCH_LOG, STRIPPED_JUNGLE_LOG,
                 STRIPPED_ACACIA_LOG, STRIPPED_DARK_OAK_LOG, OAK_WOOD, SPRUCE_WOOD, BIRCH_WOOD,
                 JUNGLE_WOOD, ACACIA_WOOD, DARK_OAK_WOOD, STRIPPED_OAK_WOOD, STRIPPED_SPRUCE_WOOD,
                 STRIPPED_BIRCH_WOOD, STRIPPED_JUNGLE_WOOD, STRIPPED_ACACIA_WOOD, STRIPPED_DARK_OAK_WOOD -> Material.CHARCOAL;
            default -> type;
        };
        return new ItemStack(result, item.getAmount());
    }

    private boolean isOreDrop(Material material) {
        String name = material.name();
        return name.endsWith("_INGOT") || name.equals("DIAMOND") || name.equals("EMERALD")
                || name.equals("COAL") || name.equals("REDSTONE") || name.equals("LAPIS_LAZULI")
                || name.equals("QUARTZ") || name.equals("NETHERITE_SCRAP");
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

    private BlockFace getPlayerDirection(Player player) {
        float pitch = player.getLocation().getPitch();
        if (pitch <= -45.0f) return BlockFace.UP;
        if (pitch >= 45.0f) return BlockFace.DOWN;
        return player.getFacing();
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