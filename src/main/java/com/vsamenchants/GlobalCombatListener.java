package com.vsamenchants;

import org.bukkit.ChatColor;
import org.bukkit.Color;
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
import org.bukkit.event.entity.EntityResurrectEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Оптимизированный слушатель всех боевых зачарований с исправленным порядком вычислений.
 */
public class GlobalCombatListener implements Listener {

    private final VsamEnchants plugin;
    private final EnchantManager manager;

    // Регистрация последнего атакующего для корректной работы Анти-Тотема
    private record LastHitInfo(UUID attackerId, ItemStack weapon, long timestamp) {}
    private final Map<UUID, LastHitInfo> lastAttackers = new ConcurrentHashMap<>();

    public GlobalCombatListener(VsamEnchants plugin) {
        this.plugin = plugin;
        this.manager = plugin.getEnchantManager();
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onCombat(EntityDamageByEntityEvent event) {
        // Игнорируем стойки для брони во избежание NPE при обращении к атрибутам здоровья
        if (event.getEntity() instanceof ArmorStand) return;
        if (!(event.getEntity() instanceof LivingEntity target)) return;

        // ==========================================
        // 1. ЗАЩИТА: УКЛОНЕНИЕ (ПРОВЕРЯЕТСЯ ПЕРВЫМ!)
        // ==========================================
        if (target instanceof Player defender) {
            ItemStack[] armor = defender.getInventory().getArmorContents();
            if (armor != null) {
                int dodgeLevel = 0;
                for (ItemStack piece : armor) {
                    dodgeLevel += getLvl(piece, "dodge");
                }

                // Шанс уклонения: 2% за уровень зачарования
                if (dodgeLevel > 0 && Math.random() * 100.0 < (dodgeLevel * 2.0)) {
                    event.setCancelled(true);
                    defender.getWorld().playSound(defender.getLocation(), Sound.ENTITY_ENDER_DRAGON_FLAP, 1.0f, 2.0f);
                    defender.sendMessage(ChatColor.GREEN + "Вы уклонились от атаки!");
                    return; // Полностью прерываем дальнейшую обработку урона и эффектов атаки
                }
            }
        }

        // ==========================================
        // 2. АТАКА (Оружие атакующего)
        // ==========================================
        if (event.getDamager() instanceof Player attacker) {
            ItemStack weapon = attacker.getInventory().getItemInMainHand();
            if (!weapon.getType().isAir()) {

                // Фиксируем атаку для Анти-Тотема
                lastAttackers.put(target.getUniqueId(), new LastHitInfo(attacker.getUniqueId(), weapon.clone(), System.currentTimeMillis()));

                // 1. Вампиризм
                int vampLevel = getLvl(weapon, "vampirism");
                if (vampLevel > 0 && checkChance("vampirism", vampLevel)) {
                    double heal = event.getFinalDamage() * (0.10 * vampLevel);
                    AttributeInstance maxHealthAttr = attacker.getAttribute(Attribute.GENERIC_MAX_HEALTH);
                    if (maxHealthAttr != null) {
                        attacker.setHealth(Math.min(maxHealthAttr.getValue(), attacker.getHealth() + heal));
                        attacker.playSound(attacker.getLocation(), Sound.ENTITY_WITCH_DRINK, 0.5f, 1.5f);
                    }
                }

                // 2. Молния
                int lightLevel = getLvl(weapon, "lightning");
                if (lightLevel > 0 && checkChance("lightning", lightLevel)) {
                    target.getWorld().strikeLightningEffect(target.getLocation());
                    event.setDamage(event.getDamage() + (2.0 * lightLevel));
                }

                // 3. Отравление
                int poisonLevel = getLvl(weapon, "poison");
                if (poisonLevel > 0 && checkChance("poison", poisonLevel)) {
                    target.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 100, poisonLevel - 1));
                }

                // 4. Оглушение
                int stunLevel = getLvl(weapon, "stun");
                if (stunLevel > 0 && checkChance("stun", stunLevel)) {
                    target.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 60, stunLevel));
                    target.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 60, 0));
                    target.getWorld().playSound(target.getLocation(), Sound.BLOCK_ANVIL_PLACE, 1.0f, 0.5f);
                }

                // 5. Охотник
                int hunterLevel = getLvl(weapon, "hunter");
                if (hunterLevel > 0) {
                    AttributeInstance maxHpAttr = target.getAttribute(Attribute.GENERIC_MAX_HEALTH);
                    if (maxHpAttr != null && target.getHealth() >= maxHpAttr.getValue() - 1.0) {
                        event.setDamage(event.getDamage() * (1.0 + (0.2 * hunterLevel)));
                    }
                }

                // 6. Кровотечение
                int bleedLevel = getLvl(weapon, "bleeding");
                if (bleedLevel > 0 && checkChance("bleeding", bleedLevel)) {
                    target.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 80, bleedLevel - 1));
                    target.getWorld().spawnParticle(Particle.REDSTONE, target.getLocation().add(0, 1, 0), 20, new Particle.DustOptions(Color.RED, 1.5f));
                }
            }
        }

        // ==========================================
        // 3. ЗАЩИТА (Титан и Шипы+)
        // ==========================================
        if (target instanceof Player defender) {
            ItemStack[] armor = defender.getInventory().getArmorContents();
            if (armor != null) {
                int titanLevel = 0, thornsLevel = 0;
                for (ItemStack piece : armor) {
                    titanLevel += getLvl(piece, "titan");
                    thornsLevel += getLvl(piece, "thornsplus");
                }

                // Титан (снижение урона)
                if (titanLevel > 0) {
                    double reduction = Math.min(0.05 * titanLevel, 0.80);
                    event.setDamage(event.getDamage() * (1.0 - reduction));
                }

                // Шипы+ (возврат урона)
                if (thornsLevel > 0 && event.getDamager() instanceof LivingEntity attackerEntity) {
                    if (Math.random() * 100.0 < (thornsLevel * 5.0)) {
                        attackerEntity.damage(event.getDamage() * 0.3);
                        attackerEntity.getWorld().spawnParticle(Particle.CRIT_MAGIC, attackerEntity.getLocation().add(0, 1, 0), 10);
                    }
                }
            }
        }
    }

    /**
     * Анти-Тотем: отменяет срабатывание тотема у противника.
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onTotemPop(EntityResurrectEvent event) {
        if (!(event.getEntity() instanceof Player target)) return;

        LastHitInfo lastHit = lastAttackers.get(target.getUniqueId());
        if (lastHit == null) return;

        // Проверяем, был ли удар совершен не более 1.5 секунд назад
        if (System.currentTimeMillis() - lastHit.timestamp > 1500) {
            lastAttackers.remove(target.getUniqueId());
            return;
        }

        int antiTotemLevel = getLvl(lastHit.weapon, "antitotem");
        if (antiTotemLevel > 0 && checkChance("antitotem", antiTotemLevel)) {
            event.setCancelled(true); // Тотем не сработает — игрок умирает

            Player killer = plugin.getServer().getPlayer(lastHit.attackerId);
            if (killer != null && killer.isOnline()) {
                killer.sendMessage(ChatColor.translateAlternateColorCodes('&', "&8[&4VE&8] &cВаш меч пробил тотем противника!"));
            }

            target.getWorld().playSound(target.getLocation(), Sound.ENTITY_WITHER_DEATH, 1.0f, 0.5f);
        }

        lastAttackers.remove(target.getUniqueId());
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