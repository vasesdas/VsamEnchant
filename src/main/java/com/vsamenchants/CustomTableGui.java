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
import java.util.concurrent.ConcurrentHashMap;

/**
 * Графический интерфейс Алтаря: Зачарование книг с выбором типа и ставкой опыта.
 */
public class CustomTableGui {

    public static final String TITLE = ChatColor.translateAlternateColorCodes('&', "&5&lАлтарь: Сотворение Книг");

    // Основные слоты
    public static final int BOOK_SLOT = 13;
    public static final int LAPIS_SLOT = 22;

    // Слоты категорий
    public static final int CATEGORY_COMBAT_SLOT = 29;
    public static final int CATEGORY_DEFENSE_SLOT = 31;
    public static final int CATEGORY_MINING_SLOT = 33;

    // Слоты изменения ставки опыта
    public static final int BET_MINUS_5_SLOT = 38;
    public static final int BET_MINUS_1_SLOT = 39;
    public static final int BET_INFO_SLOT = 40;
    public static final int BET_PLUS_1_SLOT = 41;
    public static final int BET_PLUS_5_SLOT = 42;

    // Кнопка проведения обряда
    public static final int ENCHANT_BUTTON_SLOT = 49;

    public enum EnchantCategory {
        COMBAT("Оружие и Битва", Material.DIAMOND_SWORD, "custom-table.categories.combat"),
        DEFENSE("Броня и Защита", Material.DIAMOND_CHESTPLATE, "custom-table.categories.defense"),
        MINING("Добыча и Инструменты", Material.DIAMOND_PICKAXE, "custom-table.categories.mining");

        private final String displayName;
        private final Material icon;
        private final String configPath;

        EnchantCategory(String displayName, Material icon, String configPath) {
            this.displayName = displayName;
            this.icon = icon;
            this.configPath = configPath;
        }

        public String getDisplayName() { return displayName; }
        public Material getIcon() { return icon; }
        public String getConfigPath() { return configPath; }
    }

    public static class AltarSession {
        private EnchantCategory category = EnchantCategory.COMBAT;
        private int expBet = 20;

        public EnchantCategory getCategory() { return category; }
        public void setCategory(EnchantCategory category) { this.category = category; }
        public int getExpBet() { return expBet; }
        public void addBet(int amount) { this.expBet = Math.max(10, Math.min(50, this.expBet + amount)); }
    }

    private static final Map<UUID, AltarSession> sessions = new ConcurrentHashMap<>();

    public static AltarSession getSession(Player player) {
        return sessions.computeIfAbsent(player.getUniqueId(), k -> new AltarSession());
    }

    public static void removeSession(Player player) {
        sessions.remove(player.getUniqueId());
    }

    public static void open(Player player, VsamEnchants plugin) {
        Inventory inv = Bukkit.createInventory(null, 54, TITLE);
        AltarSession session = getSession(player);

        fillDecoration(inv);
        renderControls(inv, session, plugin);

        player.openInventory(inv);
    }

    public static void fillDecoration(Inventory inv) {
        ItemStack filler = createGuiItem(Material.BLACK_STAINED_GLASS_PANE, " ", null);
        for (int i = 0; i < 54; i++) {
            inv.setItem(i, filler);
        }

        // Информационные маркеры
        inv.setItem(4, createGuiItem(Material.ENCHANTING_TABLE, "&5&lДревний Алтарь Книг", Arrays.asList(
                "&7Поместите обычную книгу и лазурит,",
                "&7выберите категорию и сделайте ставку опыта.",
                "",
                "&eЧем больше ставка опыта:",
                " &a• Выше шанс успешного обряда (до 95%)",
                " &6• Выше шанс выбить легендарные чары (до 40%)",
                " &d• Выше финальный уровень зачарования!"
        )));

        inv.setItem(12, createGuiItem(Material.PURPLE_STAINED_GLASS_PANE, "&dПустая книга ⇨", null));
        inv.setItem(14, createGuiItem(Material.PURPLE_STAINED_GLASS_PANE, "&d⇦ Пустая книга", null));
        inv.setItem(21, createGuiItem(Material.BLUE_STAINED_GLASS_PANE, "&9Лазурит ⇨", null));
        inv.setItem(23, createGuiItem(Material.BLUE_STAINED_GLASS_PANE, "&9⇦ Лазурит", null));

        // Освобождаем слоты под ввод
        inv.setItem(BOOK_SLOT, null);
        inv.setItem(LAPIS_SLOT, null);
    }

