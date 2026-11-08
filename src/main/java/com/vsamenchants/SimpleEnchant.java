package com.vsamenchants;

/**
 * Универсальный класс для регистрации зачарований,
 * механика которых обрабатывается в глобальных слушателях (GlobalCombatListener и т.д.).
 */
public class SimpleEnchant extends CustomEnchant {

    public SimpleEnchant(String id, EnchantTarget target) {
        super(id, target);
    }
}