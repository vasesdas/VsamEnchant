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
 * Графический интерфейс Алтаря: Единое Таинство 30-го уровня с взвешенным шансом редкости.
 */
public class CustomTableGui {

    public static final String TITLE = ChatColor.translateAlternateColorCodes('&', "&5&lАлтарь Зачарований");
    public static final int ITEM_SLOT = 19;
    public static final int LAPIS_SLOT = 25;
    public static final int ENCHANT_BUTTON_SLOT = 31;

    public static void open(Player player, VsamEnchants plugin) {
        Inventory inv = Bukkit.createInventory(null, 54, TITLE);
        fillBackground(inv);

        inv.setItem(10, createGuiItem(Material.PURPLE_STAINED_GLASS_PANE, "&dПоместите предмет ниже ⇩", null));
        inv.setItem(16, createGuiItem(Material.BLUE_STAINED_GLASS_PANE, "&9Поместите лазурит ниже ⇩", null));

        updateEnchantButton(inv, null, plugin);

        player.openInventory(inv);
    }

    public static void fillBackground(Inventory inv) {
        ItemStack filler = createGuiItem(Material.BLACK_STAINED_GLASS_PANE, " ", null);
        for (int i = 0; i < 54; i++) {
            if (i == ITEM_SLOT || i == LAPIS_SLOT || i == ENCHANT_BUTTON_SLOT) continue;
            inv.setItem(i, filler);
        }

        inv.setItem(4, createGuiItem(Material.ENCHANTING_TABLE, "&5&lДревний Алтарь", Arrays.asList(
                "&7Поместите экипировку и лазурит,",
                "&7чтобы провести таинство зачарования.",
                "",
                "&fСтоимость: &e30 уровней опыта",
                "&fТребуется: &93 лазурита",
                "",
                "&7Редкости зачарований:",
                " &a• Обычные (60%): &7Базовые эффекты",
                " &b• Редкие (30%): &7Продвинутые эффекты",
                " &6• Легендарные (10%): &4Казнь, Анти-Тотем, Бур 3x3",
                "",
                "&cВнимание: &7При неудаче ингредиенты",
                "&7сгорают, но предмет сохраняется!"
        )));
    }

    public static void updateEnchantButton(Inventory inv, ItemStack targetItem, VsamEnchants plugin) {
        int expCost = plugin.getConfig().getInt("custom-table.enchanting.exp-cost", 30);
        int lapisCost = plugin.getConfig().getInt("custom-table.enchanting.lapis-cost", 3);
        double chance = plugin.getConfig().getDouble("custom-table.enchanting.success-chance", 80.0);

        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add(color("&fСтоимость: &e" + expCost + " lvl"));
        lore.add(color("&fТребуется лазурита: &9" + lapisCost + " шт."));
        lore.add(color("&fШанс наложения: &a" + chance + "%"));
        lore.add("");

        if (targetItem == null || targetItem.getType().isAir()) {
            lore.add(color("&cПоместите предмет в алтарь!"));
        } else {
            lore.add(color("&aНажмите, чтобы провести обряд!"));
        }

        inv.setItem(ENCHANT_BUTTON_SLOT, createGuiItem(Material.NETHER_STAR, "&6&lПровести Таинство Зачарования", lore));
    }