    public static void renderControls(Inventory inv, AltarSession session, VsamEnchants plugin) {
        // 1. Отрисовка кнопок категорий
        renderCategoryButton(inv, CATEGORY_COMBAT_SLOT, EnchantCategory.COMBAT, session);
        renderCategoryButton(inv, CATEGORY_DEFENSE_SLOT, EnchantCategory.DEFENSE, session);
        renderCategoryButton(inv, CATEGORY_MINING_SLOT, EnchantCategory.MINING, session);

        // 2. Кнопки изменения ставки
        inv.setItem(BET_MINUS_5_SLOT, createGuiItem(Material.RED_STAINED_GLASS_PANE, "&c&l-5 Уровней", Arrays.asList("&7Уменьшить ставку на 5")));
        inv.setItem(BET_MINUS_1_SLOT, createGuiItem(Material.ORANGE_STAINED_GLASS_PANE, "&6&l-1 Уровень", Arrays.asList("&7Уменьшить ставку на 1")));

        int bet = session.getExpBet();
        int lapisReq = Math.max(1, bet / 10);
        double successChance = calculateSuccessChance(bet);
        double legendaryChance = calculateLegendaryChance(bet);

        inv.setItem(BET_INFO_SLOT, createGuiItem(Material.EXPERIENCE_BOTTLE, "&e&lСтавка Опыта: &f" + bet + " lvl", Arrays.asList(
                "",
                "&fТребуется лазурита: &9" + lapisReq + " шт.",
                "&fШанс успеха обряда: &a" + String.format("%.1f", successChance) + "%",
                "&fШанс на редкие/топ чары: &6" + String.format("%.1f", legendaryChance) + "%",
                "",
                "&7Используйте соседние кнопки",
                "&7для изменения ставки (10 - 50 lvl)."
        )));

        inv.setItem(BET_PLUS_1_SLOT, createGuiItem(Material.LIME_STAINED_GLASS_PANE, "&a&l+1 Уровень", Arrays.asList("&7Увеличить ставку на 1")));
        inv.setItem(BET_PLUS_5_SLOT, createGuiItem(Material.GREEN_STAINED_GLASS_PANE, "&2&l+5 Уровней", Arrays.asList("&7Увеличить ставку на 5")));

        // 3. Центральная кнопка зачарования
        inv.setItem(ENCHANT_BUTTON_SLOT, createGuiItem(Material.NETHER_STAR, "&6&lСотворить Книгу Зачарования", Arrays.asList(
                "",
                "&7Категория: &e" + session.getCategory().getDisplayName(),
                "&7Списание опыта: &e" + bet + " lvl",
                "&7Списание лазурита: &9" + lapisReq + " шт.",
                "",
                "&aНажмите, чтобы провести обряд!"
        )));
    }

    private static void renderCategoryButton(Inventory inv, int slot, EnchantCategory category, AltarSession session) {
        boolean selected = (session.getCategory() == category);
        List<String> lore = new ArrayList<>();
        lore.add("");
        if (selected) {
            lore.add(color("&a✔ Выбрана данная направленность!"));
        } else {
            lore.add(color("&7Нажмите, чтобы выбрать эту категорию."));
        }
        Material icon = selected ? Material.ENCHANTED_BOOK : category.getIcon();
        String prefix = selected ? "&a&l" : "&7";
        inv.setItem(slot, createGuiItem(icon, prefix + category.getDisplayName(), lore));
    }

    public static double calculateSuccessChance(int bet) {
        // От 50% при 10 lvl до 95% при 50 lvl
        return Math.min(95.0, 40.0 + (bet * 1.1));
    }

    public static double calculateLegendaryChance(int bet) {
        // 0% на низких уровнях, до 40% на 50 lvl
        if (bet < 20) return 0.0;
        return Math.min(40.0, (bet - 20) * 1.33);
    }

