package com.ericlam.mc.elrpgitem

import com.ericlam.mc.kotlib.KotLib
import com.ericlam.mc.kotlib.bukkit.BukkitPlugin
import com.ericlam.mc.kotlib.command.BukkitCommand
import com.ericlam.mc.kotlib.translateColorCode
import net.milkbowl.vault.economy.Economy
import org.bukkit.Material
import org.bukkit.attribute.Attribute
import org.bukkit.attribute.AttributeModifier
import org.bukkit.entity.*
import org.bukkit.event.enchantment.EnchantItemEvent
import org.bukkit.event.entity.CreatureSpawnEvent
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityDeathEvent
import org.bukkit.event.entity.EntitySpawnEvent
import org.bukkit.event.player.PlayerChangedMainHandEvent
import org.bukkit.event.player.PlayerSwapHandItemsEvent
import org.bukkit.inventory.EquipmentSlot
import xuan.cat.XuanCatAPI.NBT
import xuan.cat.XuanCatAPI.api.event.packet.ClientHeldItemSlotPacketEvent
import java.awt.event.ItemEvent
import java.util.*
import kotlin.random.Random


class ELRpgItem : BukkitPlugin() {

    companion object {
        lateinit var elConfig: ELConfig
    }

    private lateinit var eco: Economy

    override fun enable() {
        val manager = KotLib.getConfigFactory(this).register(ELConfig::class).dump()
        elConfig = manager.getConfig(ELConfig::class)

        val rsp = server.servicesManager.getRegistration(Economy::class.java)
        eco = rsp?.provider ?: throw IllegalStateException("Cannot find an economy plugin which support vault")
        listen<EntityDeathEvent> {
            if (it.entity.world.name in elConfig.disabled_world) return@listen
            val nbt = NBT.getEntityNBT(it.entity)
            if (nbt.getBoolean("rpg.monster") && !it.entity.isCustomNameVisible) return@listen
            val drops = (0..elConfig.random.maxDrops).rpgRandom()
            with(elConfig.drops) {
                val meta = when {
                    n.list.contains(it.entityType) -> ItemManager.Item.NORMAL to n.enchants to n.attributes
                    r.list.contains(it.entityType) -> ItemManager.Item.RARE to r.enchants to r.attributes
                    sr.list.contains(it.entityType) -> ItemManager.Item.SUPER_RARE to sr.enchants to sr.attributes
                    nbt.getBoolean("rpg.monster.named") -> {
                        it.entity.killer?.let { p ->
                            val money = elConfig.named_boss_settings.money
                            eco.depositPlayer(p, money)
                            p.sendMessage("§a擊殺具名怪物被獎勵金錢 $$money")
                        }

                        ItemManager.Item.SUPERIOR_SUPER_RARE to ssr.enchants to ssr.attributes
                    }
                    else -> return@listen
                }
                for (i in 1..drops) {
                    val item = ItemManager.generateWeapon(meta.second, meta.first.second, meta.first.first)
                    it.drops.add(item)
                }
            }
        }

        listen<EnchantItemEvent> {
            if (it.enchanter.world.name in elConfig.disabled_world) return@listen
            val nbt = NBT.getItemNBT(it.item)
            if (nbt.getBoolean("rpg.item")) {
                it.isCancelled = true
                it.enchanter.sendMessage("§c你無法額外附魔RPG物品")
            }
        }

        listen<CreatureSpawnEvent> {
            if (it.entity.world.name in elConfig.disabled_world) return@listen
            if (it.entity !is Monster) return@listen
            val random = Random.nextDouble()
            if (random > elConfig.random.mob_spawn.equipped) return@listen
            debug("Equipped Monster Spawned.")
            val eq = (it.entity as Monster).equipment ?: return@listen
            with(elConfig.named_boss_settings) {
                val named = random < elConfig.random.mob_spawn.named
                val qualities = if (named) ItemManager.Item.SUPERIOR_SUPER_RARE else ItemManager.Item.SUPER_RARE
                val nbt = NBT.getEntityNBT(it.entity)
                nbt.setBoolean("rpg.monster", true)
                nbt.setBoolean("rpg.monster.named", named)
                NBT.setEntityNBT(it.entity, nbt)
                if (named){
                    debug("This Equipped Monster is Named")
                    val mon = (it.entity as Monster)
                    val health = (health.min..health.max).rpgRandom().toDouble()
                    mon.getAttribute(Attribute.GENERIC_MAX_HEALTH)?.baseValue = health
                    it.entity.health = health
                    mon.getAttribute(Attribute.ZOMBIE_SPAWN_REINFORCEMENTS)?.baseValue = Random.nextDouble().coerceAtLeast(spawn_reinforcement.min).coerceAtMost(spawn_reinforcement.max)
                    mon.getAttribute(Attribute.GENERIC_KNOCKBACK_RESISTANCE)?.baseValue = Random.nextDouble().coerceAtLeast(knockback_resistance.min).coerceAtMost(knockback_resistance.max)
                    mon.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED)?.baseValue = Random.nextDouble().coerceAtLeast(movement_speed.min).coerceAtMost(movement_speed.max)
                    mon.getAttribute(Attribute.GENERIC_ARMOR)?.baseValue = (armor.min..armor.max).rpgRandom().toDouble()
                    mon.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE)?.baseValue = (atk_dmg.min..atk_dmg.max).rpgRandom().toDouble()
                    mon.isCustomNameVisible = true
                    mon.customName = elConfig.named_boss_list.random().translateColorCode()

                    debug("Named Monster ${mon.customName}: ")
                    debug("MAX_HEALTH: ${mon.getAttribute(Attribute.GENERIC_MAX_HEALTH)?.baseValue}")
                    debug("ZOMBIE_SPAWN_REINFORCEMENTS: ${mon.getAttribute(Attribute.ZOMBIE_SPAWN_REINFORCEMENTS)?.baseValue}")
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
                eq.setItemInMainHand(ItemManager.generateWeapon((0..2).random(), (0..2).random(), qualities.filter { m -> m.equipmentSlot == EquipmentSlot.HAND && m in ItemManager.Item.WEAPONS }))
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

            debug("${damagerEntity.name} -> ${it.entity.name}: -${it.finalDamage} HP || ${it.entity.name} HP is now ${(it.entity as? LivingEntity)?.health ?: "NULL"}")
        }

        val cmd  = object : BukkitCommand(
                name = "elrpg",
                description = "elrpg 指令",
                permission = "elrpg.admin",
                child = arrayOf(
                        BukkitCommand(
                                name = "generate",
                                description = "生成隨機物品"
                        ){ sender, _ ->
                            val player = sender as? Player ?: let {
                                sender.sendMessage("you are not player")
                                return@BukkitCommand
                            }
                            player.sendMessage("§a隨機物品已生成")
                            player.inventory.addItem(ItemManager.generateWeapon(2, 2))
                        },
                        BukkitCommand(
                                name = "reload",
                                description = "重載指令"
                        ){ sender , _ ->
                            elConfig.reload()
                            sender.sendMessage("§a重載成功")
                        }
                )
        ){}

        registerCmd(cmd)

        info("ELRpgItem enabled, fuck you")
    }

}