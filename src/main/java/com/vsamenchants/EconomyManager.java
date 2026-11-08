package com.vsamenchants;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.RegisteredServiceProvider;

/**
 * Менеджер экономики для работы с Vault.
 */
public class EconomyManager {

    private final VsamEnchants plugin;
    private Economy economy = null;

    public EconomyManager(VsamEnchants plugin) {
        this.plugin = plugin;
        setupEconomy();
    }

    /**
     * Инициализация экономики Vault.
     */
    private void setupEconomy() {
        if (plugin.getServer().getPluginManager().getPlugin("Vault") == null) {
            plugin.getLogger().warning("Плагин Vault не найден! Экономика отключена.");
            return;
        }

        RegisteredServiceProvider<Economy> rsp = plugin.getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            plugin.getLogger().warning("Провайдер экономики не найден (установите EssentialsX или аналог).");
            return;
        }

        economy = rsp.getProvider();
        plugin.getLogger().info("Vault экономика успешно подключена!");
    }

    /**
     * Проверка, включена ли экономика.
     */
    public boolean isEnabled() {
        return economy != null;
    }

    /**
     * Проверяет, достаточно ли средств у игрока.
     */
    public boolean hasMoney(OfflinePlayer player, double amount) {
        if (economy == null) return false;
        return economy.has(player, amount);
    }

    /**
     * Списывает средства у игрока.
     */
    public void takeMoney(OfflinePlayer player, double amount) {
        if (economy != null) {
            economy.withdrawPlayer(player, amount);
        }
    }

    /**
     * Выдает средства игроку.
     */
    public void giveMoney(OfflinePlayer player, double amount) {
        if (economy != null) {
            economy.depositPlayer(player, amount);
        }
    }
}