    /**
     * Проведение таинства создания книги зачарования.
     */
    public static void processEnchant(Player player, Inventory inv, VsamEnchants plugin) {
        AltarSession session = getSession(player);
        ItemStack bookItem = inv.getItem(BOOK_SLOT);

        // 1. Проверка книги
        if (bookItem == null || bookItem.getType() != Material.BOOK) {
            player.sendMessage(color("&cПоместите обычную пустую книгу в фиолетовый слот алтаря!"));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return;
        }

        int bet = session.getExpBet();
        int lapisReq = Math.max(1, bet / 10);

        // 2. Проверка опыта игрока
        if (player.getLevel() < bet) {
            player.sendMessage(color("&cВам не хватает опыта для вашей ставки! Требуется: &e" + bet + " lvl"));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return;
        }

        // 3. Проверка лазурита
        ItemStack lapisItem = inv.getItem(LAPIS_SLOT);
        if (lapisItem == null || lapisItem.getType() != Material.LAPIS_LAZULI || lapisItem.getAmount() < lapisReq) {
            player.sendMessage(color("&cНедостаточно лазурита! Для этой ставки требуется: &9" + lapisReq + " шт."));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return;
        }

        // 4. Поиск зачарований в выбранной категории
        List<String> poolIds = plugin.getConfig().getStringList(session.getCategory().getConfigPath());
        List<CustomEnchant> pool = new ArrayList<>();
        for (String id : poolIds) {
            CustomEnchant enc = plugin.getEnchantManager().getEnchant(id);
            if (enc != null && enc.isEnabled()) {
                pool.add(enc);
            }
        }

        if (pool.isEmpty()) {
            player.sendMessage(color("&cВ этой категории нет доступных зачарований!"));
            return;
        }

        // Списание ресурсов
        player.setLevel(player.getLevel() - bet);
        if (lapisItem.getAmount() <= lapisReq) {
            inv.setItem(LAPIS_SLOT, null);
        } else {
            lapisItem.setAmount(lapisItem.getAmount() - lapisReq);
        }

        // Проверка шанса на успех
        double successChance = calculateSuccessChance(bet);
        if (Math.random() * 100.0 > successChance) {
            player.playSound(player.getLocation(), Sound.BLOCK_FIRE_EXTINGUISH, 1.0f, 0.8f);
            player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_LAND, 0.5f, 0.5f);
            player.spawnParticle(Particle.SMOKE_LARGE, player.getLocation().add(0, 1.2, 0), 25, 0.3, 0.3, 0.3, 0.05);
            player.sendMessage(color("&cНеудача! Энергия зачарования рассеялась в воздухе... Книга осталась нетронутой."));
            return;
        }

        // Выбор зачарования с учетом ставки на легендарные чары
        double legendaryChance = calculateLegendaryChance(bet);
        List<String> topIds = Arrays.asList("execution", "antitotem", "drill");
        List<CustomEnchant> topEnchants = new ArrayList<>();
        List<CustomEnchant> regularEnchants = new ArrayList<>();

        for (CustomEnchant enc : pool) {
            if (topIds.contains(enc.getId())) {
                topEnchants.add(enc);
            } else {
                regularEnchants.add(enc);
            }
        }

        CustomEnchant chosen;
        if (Math.random() * 100.0 <= legendaryChance && !topEnchants.isEmpty()) {
            chosen = topEnchants.get(new Random().nextInt(topEnchants.size()));
        } else if (!regularEnchants.isEmpty()) {
            chosen = regularEnchants.get(new Random().nextInt(regularEnchants.size()));
        } else {
            chosen = pool.get(new Random().nextInt(pool.size()));
        }

        // Определение уровня книги в зависимости от ставки
        int maxLvl = chosen.getMaxLevel();
        int finalLevel;
        if (bet >= 40) {
            finalLevel = maxLvl; // Высокая ставка = максимальный уровень
        } else if (bet >= 25) {
            finalLevel = Math.min(maxLvl, Math.max(1, new Random().nextInt(maxLvl) + 1));
        } else {
            finalLevel = 1; // Низкая ставка = строго 1 уровень
        }

        // Снимаем 1 пустую книгу
        if (bookItem.getAmount() > 1) {
            bookItem.setAmount(bookItem.getAmount() - 1);
        } else {
            inv.setItem(BOOK_SLOT, null);
        }

        // Создаем зачарованную книгу через слушатель плагина
        ItemStack enchantedBook = plugin.getEnchantBookListener().createEnchantBook(chosen, finalLevel);

        // Помещаем результат в слот книги или в инвентарь/под ноги
        if (inv.getItem(BOOK_SLOT) == null) {
            inv.setItem(BOOK_SLOT, enchantedBook);
        } else {
            Map<Integer, ItemStack> dropLeft = player.getInventory().addItem(enchantedBook);
            for (ItemStack left : dropLeft.values()) {
                player.getWorld().dropItemNaturally(player.getLocation(), left);
            }
        }

        // Эффекты успеха
        player.playSound(player.getLocation(), Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1.0f, 1.0f);
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.2f);
        player.spawnParticle(Particle.ENCHANTMENT_TABLE, player.getLocation().add(0, 1.5, 0), 70, 0.5, 0.5, 0.5, 0.1);
        player.spawnParticle(Particle.PORTAL, player.getLocation().add(0, 1.2, 0), 50, 0.4, 0.4, 0.4, 0.2);

        player.sendMessage(color("&aАлтарь создал книгу: &e" + chosen.getName() + " " + toRoman(finalLevel) + "&a!"));
        renderControls(inv, session, plugin);
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