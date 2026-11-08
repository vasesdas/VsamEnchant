package com.vsamenchants;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Основной обработчик команд плагина.
 */
public class VsamCommand implements CommandExecutor, TabCompleter {

    private final VsamEnchants plugin;

    public VsamCommand(VsamEnchants plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            if (sender instanceof Player player) {
                plugin.getGuiManager().openMainMenu(player);
            } else {
                sender.sendMessage(color("&cИспользуйте: /" + label + " help"));
            }
            return true;
        }

        String sub = args[0].toLowerCase();

        switch (sub) {
            case "craft" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(color("&cТолько для игроков!"));
                    return true;
                }
                TableCraftEditorGui.open(player, plugin, false);
                return true;
            }
            case "setcraft" -> {
                if (!sender.hasPermission("vsamenchants.admin")) {
                    sender.sendMessage(color("&cУ вас нет прав!"));
                    return true;
                }
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(color("&cТолько для игроков!"));
                    return true;
                }
                TableCraftEditorGui.open(player, plugin, true);
                return true;
            }
            case "table", "gettable" -> {
                if (!sender.hasPermission("vsamenchants.admin")) {
                    sender.sendMessage(color("&cУ вас нет прав!"));
                    return true;
                }
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(color("&cТолько для игроков!"));
                    return true;
                }
                ItemStack table = plugin.getCustomTableManager().createTableItem(1);
                player.getInventory().addItem(table);
                player.sendMessage(color("&8[&4VE&8] &aВы получили &5Алтарь Зачарований&a!"));
                return true;
            }
            case "reload" -> {
                if (!sender.hasPermission("vsamenchants.admin")) {
                    sender.sendMessage(color("&cУ вас нет прав!"));
                    return true;
                }
                plugin.reloadConfig();
                plugin.getEnchantsConfig().reloadConfig();
                plugin.getMessagesConfig().reloadConfig();
                plugin.getCustomTableManager().registerRecipe();
                sender.sendMessage(color("&8[&4VE&8] &aКонфигурация и рецепты успешно перезагружены!"));
                return true;
            }
            default -> {
                sender.sendMessage(color("&8=== &4VsamEnchants Команды &8==="));
                sender.sendMessage(color("&e/" + label + " &7- Открыть каталог зачарований"));
                sender.sendMessage(color("&e/" + label + " craft &7- Посмотреть крафт Алтаря"));
                if (sender.hasPermission("vsamenchants.admin")) {
                    sender.sendMessage(color("&e/" + label + " setcraft &7- Настроить крафт Алтаря через GUI"));
                    sender.sendMessage(color("&e/" + label + " table &7- Выдать Алтарь Зачарований"));
                    sender.sendMessage(color("&e/" + label + " reload &7- Перезагрузить конфиги"));
                }
                return true;
            }
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> completions = new ArrayList<>(Arrays.asList("craft"));
            if (sender.hasPermission("vsamenchants.admin")) {
                completions.addAll(Arrays.asList("table", "setcraft", "reload"));
            }
            return completions;
        }
        return Collections.emptyList();
    }

    private String color(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }
}