package com.vsamenchants;

import org.bukkit.plugin.java.JavaPlugin;

/**
 * Главный класс плагина VsamEnchants.
 */
public class VsamEnchants extends JavaPlugin {

    private static VsamEnchants instance;

    private CustomConfig messagesConfig;
    private CustomConfig enchantsConfig;
    private EnchantManager enchantManager;
    private EconomyManager economyManager;
    private GuiManager guiManager;
    private EnchantBookListener enchantBookListener;
    private CustomTableManager customTableManager;

    @Override
    public void onEnable() {
        instance = this;

        saveDefaultConfig();
        this.messagesConfig = new CustomConfig(this, "messages.yml");
        this.enchantsConfig = new CustomConfig(this, "enchants.yml");

        this.enchantManager = new EnchantManager(this);
        this.economyManager = new EconomyManager(this);
        this.guiManager = new GuiManager(this);
        this.enchantBookListener = new EnchantBookListener(this);
        this.customTableManager = new CustomTableManager(this);

        // --- РЕГИСТРАЦИЯ ЗАЧАРОВАНИЙ ---
        EnchantExecution executionEnchant = new EnchantExecution();
        enchantManager.registerEnchant(executionEnchant);
        enchantManager.registerEnchant(new SimpleEnchant("vampirism", EnchantTarget.WEAPON));
        enchantManager.registerEnchant(new SimpleEnchant("lightning", EnchantTarget.WEAPON));
        enchantManager.registerEnchant(new SimpleEnchant("poison", EnchantTarget.WEAPON));
        enchantManager.registerEnchant(new SimpleEnchant("stun", EnchantTarget.WEAPON));
        enchantManager.registerEnchant(new SimpleEnchant("hunter", EnchantTarget.WEAPON));
        enchantManager.registerEnchant(new SimpleEnchant("bleeding", EnchantTarget.WEAPON));
        enchantManager.registerEnchant(new SimpleEnchant("antitotem", EnchantTarget.WEAPON));

        enchantManager.registerEnchant(new SimpleEnchant("titan", EnchantTarget.ARMOR));
        enchantManager.registerEnchant(new SimpleEnchant("dodge", EnchantTarget.ARMOR));
        enchantManager.registerEnchant(new SimpleEnchant("thornsplus", EnchantTarget.ARMOR));

        enchantManager.registerEnchant(new SimpleEnchant("drill", EnchantTarget.TOOL));
        enchantManager.registerEnchant(new SimpleEnchant("autosmelt", EnchantTarget.TOOL));
        enchantManager.registerEnchant(new SimpleEnchant("magnet", EnchantTarget.TOOL));
        enchantManager.registerEnchant(new SimpleEnchant("telepathy", EnchantTarget.TOOL));
        enchantManager.registerEnchant(new SimpleEnchant("haste", EnchantTarget.TOOL));
        enchantManager.registerEnchant(new SimpleEnchant("nightminer", EnchantTarget.TOOL));
        enchantManager.registerEnchant(new SimpleEnchant("doubleore", EnchantTarget.PICKAXE));
        enchantManager.registerEnchant(new SimpleEnchant("experienced", EnchantTarget.TOOL));
        enchantManager.registerEnchant(new SimpleEnchant("farmer", EnchantTarget.HOE));
        enchantManager.registerEnchant(new SimpleEnchant("lumberjack", EnchantTarget.AXE));

        // Регистрация слушателей событий
        getServer().getPluginManager().registerEvents(guiManager, this);
        getServer().getPluginManager().registerEvents(executionEnchant, this);
        getServer().getPluginManager().registerEvents(new GlobalCombatListener(this), this);
        getServer().getPluginManager().registerEvents(new GlobalMiningListener(this), this);
        getServer().getPluginManager().registerEvents(enchantBookListener, this);
        getServer().getPluginManager().registerEvents(new GlobalFarmingListener(this), this);
        getServer().getPluginManager().registerEvents(new CustomTableListener(this), this);

        // Регистрация команд
        VsamCommand cmd = new VsamCommand(this);
        if (getCommand("vsamenchant") != null) {
            getCommand("vsamenchant").setExecutor(cmd);
            getCommand("vsamenchant").setTabCompleter(cmd);
        }

        getLogger().info("VsamEnchants v" + getDescription().getVersion() + " с Алтарем Зачарований успешно запущен!");
    }

    @Override
    public void onDisable() {
        getLogger().info("VsamEnchants выключен!");
    }

    public static VsamEnchants getInstance() {
        return instance;
    }

    public CustomConfig getMessagesConfig() {
        return messagesConfig;
    }

    public CustomConfig getEnchantsConfig() {
        return enchantsConfig;
    }

    public EnchantManager getEnchantManager() {
        return enchantManager;
    }

    public EconomyManager getEconomyManager() {
        return economyManager;
    }

    public GuiManager getGuiManager() {
        return guiManager;
    }

    public EnchantBookListener getEnchantBookListener() {
        return enchantBookListener;
    }

    public CustomTableManager getCustomTableManager() {
        return customTableManager;
    }
}