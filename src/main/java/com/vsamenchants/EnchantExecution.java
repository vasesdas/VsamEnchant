package com.vsamenchants;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Зачарование "Казнь" для мечей.
 * Механика: моментальное гарантированное добивание цели при низком уровне здоровья.
 */
public class EnchantExecution extends CustomEnchant implements Listener {

    public EnchantExecution() {
        super("execution", EnchantTarget.SWORD);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onHit(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player)) return;
        if (event.getEntity() instanceof ArmorStand) return; // Защита от NPE
        if (!(event.getEntity() instanceof LivingEntity target)) return;

        if (!isEnabled()) return;

        ItemStack weapon = player.getInventory().getItemInMainHand();
        int level = VsamEnchants.getInstance().getEnchantManager().getEnchantLevel(weapon, this);
        if (level <= 0) return;

        double chance = getChance(level);
        if (Math.random() * 100.0 > chance) return;

        AttributeInstance maxHpAttr = target.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        if (maxHpAttr == null) return;

        double maxHealth = maxHpAttr.getValue();
        double currentHealth = target.getHealth();
        double thresholdPercentage = level * 0.10; // 1 лвл = 10%, 2 лвл = 20%, 3 лвл = 30%

        if (currentHealth / maxHealth <= thresholdPercentage) {
            // Наносим критический урон 10000.0, пробивающий любую броню и сопротивление
            event.setDamage(10000.0);

            Location loc = target.getLocation().add(0, 1, 0);
            target.getWorld().spawnParticle(Particle.BLOCK_CRACK, loc, 50, 0.3, 0.5, 0.3, Material.REDSTONE_BLOCK.createBlockData());
            target.getWorld().playSound(loc, Sound.ENTITY_WITHER_BREAK_BLOCK, 1.0f, 0.5f);
            target.getWorld().playSound(loc, Sound.ENTITY_PLAYER_DEATH, 1.0f, 0.5f);

            player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&8[&4VE&8] &cСработала Казнь!"));
        }
    }
}