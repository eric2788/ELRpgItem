package com.ericlam.mc.elrpgitem

import com.ericlam.mc.kotlib.bukkit.BukkitPlugin
import com.ericlam.mc.kotlib.translateColorCode
import net.Indyuce.mmoitems.MMOItems
import net.Indyuce.mmoitems.api.Type
import net.Indyuce.mmoitems.stat.type.ItemStat
import org.bukkit.Material
import org.bukkit.attribute.Attribute
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.Monster
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack
import xuan.cat.XuanCatAPI.NBT
import java.util.*
import kotlin.random.Random

fun Enchantment.randomLevel(): Int {
    return (this.startLevel..this.maxLevel).rpgRandom()
}

fun ItemStack.randomEnchantments(amount: Int) {
    val canEnchants = Enchantment.values().filter { it.itemTarget.includes(this) && it.canEnchantItem(ItemStack(this)) }
    val map = mutableMapOf<Enchantment, Int>()
    for (i in 1..amount) {
        val r = canEnchants.filter { e ->
            (map.keys.lastOrNull()?.let { e.conflictsWith(it) } ?: true) && e !in map
        }.takeIf { it.isNotEmpty() }?.random() ?: break
        map[r] = r.randomLevel()
    }
    map.forEach(this::addEnchantment)
}

fun Material.randomAttributes(amount: Int): Map<Attribute, ELConfig.AttributeMeta> {
    val map = mutableMapOf<Attribute, ELConfig.AttributeMeta>()
    for (i in 1..amount) {
        val attr = ELRpgItem.elConfig.attributes.entries.filterNot {
            it.key in map || !(ELRPGManager.Item.ALLOW_ATTRIBUTES[it.key]?.contains(this.equipmentSlot) ?: true)
        }.takeIf { it.isNotEmpty() }?.random()?.toPair() ?: return map
        map += attr
    }
    return map
}

tailrec fun IntRange.rpgRandom(start: Double = ELRpgItem.elConfig.random.start): Int {
    var s = start
    val step = (1 - start) / this.last
    for (i in this) {
        if (Random.nextDouble() < start) return i
        s += step
    }
    return this.rpgRandom(s)
}

fun Attribute.toName(): String {
    val s = this.toString().toLowerCase().split("_")
    val name = StringBuilder()
    var i = 0
    for (word in s) {
        when {
            word == "generic" -> name.append("generic.")
            i > 0 -> name.append(word.replaceFirst(word.first(), word.first().toUpperCase()))
            else -> {
                name.append(word)
                i++
            }
        }
    }
    return name.toString()
}

