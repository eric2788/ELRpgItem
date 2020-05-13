package com.ericlam.mc.elrpgitem

import org.bukkit.Material
import org.bukkit.attribute.Attribute
import org.bukkit.enchantments.Enchantment
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack
import kotlin.random.Random

fun Enchantment.randomLevel(): Int {
    return (this.startLevel..this.maxLevel).rpgRandom()
}

fun ItemStack.randomEnchantments(amount: Int) {
    val canEnchants = Enchantment.values().filter { it.itemTarget.includes(this) && it.canEnchantItem(ItemStack(this)) }
    val map = mutableMapOf<Enchantment, Int>()
    for (i in 1..amount){
        val r = canEnchants.filter { e -> (map.keys.lastOrNull()?.let { e.conflictsWith(it) } ?: true) && e !in map }.takeIf { it.isNotEmpty() }?.random() ?: break
        map[r] = r.randomLevel()
    }
    map.forEach(this::addEnchantment)
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
    val step = (1 - start) / this.last
    for (i in this) {
        if (Random.nextDouble() < start) return i
        s += step
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

