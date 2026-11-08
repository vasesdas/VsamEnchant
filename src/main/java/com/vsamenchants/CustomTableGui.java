package com.vsamenchants;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

/**
 * Графический интерфейс Алтаря с разделением зачарований по тирам и шансам.
 */
public class CustomTableGui {

    public static final String TITLE = ChatColor.translateAlternateColorCodes('&', "&5&lАлтарь Зачарований");
    public static final int ITEM_SLOT = 19;
    public static final int LAPIS_SLOT = 25;

    public static final int TIER_1_SLOT = 38;
    public static final int TIER_2_SLOT = 40;
    public static final int TIER_3_SLOT = 42;

    public static void open(Player player, VsamEnchants plugin) {
        Inventory inv = Bukkit.createInventory(null, 54, TITLE);
        fillBackground(inv);

        inv.setItem(10, createGuiItem(Material.PURPLE_STAINED_GLASS_PANE, "&dПоместите предмет ниже ⇩", null));
        inv.setItem(16, createGuiItem(Material.BLUE_STAINED_GLASS_PANE, "&9Поместите лазурит ниже ⇩", null));

        updateTierButtons(inv, null, plugin);

        player.openInventory(inv);
    }

    public static void fillBackground(Inventory inv) {
        ItemStack filler = createGuiItem(Material.BLACK_STAINED_GLASS_PANE, " ", null);
        for (int i = 0; i < 54; i++) {
            if (i == ITEM_SLOT || i == LAPIS_SLOT || i == TIER_1_SLOT || i == TIER_2_SLOT || i == TIER_3_SLOT) continue;
            inv.setItem(i, filler);
        }

        inv.setItem(4, createGuiItem(Material.ENCHANTING_TABLE, "&5&lДревний Алтарь", Arrays.asList(
                "&7Поместите экипировку и лазурит,",
                "&7затем выберите ранг таинства.",
                "",
                "&fКаждый ранг имеет свой уникальный",
                "&fпул чар от обычных до мифических!",
                "",
                "&cВнимание: &7При неудаче ингредиенты",
                "&7сгорают, но предмет сохраняется!"
        )));
    }

    public static void updateTierButtons(Inventory inv, ItemStack targetItem, VsamEnchants plugin) {
        inv.setItem(TIER_1_SLOT, createTierButton(1, targetItem, plugin));
        inv.setItem(TIER_2_SLOT, createTierButton(2, targetItem, plugin));
        inv.setItem(TIER_3_SLOT, createTierButton(3, targetItem, plugin));
    }

    private static ItemStack createTierButton(int tier, ItemStack targetItem, VsamEnchants plugin) {
        String path = "custom-table.tiers.tier-" + tier;
        String name = plugin.getConfig().getString(path + ".name", "&eТир " + tier);
        double chance = plugin.getConfig().getDouble(path + ".chance", 75.0);
        int expCost = plugin.getConfig().getInt(path + ".exp-levels", 10);
        int lapisCost = plugin.getConfig().getInt(path + ".lapis-cost", 1);
        String desc = plugin.getConfig().getString(path + ".description", "");

        Material mat = switch (tier) {
            case 1 -> Material.BOOK;
            case 2 -> Material.ENCHANTED_BOOK;
            default -> Material.NETHER_STAR;
        };

        List<String> lore = new ArrayList<>();
        lore.add("");
        if (!desc.isEmpty()) {
            lore.add(color("&7" + desc));
            lore.add("");
        }
        lore.add(color("&fШанс успеха: &a" + chance + "%"));
        lore.add(color("&fТребуется уровней: &e" + expCost + " lvl"));
        lore.add(color("&fТребуется лазурита: &9" + lapisCost + " шт."));
        lore.add("");

        if (targetItem == null || targetItem.getType().isAir()) {
            lore.add(color("&cПоместите предмет в алтарь!"));
        } else {
            lore.add(color("&aНажмите, чтобы провести обряд!"));
        }

        return createGuiItem(mat, name, lore);
    }

