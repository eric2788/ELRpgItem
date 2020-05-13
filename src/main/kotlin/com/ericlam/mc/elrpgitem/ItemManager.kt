package com.ericlam.mc.elrpgitem

import com.ericlam.mc.kotlib.bukkit.BukkitPlugin
import org.bukkit.Material
import org.bukkit.attribute.Attribute
import org.bukkit.attribute.AttributeModifier
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack
import xuan.cat.XuanCatAPI.NBT
import java.util.*

object ItemManager {

    object Item {

        object Tools {
            val WOOD_TOOLS: List<Material> = listOf(Material.WOODEN_AXE, Material.WOODEN_HOE, Material.WOODEN_PICKAXE, Material.WOODEN_SHOVEL)
            val GOLD_TOOLS: List<Material> = listOf(Material.GOLDEN_AXE, Material.GOLDEN_HOE, Material.GOLDEN_PICKAXE, Material.GOLDEN_SHOVEL)
            val STONE_TOOLS: List<Material> = listOf(Material.STONE_AXE, Material.STONE_HOE, Material.STONE_PICKAXE, Material.STONE_SHOVEL)
            val IRON_TOOLS: List<Material> = listOf(Material.STONE_AXE, Material.IRON_HOE, Material.IRON_PICKAXE, Material.IRON_SHOVEL)
            val DIAMOND_TOOLS: List<Material> = listOf(Material.DIAMOND_AXE, Material.DIAMOND_HOE, Material.DIAMOND_PICKAXE, Material.DIAMOND_SHOVEL)
        }


        val TOOLS: List<Material> = Tools.WOOD_TOOLS + Tools.GOLD_TOOLS + Tools.STONE_TOOLS + Tools.IRON_TOOLS + Tools.DIAMOND_TOOLS

        val SWORDS: List<Material> = listOf(Material.WOODEN_SWORD, Material.STONE_SWORD, Material.GOLDEN_SWORD, Material.IRON_SWORD, Material.DIAMOND_SWORD)

        val WEAPONS: List<Material> = SWORDS + Material.BOW

        object Armors {

            val LEATHER_ARMORS: List<Material> = listOf(Material.LEATHER_BOOTS, Material.LEATHER_CHESTPLATE, Material.LEATHER_HELMET, Material.LEATHER_LEGGINGS)
            val GOLD_ARMORS: List<Material> = listOf(Material.GOLDEN_BOOTS, Material.GOLDEN_CHESTPLATE, Material.GOLDEN_HELMET, Material.GOLDEN_LEGGINGS)
            val CHAIN_ARMORS: List<Material> = listOf(Material.CHAINMAIL_BOOTS, Material.CHAINMAIL_CHESTPLATE, Material.CHAINMAIL_HELMET, Material.CHAINMAIL_LEGGINGS)
            val IRON_ARMORS: List<Material> = listOf(Material.IRON_BOOTS, Material.IRON_CHESTPLATE, Material.IRON_HELMET, Material.IRON_LEGGINGS)
            val DIAMOND_ARMORS: List<Material> = listOf(Material.DIAMOND_BOOTS, Material.DIAMOND_CHESTPLATE, Material.DIAMOND_HELMET, Material.DIAMOND_LEGGINGS)
        }

        val ARMORS: List<Material> = Armors.LEATHER_ARMORS + Armors.GOLD_ARMORS + Armors.CHAIN_ARMORS + Armors.IRON_ARMORS + Armors.DIAMOND_ARMORS

        val ALL: List<Material> = TOOLS + WEAPONS + ARMORS

        val GOLD_ALL: List<Material> = ALL.filter { it.toString().startsWith("GOLDEN_") }
        val IRON_ALL: List<Material> = ALL.filter { it.toString().startsWith("IRON_") }
        val DIAMOND_ALL: List<Material> = ALL.filter { it.toString().startsWith("DIAMOND_") }

        val NORMAL: List<Material> = Tools.WOOD_TOOLS + Material.WOODEN_SWORD + Material.BOW + Armors.LEATHER_ARMORS + GOLD_ALL
        val RARE: List<Material> = NORMAL + Tools.STONE_TOOLS + Material.STONE_SWORD + Armors.CHAIN_ARMORS
        val SUPER_RARE: List<Material> = RARE + IRON_ALL
        val SUPERIOR_SUPER_RARE: List<Material> = SUPER_RARE + DIAMOND_ALL

        val BODY: List<EquipmentSlot> = listOf(EquipmentSlot.FEET, EquipmentSlot.LEGS, EquipmentSlot.CHEST, EquipmentSlot.HEAD)
        val HAND: List<EquipmentSlot> = listOf(EquipmentSlot.HAND, EquipmentSlot.OFF_HAND)

        var ALLOW_ATTRIBUTES: Map<Attribute, List<EquipmentSlot>> = mapOf(
                Attribute.GENERIC_ARMOR to BODY,
                Attribute.GENERIC_ARMOR_TOUGHNESS to BODY,
                Attribute.GENERIC_ATTACK_DAMAGE to HAND + BODY,
                Attribute.GENERIC_ATTACK_SPEED to HAND + BODY,
                Attribute.GENERIC_FLYING_SPEED to BODY,
                Attribute.GENERIC_FOLLOW_RANGE to BODY + HAND,
                Attribute.GENERIC_KNOCKBACK_RESISTANCE to BODY + HAND,
                Attribute.GENERIC_LUCK to HAND + BODY,
                Attribute.GENERIC_MAX_HEALTH to BODY,
                Attribute.GENERIC_MOVEMENT_SPEED to BODY + HAND,
                Attribute.HORSE_JUMP_STRENGTH to BODY,
                Attribute.ZOMBIE_SPAWN_REINFORCEMENTS to BODY
        )
    }


    fun generateWeapon(attributes: Int, enchants: Int, vararg materials: List<Material>): ItemStack {
        val ms = materials.takeIf { it.isNotEmpty() }?.toList()?.flatten() ?: Item.ALL
        val m = ms.random()
        return BukkitPlugin.plugin.itemStack(
                material = m,
                amount = 1
        ).apply {
            this.randomEnchantments(enchants)
            val meta = this.itemMeta
            m.randomAttributes(attributes).forEach { (attr, attrMeta) ->
                val percent = (1..attrMeta.maxPercent).rpgRandom().toDouble() / 100
                meta.addAttributeModifier(attr, AttributeModifier(UUID.randomUUID(), "elrpg.item.attr", percent, AttributeModifier.Operation.MULTIPLY_SCALAR_1, m.equipmentSlot))
            }
            this.itemMeta = meta
            val nbt = NBT.getItemNBT(this)
            nbt.setBoolean("rpg.item", true)
            NBT.setItemNBT(this, nbt)
        }
    }


}