    public static void processEnchant(Player player, Inventory inv, VsamEnchants plugin) {
        ItemStack target = inv.getItem(ITEM_SLOT);
        if (target == null || target.getType().isAir()) {
            player.sendMessage(color("&cПоместите предмет в алтарь перед началом обряда!"));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return;
        }

        int expCost = plugin.getConfig().getInt("custom-table.enchanting.exp-cost", 30);
        int lapisCost = plugin.getConfig().getInt("custom-table.enchanting.lapis-cost", 3);
        double chance = plugin.getConfig().getDouble("custom-table.enchanting.success-chance", 80.0);

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

        // Пулы редкостей из config.yml
        List<String> legendaryIds = plugin.getConfig().getStringList("custom-table.enchanting.pools.legendary");
        List<String> rareIds = plugin.getConfig().getStringList("custom-table.enchanting.pools.rare");
        List<String> commonIds = plugin.getConfig().getStringList("custom-table.enchanting.pools.common");

        List<CustomEnchant> legendaryAvailable = filterEnchants(legendaryIds, target, plugin);
        List<CustomEnchant> rareAvailable = filterEnchants(rareIds, target, plugin);
        List<CustomEnchant> commonAvailable = filterEnchants(commonIds, target, plugin);

        List<CustomEnchant> allAvailable = new ArrayList<>();
        allAvailable.addAll(legendaryAvailable);
        allAvailable.addAll(rareAvailable);
        allAvailable.addAll(commonAvailable);

        if (allAvailable.isEmpty()) {
            player.sendMessage(color("&cНа этот предмет больше нельзя наложить зачарований из алтаря!"));
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

        // Проверка шанса на успех
        if (Math.random() * 100.0 > chance) {
            player.playSound(player.getLocation(), Sound.BLOCK_FIRE_EXTINGUISH, 1.0f, 0.8f);
            player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_LAND, 0.5f, 0.5f);
            player.spawnParticle(Particle.SMOKE_LARGE, player.getLocation().add(0, 1, 0), 20, 0.3, 0.3, 0.3, 0.05);
            player.sendMessage(color("&cНеудача! Энергия зачарования рассеялась в воздухе..."));
            return;
        }

        // Взвешенный ролл редкости (10% Легендарные, 30% Редкие, 60% Обычные)
        double roll = Math.random() * 100.0;
        CustomEnchant chosen;

        if (roll < 10.0 && !legendaryAvailable.isEmpty()) {
            chosen = legendaryAvailable.get(new Random().nextInt(legendaryAvailable.size()));
        } else if (roll < 40.0 && !rareAvailable.isEmpty()) {
            chosen = rareAvailable.get(new Random().nextInt(rareAvailable.size()));
        } else if (!commonAvailable.isEmpty()) {
            chosen = commonAvailable.get(new Random().nextInt(commonAvailable.size()));
        } else {
            chosen = allAvailable.get(new Random().nextInt(allAvailable.size()));
        }

        // Расчет уровня: повышаем существующий на +1, либо выдаем стартовый
        int currentLvl = plugin.getEnchantManager().getEnchantLevel(target, chosen);
        int finalLevel = Math.min(chosen.getMaxLevel(), Math.max(1, currentLvl + 1));

        plugin.getEnchantManager().applyEnchant(target, chosen, finalLevel);
        inv.setItem(ITEM_SLOT, target);

        player.playSound(player.getLocation(), Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1.0f, 1.0f);
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.2f);
        player.spawnParticle(Particle.ENCHANTMENT_TABLE, player.getLocation().add(0, 1.5, 0), 60, 0.5, 0.5, 0.5, 0.1);
        player.spawnParticle(Particle.PORTAL, player.getLocation().add(0, 1.2, 0), 40, 0.4, 0.4, 0.4, 0.2);

        player.sendMessage(color("&aАлтарь наделил ваш предмет силой: &e" + chosen.getName() + " " + toRoman(finalLevel) + "&a!"));
        updateEnchantButton(inv, target, plugin);
    }

    private static List<CustomEnchant> filterEnchants(List<String> ids, ItemStack target, VsamEnchants plugin) {
        List<CustomEnchant> list = new ArrayList<>();
        for (String id : ids) {
            CustomEnchant enc = plugin.getEnchantManager().getEnchant(id);
            if (enc == null || !enc.isEnabled()) continue;
            if (enc.getTarget().includes(target.getType())) {
                int curLvl = plugin.getEnchantManager().getEnchantLevel(target, enc);
                if (curLvl < enc.getMaxLevel()) {
                    list.add(enc);
                }
            }
        }
        return list;
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