    public static void processEnchant(Player player, Inventory inv, int tier, VsamEnchants plugin) {
        ItemStack target = inv.getItem(ITEM_SLOT);
        if (target == null || target.getType().isAir()) {
            player.sendMessage(color("&cПоместите предмет в алтарь перед началом обряда!"));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return;
        }

        String path = "custom-table.tiers.tier-" + tier;
        int expCost = plugin.getConfig().getInt(path + ".exp-levels", 10);
        int lapisCost = plugin.getConfig().getInt(path + ".lapis-cost", 1);
        double chance = plugin.getConfig().getDouble(path + ".chance", 70.0);
        int levelMin = plugin.getConfig().getInt(path + ".level-min", 1);
        int levelMax = plugin.getConfig().getInt(path + ".level-max", 1);

        // Список разрешенных зачарований строго для этого тира
        List<String> allowedEnchantIds = plugin.getConfig().getStringList(path + ".enchants");

        if (player.getLevel() < expCost) {
            player.sendMessage(color("&cВам не хватает уровней опыта! Требуется: &e" + expCost + " lvl"));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return;
        }

        ItemStack lapis = inv.getItem(LAPIS_SLOT);
        if (lapisCost > 0) {
            if (lapis == null || lapis.getType() != Material.LAPIS_LAZULI || lapis.getAmount() < lapisCost) {
                player.sendMessage(color("&cНедостаточно лазурита! Требуется: &9" + lapisCost + " шт."));
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                return;
            }
        }

        // Фильтруем зачарования, доступные для данного тира И подходящие под целевой предмет
        List<CustomEnchant> availableEnchants = new ArrayList<>();
        for (String id : allowedEnchantIds) {
            CustomEnchant enchant = plugin.getEnchantManager().getEnchant(id);
            if (enchant == null || !enchant.isEnabled()) continue;

            if (enchant.getTarget().includes(target.getType())) {
                int currentLevel = plugin.getEnchantManager().getEnchantLevel(target, enchant);
                // Зачарование подходит, если оно еще не максимального уровня для этого тира
                if (currentLevel < Math.min(enchant.getMaxLevel(), levelMax)) {
                    availableEnchants.add(enchant);
                }
            }
        }

        if (availableEnchants.isEmpty()) {
            player.sendMessage(color("&cВ этом таинстве нет доступных зачарований для данного предмета! Выберите другой ранг."));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return;
        }

        // Списание ресурсов
        player.setLevel(player.getLevel() - expCost);
        if (lapisCost > 0 && lapis != null) {
            if (lapis.getAmount() <= lapisCost) {
                inv.setItem(LAPIS_SLOT, null);
            } else {
                lapis.setAmount(lapis.getAmount() - lapisCost);
            }
        }

        // Проверка вероятности
        if (Math.random() * 100.0 > chance) {
            player.playSound(player.getLocation(), Sound.BLOCK_FIRE_EXTINGUISH, 1.0f, 0.8f);
            player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_LAND, 0.5f, 0.5f);
            player.spawnParticle(Particle.SMOKE_LARGE, player.getLocation().add(0, 1, 0), 20, 0.3, 0.3, 0.3, 0.05);
            player.sendMessage(color("&cНеудача! Энергия зачарования рассеялась в воздухе..."));
            return;
        }

        // Успех: случайный выбор из пула этого тира
        CustomEnchant chosen = availableEnchants.get(new Random().nextInt(availableEnchants.size()));
        int currentLvl = plugin.getEnchantManager().getEnchantLevel(target, chosen);

        // Расчет уровня: в пределах levelMin..levelMax, не ниже текущего + 1
        int targetLvl = Math.max(levelMin, currentLvl + 1);
        int finalLevel = Math.min(chosen.getMaxLevel(), Math.min(targetLvl, levelMax));

        plugin.getEnchantManager().applyEnchant(target, chosen, finalLevel);
        inv.setItem(ITEM_SLOT, target);

        player.playSound(player.getLocation(), Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1.0f, 1.0f);
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.2f);
        player.spawnParticle(Particle.ENCHANTMENT_TABLE, player.getLocation().add(0, 1.5, 0), 60, 0.5, 0.5, 0.5, 0.1);
        player.spawnParticle(Particle.PORTAL, player.getLocation().add(0, 1.2, 0), 40, 0.4, 0.4, 0.4, 0.2);

        player.sendMessage(color("&aАлтарь наделил ваш предмет силой: &e" + chosen.getName() + " " + toRoman(finalLevel) + "&a!"));
        updateTierButtons(inv, target, plugin);
    }

    public static ItemStack createGuiItem(Material mat, String name, List<String> lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(color(name));
            if (lore != null) {
                List<String> coloredLore = new ArrayList<>();
                for (String l : lore) coloredLore.add(color(l));
                meta.setLore(coloredLore);
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    private static String toRoman(int number) {
        return switch (number) {
            case 1 -> "I"; case 2 -> "II"; case 3 -> "III"; case 4 -> "IV";
            case 5 -> "V"; default -> String.valueOf(number);
        };
    }

    private static String color(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }
}