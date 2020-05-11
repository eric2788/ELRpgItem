package com.ericlam.mc.elrpgitem

import org.bukkit.Material
import org.bukkit.attribute.Attribute
import org.bukkit.enchantments.Enchantment
import org.bukkit.inventory.EquipmentSlot
import kotlin.random.Random

fun Enchantment.randomLevel(): Int {
    return (this.startLevel..this.maxLevel).rpgRandom()
}

fun Material.randomEnchantments(amount: Int): Map<Enchantment, Int> {
    val canEnchants = Enchantment.values().filter { it.itemTarget.includes(this) }
    val map = mutableMapOf<Enchantment, Int>()
    for (i in 1..amount){
        val r = canEnchants.filter { e -> (map.keys.lastOrNull()?.let { e.conflictsWith(it) } ?: true) && e !in map }.takeIf { it.isNotEmpty() }?.random() ?: return map
        map[r] = r.randomLevel()
    }
    return map
}

fun Material.randomAttributes(amount: Int): Map<Attribute, ELConfig.AttributeMeta> {
    val map = mutableMapOf<Attribute, ELConfig.AttributeMeta>()
    for (i in 1..amount){
        val attr = ELRpgItem.elConfig.attributes.entries.filterNot { it.key in map || !(ItemManager.Item.ALLOW_ATTRIBUTES[it.key]?.contains(this.equipmentSlot) ?: true ) }.takeIf { it.isNotEmpty() }?.random()?.toPair() ?: return map
        map += attr
    }
    return map
}

tailrec fun IntRange.rpgRandom(start: Double = ELRpgItem.elConfig.random.start): Int{
    var s = start
    for (i in this) {
        if (Random.nextDouble() < start) return i
        s += ELRpgItem.elConfig.random.step
    }
    return this.rpgRandom(s)
}

val Material.equipmentSlot: EquipmentSlot
    get() {
        val str = this.toString()
        return when {
            str.endsWith("BOOTS") -> EquipmentSlot.FEET
            str.endsWith("CHESTPLATE") -> EquipmentSlot.CHEST
            str.endsWith("HELMET") -> EquipmentSlot.HEAD
            str.endsWith("LEGGINGS") -> EquipmentSlot.LEGS
            else -> EquipmentSlot.HAND
        }
    }
