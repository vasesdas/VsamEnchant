package com.vsamenchants;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Обработчик всех команд плагина VsamEnchants.
 */
public class VsamCommand implements CommandExecutor, TabCompleter {

    private final VsamEnchants plugin;

    public VsamCommand(VsamEnchants plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("vsamenchants.admin")) {
            sender.sendMessage(color(plugin.getMessagesConfig().getConfig().getString("errors.no-permission", "&cУ вас нет прав!")));
            return true;
        }

        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            sendHelp(sender);
            return true;
        }

        // Подкоманда: /ve menu
        if (args[0].equalsIgnoreCase("menu")) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(color(plugin.getMessagesConfig().getConfig().getString("errors.not-player", "&cТолько для игроков.")));
                return true;
            }
            plugin.getGuiManager().openMainMenu(player);
            return true;
        }

        // Подкоманда: /ve reload
        if (args[0].equalsIgnoreCase("reload")) {
            plugin.getMessagesConfig().reloadConfig();
            plugin.getEnchantsConfig().reloadConfig();
            plugin.reloadConfig();
            sender.sendMessage(color(plugin.getMessagesConfig().getConfig().getString("prefix", "&8[&4VE&8] &r") + "&aКонфигурации успешно перезагружены!"));
            return true;
        }

        // Подкоманда: /ve book <зачарование> [уровень] (выдача себе)
        if (args[0].equalsIgnoreCase("book")) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(color(plugin.getMessagesConfig().getConfig().getString("errors.not-player", "&cТолько для игроков. Используйте /ve give")));
                return true;
            }

            if (args.length < 2) {
                player.sendMessage(color("&cИспользование: /ve book <зачарование> [уровень]"));
                return true;
            }

            String enchantId = args[1].toLowerCase();
            CustomEnchant enchant = plugin.getEnchantManager().getEnchant(enchantId);
            if (enchant == null) {
                player.sendMessage(color("&cЗачарование '&e" + enchantId + "&c' не найдено!"));
                return true;
            }

            int level = 1;
            if (args.length >= 3) {
                try {
                    level = Integer.parseInt(args[2]);
                } catch (NumberFormatException e) {
                    player.sendMessage(color("&cУровень должен быть числом!"));
                    return true;
                }
            }

            level = Math.max(1, Math.min(level, enchant.getMaxLevel()));

            ItemStack book = plugin.getEnchantBookListener().createEnchantBook(enchant, level);
            Map<Integer, ItemStack> leftover = player.getInventory().addItem(book);
            for (ItemStack rem : leftover.values()) {
                player.getWorld().dropItemNaturally(player.getLocation(), rem);
            }

            player.sendMessage(color("&8[&4VE&8] &aВам выдана книга: &e" + enchant.getName() + " " + toRoman(level)));
            return true;
        }

        // Подкоманда: /ve give <игрок> <зачарование> [уровень] [количество]
        if (args[0].equalsIgnoreCase("give")) {
            if (args.length < 3) {
                sender.sendMessage(color("&cИспользование: /ve give <игрок> <зачарование> [уровень] [количество]"));
                return true;
            }

            Player target = Bukkit.getPlayer(args[1]);
            if (target == null || !target.isOnline()) {
                sender.sendMessage(color("&cИгрок '&e" + args[1] + "&c' не найден или не в сети!"));
                return true;
            }

            String enchantId = args[2].toLowerCase();
            CustomEnchant enchant = plugin.getEnchantManager().getEnchant(enchantId);
            if (enchant == null) {
                sender.sendMessage(color("&cЗачарование '&e" + enchantId + "&c' не найдено!"));
                return true;
            }

            int level = 1;
            if (args.length >= 4) {
                try {
                    level = Integer.parseInt(args[3]);
                } catch (NumberFormatException e) {
                    sender.sendMessage(color("&cУровень должен быть числом!"));
                    return true;
                }
            }
            level = Math.max(1, Math.min(level, enchant.getMaxLevel()));

            int amount = 1;
            if (args.length >= 5) {
                try {
                    amount = Integer.parseInt(args[4]);
                } catch (NumberFormatException e) {
                    sender.sendMessage(color("&cКоличество должно быть числом!"));
                    return true;
                }
            }
            amount = Math.max(1, Math.min(amount, 64));

            ItemStack book = plugin.getEnchantBookListener().createEnchantBook(enchant, level);
            book.setAmount(amount);

            Map<Integer, ItemStack> leftover = target.getInventory().addItem(book);
            for (ItemStack rem : leftover.values()) {
                target.getWorld().dropItemNaturally(target.getLocation(), rem);
            }

            sender.sendMessage(color("&8[&4VE&8] &aИгроку &e" + target.getName() + " &aвыдана книга &e" + enchant.getName() + " " + toRoman(level) + " &7(x" + amount + ")"));
            target.sendMessage(color("&8[&4VE&8] &aВам выдана книга зачарования: &e" + enchant.getName() + " " + toRoman(level) + " &7(x" + amount + ")"));
            return true;
        }

        // Подкоманда: /ve apply <зачарование> [уровень]
        if (args[0].equalsIgnoreCase("apply")) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(color(plugin.getMessagesConfig().getConfig().getString("errors.not-player", "&cТолько для игроков.")));
                return true;
            }

            if (args.length < 2) {
                player.sendMessage(color("&cИспользование: /ve apply <зачарование> [уровень]"));
                return true;
            }

            String enchantId = args[1].toLowerCase();
            CustomEnchant enchant = plugin.getEnchantManager().getEnchant(enchantId);
            if (enchant == null) {
                player.sendMessage(color("&cЗачарование '&e" + enchantId + "&c' не найдено!"));
                return true;
            }

            int level = 1;
            if (args.length >= 3) {
                try {
                    level = Integer.parseInt(args[2]);
                } catch (NumberFormatException e) {
                    player.sendMessage(color("&cУровень должен быть числом!"));
                    return true;
                }
            }
            level = Math.max(1, Math.min(level, enchant.getMaxLevel()));

            ItemStack item = player.getInventory().getItemInMainHand();
            if (item.getType().isAir()) {
                player.sendMessage(color("&cВозьмите предмет в руку!"));
                return true;
            }

            if (!enchant.getTarget().includes(item.getType())) {
                player.sendMessage(color("&cЭто зачарование нельзя применить на этот предмет!"));
                return true;
            }

            plugin.getEnchantManager().applyEnchant(item, enchant, level);
            player.sendMessage(color("&aУспешно нанесено зачарование &e" + enchant.getName() + " " + toRoman(level) + "&a!"));
            return true;
        }

        // Подкоманда: /ve info <зачарование>
        if (args[0].equalsIgnoreCase("info")) {
            if (args.length < 2) {
                sender.sendMessage(color("&cИспользование: /ve info <зачарование>"));
                return true;
            }

            String enchantId = args[1].toLowerCase();
            CustomEnchant enchant = plugin.getEnchantManager().getEnchant(enchantId);
            if (enchant == null) {
                sender.sendMessage(color("&cЗачарование '&e" + enchantId + "&c' не найдено!"));
                return true;
            }

            sender.sendMessage(color("&8=== &4Информация о зачаровании &8==="));
            sender.sendMessage(color("&7ID: &c" + enchant.getId()));
            sender.sendMessage(color("&7Название: &r" + enchant.getName()));
            sender.sendMessage(color("&7Макс. Уровень: &e" + enchant.getMaxLevel()));
            sender.sendMessage(color("&7Базовый шанс: &e" + enchant.getChance(1) + "%"));
            sender.sendMessage(color("&7Кулдаун: &e" + enchant.getCooldown() + " сек."));
            sender.sendMessage(color("&7Подходит для: &b" + enchant.getTarget().name()));
            return true;
        }

        sendHelp(sender);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("vsamenchants.admin")) return new ArrayList<>();

        if (args.length == 1) {
            return filter(Arrays.asList("help", "reload", "apply", "info", "menu", "book", "give"), args[0]);
        }

        if (args[0].equalsIgnoreCase("give")) {
            if (args.length == 2) {
                return filter(Bukkit.getOnlinePlayers().stream().map(Player::getName).collect(Collectors.toList()), args[1]);
            }
            if (args.length == 3) {
                return filter(new ArrayList<>(plugin.getEnchantManager().getRegisteredIds()), args[2]);
            }
            if (args.length == 4) {
                return getLevelSuggestions(args[2], args[3]);
            }
            if (args.length == 5) {
                return filter(Arrays.asList("1", "4", "16", "32", "64"), args[4]);
            }
        }

        if (args[0].equalsIgnoreCase("book")) {
            if (args.length == 2) {
                return filter(new ArrayList<>(plugin.getEnchantManager().getRegisteredIds()), args[1]);
            }
            if (args.length == 3) {
                return getLevelSuggestions(args[1], args[2]);
            }
        }

        if (args[0].equalsIgnoreCase("apply") || args[0].equalsIgnoreCase("info")) {
            if (args.length == 2) {
                return filter(new ArrayList<>(plugin.getEnchantManager().getRegisteredIds()), args[1]);
            }
            if (args.length == 3 && args[0].equalsIgnoreCase("apply")) {
                return getLevelSuggestions(args[1], args[2]);
            }
        }

        return new ArrayList<>();
    }

    private List<String> getLevelSuggestions(String enchantId, String currentArg) {
        CustomEnchant enchant = plugin.getEnchantManager().getEnchant(enchantId.toLowerCase());
        List<String> levels = new ArrayList<>();
        int max = (enchant != null) ? enchant.getMaxLevel() : 10;
        for (int i = 1; i <= max; i++) {
            levels.add(String.valueOf(i));
        }
        return filter(levels, currentArg);
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(color("&8=== &4VsamEnchants Команды &8==="));
        sender.sendMessage(color("&c/ve menu &7- открыть меню зачарований"));
        sender.sendMessage(color("&c/ve book <зачарование> [ур] &7- получить книгу зачарования"));
        sender.sendMessage(color("&c/ve give <игрок> <зачарование> [ур] [кол-во] &7- выдать книгу игроку"));
        sender.sendMessage(color("&c/ve apply <зачарование> [ур] &7- наложить зачарование на предмет в руке"));
        sender.sendMessage(color("&c/ve info <зачарование> &7- информация о зачаровании"));
        sender.sendMessage(color("&c/ve reload &7- перезагрузить конфигурации"));
    }

    private String toRoman(int number) {
        return switch (number) {
            case 1 -> "I"; case 2 -> "II"; case 3 -> "III"; case 4 -> "IV";
            case 5 -> "V"; case 6 -> "VI"; case 7 -> "VII"; case 8 -> "VIII";
            case 9 -> "IX"; case 10 -> "X";
            default -> String.valueOf(number);
        };
    }

    private String color(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }

    private List<String> filter(List<String> list, String start) {
        return list.stream().filter(s -> s.toLowerCase().startsWith(start.toLowerCase())).collect(Collectors.toList());
    }
}