fun Monster.toEquipped(named: Boolean): Boolean {
    val eq = this.equipment ?: return false
    with(ELRpgItem.elConfig.named_boss_settings) {
        val qualities = if (named) ELRPGManager.Item.SUPERIOR_SUPER_RARE else ELRPGManager.Item.SUPER_RARE
        val nbt = NBT.getEntityNBT(this@toEquipped)
        nbt.setBoolean("rpg.monster", true)
        nbt.setBoolean("rpg.monster.named", named)
        NBT.setEntityPermanentNBT(this@toEquipped, nbt)
        if (named) {
            BukkitPlugin.plugin.debug("This Equipped Monster is Named")
            val health = (health.min..health.max).rpgRandom().toDouble()
            this@toEquipped.getAttribute(Attribute.GENERIC_MAX_HEALTH)?.baseValue = health
            this@toEquipped.health = health
            this@toEquipped.getAttribute(Attribute.ZOMBIE_SPAWN_REINFORCEMENTS)?.baseValue = Random.nextDouble().coerceAtLeast(spawn_reinforcement.min).coerceAtMost(spawn_reinforcement.max)
            this@toEquipped.getAttribute(Attribute.GENERIC_KNOCKBACK_RESISTANCE)?.baseValue = Random.nextDouble().coerceAtLeast(knockback_resistance.min).coerceAtMost(knockback_resistance.max)
            this@toEquipped.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED)?.baseValue = Random.nextDouble().coerceAtLeast(movement_speed.min).coerceAtMost(movement_speed.max)
            this@toEquipped.getAttribute(Attribute.GENERIC_ARMOR)?.baseValue = (armor.min..armor.max).rpgRandom().toDouble()
            this@toEquipped.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE)?.baseValue = (atk_dmg.min..atk_dmg.max).rpgRandom().toDouble()
            this@toEquipped.isCustomNameVisible = true
            this@toEquipped.customName = ELRpgItem.elConfig.named_boss_list.random().translateColorCode()

            BukkitPlugin.plugin.debug("Named Monster ${this@toEquipped.customName}: ")
            BukkitPlugin.plugin.debug("MAX_HEALTH: ${this@toEquipped.getAttribute(Attribute.GENERIC_MAX_HEALTH)?.baseValue}")
            BukkitPlugin.plugin.debug("ZOMBIE_SPAWN_REINFORCEMENTS: ${this@toEquipped.getAttribute(Attribute.ZOMBIE_SPAWN_REINFORCEMENTS)?.baseValue}")
            BukkitPlugin.plugin.debug("KNOCKBACK_RESISTANCE: ${this@toEquipped.getAttribute(Attribute.GENERIC_KNOCKBACK_RESISTANCE)?.baseValue}")
            BukkitPlugin.plugin.debug("MOVEMENT_SPEED: ${this@toEquipped.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED)?.baseValue}")
            BukkitPlugin.plugin.debug("ARMOR: ${this@toEquipped.getAttribute(Attribute.GENERIC_ARMOR)?.baseValue}")
            BukkitPlugin.plugin.debug("ATTACK_DAMAGE: ${this@toEquipped.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE)?.baseValue}")
        }

        eq.boots = ELRPGManager.generateItem((0..2).random(), (0..2).random(), qualities.filter { m -> m.equipmentSlot == EquipmentSlot.FEET })
        eq.bootsDropChance = Random.nextFloat()
        eq.chestplate = ELRPGManager.generateItem((0..2).random(), (0..2).random(), qualities.filter { m -> m.equipmentSlot == EquipmentSlot.CHEST })
        eq.chestplateDropChance = Random.nextFloat()
        eq.helmet = ELRPGManager.generateItem((0..2).random(), (0..2).random(), qualities.filter { m -> m.equipmentSlot == EquipmentSlot.HEAD })
        eq.helmetDropChance = Random.nextFloat()
        eq.leggings = ELRPGManager.generateItem((0..2).random(), (0..2).random(), qualities.filter { m -> m.equipmentSlot == EquipmentSlot.LEGS })
        eq.leggingsDropChance = Random.nextFloat()
        eq.setItemInMainHand(ELRPGManager.generateItem((0..2).random(), (0..2).random(), qualities.filter { m -> m.equipmentSlot == EquipmentSlot.HAND && m in ELRPGManager.Item.WEAPONS }))
        eq.itemInMainHandDropChance = Random.nextFloat()
        return true
    }
}

fun ItemStat.all(type: Type): List<ItemStat> {
    return MMOItems.plugin.stats.all.filter { type.canHave(this) }
}

fun Material.defaultAttributeUUID(attr: Attribute): UUID {
    val uid = when {
        this in ELRPGManager.Item.WEAPONS -> {
            when (attr) {
                Attribute.GENERIC_ATTACK_DAMAGE -> "CB3F55D3-645C-4F38-A497-9C13A33DB5CF"
                Attribute.GENERIC_ATTACK_SPEED -> "FA233E1C-4180-4865-B01B-BCCE9785ACA3"
                else -> null
            }
        }
        this in ELRPGManager.Item.TOOLS -> {
            when (attr) {
                Attribute.GENERIC_ATTACK_DAMAGE -> "CB3F55D3-645C-4F38-A497-9C13A33DB5CF"
                Attribute.GENERIC_ATTACK_SPEED -> "FA233E1C-4180-4865-B01B-BCCE9785ACA3"
                else -> null
            }
        }
        attr == Attribute.GENERIC_ARMOR || attr == Attribute.GENERIC_ARMOR_TOUGHNESS -> {
            when (equipmentSlot) {
                EquipmentSlot.FEET -> "845DB27C-C624-495F-8C9F-6020A9A58B6B"
                EquipmentSlot.LEGS -> "D8499B04-0E66-4726-AB29-64469D734E0D"
                EquipmentSlot.CHEST -> "9F3D476D-C118-4544-8365-64846904B48E"
                EquipmentSlot.HEAD -> "2AD3F246-FEE1-4E67-B886-69FD380BB150"
                else -> null
            }
        }
        else -> null
    }
    return uid?.let { UUID.fromString(it.toLowerCase()) } ?: UUID.randomUUID()
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

