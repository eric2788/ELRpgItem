package com.ericlam.mc.elrpgitem

import com.ericlam.mc.kotlib.KotLib
import com.ericlam.mc.kotlib.bukkit.BukkitPlugin
import com.ericlam.mc.kotlib.translateColorCode
import org.bukkit.attribute.Attribute
import org.bukkit.entity.*
import org.bukkit.event.enchantment.EnchantItemEvent
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityDeathEvent
import org.bukkit.event.entity.EntitySpawnEvent
import org.bukkit.inventory.EquipmentSlot
import xuan.cat.XuanCatAPI.NBT
import kotlin.random.Random

class ELRpgItem : BukkitPlugin() {

    companion object {
        lateinit var elConfig: ELConfig
    }

    override fun enable() {
        val manager = KotLib.getConfigFactory(this).register(ELConfig::class).dump()
        elConfig = manager.getConfig(ELConfig::class)

        listen<EntityDeathEvent> {
            val nbt = NBT.getEntityNBT(it.entity)
            if (nbt.getBoolean("rpg.monster") && !it.entity.isCustomNameVisible) return@listen
            val drops = (0..elConfig.random.maxDrops).rpgRandom()
            with(elConfig.drops) {
                val meta = when {
                    n.list.contains(it.entityType) -> ItemManager.Item.NORMAL to n.enchants to n.attributes
                    r.list.contains(it.entityType) -> ItemManager.Item.RARE to r.enchants to r.attributes
                    sr.list.contains(it.entityType) -> ItemManager.Item.SUPER_RARE to sr.enchants to sr.attributes
                    it.entity.isCustomNameVisible -> ItemManager.Item.SUPERIOR_SUPER_RARE to ssr.enchants to ssr.attributes
                    else -> return@listen
                }
                for (i in 1..drops) {
                    val item = ItemManager.generateWeapon(meta.second, meta.first.second, meta.first.first)
                    it.drops.add(item)
                }
            }
        }

        listen<EnchantItemEvent> {
            val nbt = NBT.getItemNBT(it.item)
            if (nbt.getBoolean("rpg.item")) {
                it.isCancelled = true
                it.enchanter.sendMessage("§c你無法額外附魔RPG物品")
            }
        }

        listen<EntitySpawnEvent> {
            if (it.entity !is Monster) return@listen
            val random = Random.nextDouble()
            if (random > elConfig.random.mob_spawn.equipped) return@listen
            debug("Equipped Monster Spawned.")
            val eq = (it.entity as Monster).equipment ?: return@listen
            with(elConfig.drops) {
                val named = random < elConfig.random.mob_spawn.named
                val qualities = if (named) ItemManager.Item.SUPERIOR_SUPER_RARE else ItemManager.Item.SUPER_RARE
                val nbt = NBT.getEntityNBT(it.entity)
                nbt.setBoolean("rpg.monster", true)
                nbt.setBoolean("rpg.monster.named", named)
                NBT.setEntityNBT(it.entity, nbt)
                if (named){
                    debug("This Equipped Monster is Named")
                    val mon = (it.entity as Monster)
                    mon.getAttribute(Attribute.GENERIC_MAX_HEALTH)?.baseValue = (30..100).rpgRandom().toDouble()
                    mon.getAttribute(Attribute.GENERIC_FOLLOW_RANGE)?.baseValue = (50..70).rpgRandom().toDouble()
                    mon.getAttribute(Attribute.GENERIC_KNOCKBACK_RESISTANCE)?.baseValue = Random.nextDouble()
                    mon.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED)?.baseValue = Random.nextDouble().coerceAtLeast(0.1).coerceAtMost(0.5)
                    mon.getAttribute(Attribute.GENERIC_ARMOR)?.baseValue = (1..30).rpgRandom().toDouble()
                    mon.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE)?.baseValue = (3..10).rpgRandom().toDouble()
                    mon.isCustomNameVisible = true
                    mon.customName = elConfig.named_boss_list.random().translateColorCode()

                    debug("Named Monster ${mon.customName}: ")
                    debug("MAX_HEALTH: ${mon.getAttribute(Attribute.GENERIC_MAX_HEALTH)?.baseValue}")
                    debug("FOLLOW_RANGE: ${mon.getAttribute(Attribute.GENERIC_FOLLOW_RANGE)?.baseValue}")
                    debug("KNOCKBACK_RESISTANCE: ${mon.getAttribute(Attribute.GENERIC_KNOCKBACK_RESISTANCE)?.baseValue}")
                    debug("MOVEMENT_SPEED: ${mon.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED)?.baseValue}")
                    debug("ARMOR: ${mon.getAttribute(Attribute.GENERIC_ARMOR)?.baseValue}")
                    debug("ATTACK_DAMAGE: ${mon.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE)?.baseValue}")
                }

                eq.boots = ItemManager.generateWeapon((0..2).random(), (0..2).random(), qualities.filter { m -> m.equipmentSlot == EquipmentSlot.FEET })
                eq.bootsDropChance = Random.nextFloat()
                eq.chestplate = ItemManager.generateWeapon((0..2).random(), (0..2).random(), qualities.filter { m -> m.equipmentSlot == EquipmentSlot.CHEST })
                eq.chestplateDropChance = Random.nextFloat()
                eq.helmet = ItemManager.generateWeapon((0..2).random(), (0..2).random(), qualities.filter { m -> m.equipmentSlot == EquipmentSlot.HEAD })
                eq.helmetDropChance = Random.nextFloat()
                eq.leggings = ItemManager.generateWeapon((0..2).random(), (0..2).random(), qualities.filter { m -> m.equipmentSlot == EquipmentSlot.LEGS })
                eq.leggingsDropChance = Random.nextFloat()
                eq.setItemInMainHand(ItemManager.generateWeapon((0..2).random(), (0..2).random(), qualities.filter { m -> m.equipmentSlot == EquipmentSlot.HAND }))
                eq.itemInMainHandDropChance = Random.nextFloat()

            }
        }

        listen<EntityDamageByEntityEvent> {
            val damagerEntity  = when(it.damager){
                is Player -> it.damager as? Player
                is Projectile -> (it.damager as Projectile).shooter as? LivingEntity
                is TNTPrimed -> (it.damager as TNTPrimed).source as? LivingEntity
                is ThrownPotion -> (it.damager as ThrownPotion).shooter as? LivingEntity
                else -> return@listen
            } ?: return@listen

            debug("${damagerEntity.name} -> ${it.entity.name}: ${it.finalDamage} HP")
        }

        command("generate-item", "elrpg.item") { sender, strings ->
            val player = sender as? Player ?: let {
                sender.sendMessage("you are not player")
                return@command
            }
            player.sendMessage("§a隨機物品已生成")
            player.inventory.addItem(ItemManager.generateWeapon(2, 2))
        }

        info("ELRpgItem enabled, fuck you")
    }

}