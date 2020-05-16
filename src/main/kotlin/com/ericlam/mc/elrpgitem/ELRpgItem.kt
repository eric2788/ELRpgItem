package com.ericlam.mc.elrpgitem

import com.ericlam.mc.kotlib.KotLib
import com.ericlam.mc.kotlib.bukkit.BukkitPlugin
import com.ericlam.mc.kotlib.command.BukkitCommand
import net.Indyuce.mmoitems.MMOItems
import net.Indyuce.mmoitems.api.droptable.DropTable
import net.Indyuce.mmoitems.manager.DropTableManager
import net.milkbowl.vault.economy.Economy
import org.bukkit.entity.*
import org.bukkit.event.HandlerList
import org.bukkit.event.enchantment.EnchantItemEvent
import org.bukkit.event.entity.CreatureSpawnEvent
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityDeathEvent
import org.bukkit.inventory.ItemStack
import xuan.cat.XuanCatAPI.NBT
import kotlin.random.Random


class ELRpgItem : BukkitPlugin() {

    companion object {
        lateinit var elConfig: ELConfig
    }

    private lateinit var eco: Economy

    override fun enable() {
        val manager = KotLib.getConfigFactory(this).register(ELConfig::class).dump()
        elConfig = manager.getConfig(ELConfig::class)
        val mmoEnabled = server.pluginManager.getPlugin("MMOItems") != null
        debug("MMOItems hooking is now $mmoEnabled")
        if (mmoEnabled){
            info("MMOItems hooked, unregistered mmo mob drop listener and using elrpg own drop listener.")
            HandlerList.unregisterAll(MMOItems.plugin.dropTables)
        }
        val rsp = server.servicesManager.getRegistration(Economy::class.java)
        eco = rsp?.provider ?: throw IllegalStateException("Cannot find an economy plugin which support vault")
        listen<EntityDeathEvent> {
            if (it.entity.world.name in elConfig.disabled_world) return@listen
            val nbt = NBT.getEntityNBT(it.entity)
            val drops = (0..elConfig.random.maxDrops).rpgRandom()
            with(elConfig.drops) {
                val meta = when {
                    n.list.contains(it.entityType) -> ELRPGManager.Item.NORMAL to n.enchants to n.attributes
                    r.list.contains(it.entityType) -> ELRPGManager.Item.RARE to r.enchants to r.attributes
                    sr.list.contains(it.entityType) -> ELRPGManager.Item.SUPER_RARE to sr.enchants to sr.attributes
                    nbt.getBoolean("rpg.monster.named") -> {
                        val damager = (it.entity.lastDamageCause as? EntityDamageByEntityEvent)?.damager
                        when(damager){
                            is Projectile -> damager.shooter as Player
                            is TNTPrimed -> damager.source as Player
                            is Player -> damager
                            else -> null
                        }?.let { p ->
                            val money = elConfig.named_boss_settings.money
                            eco.depositPlayer(p, money)
                            p.sendMessage("§a擊殺具名怪物被獎勵金錢 $$money")
                        }
                        if (mmoEnabled) {
                            debug("named mob killed, drop MMOItems")
                            MMOItems.plugin.dropTables.a(it)
                        }
                        ELRPGManager.Item.SUPERIOR_SUPER_RARE to ssr.enchants to ssr.attributes
                    }
                    else -> return@listen
                }
                for (i in 1..drops) {
                    val item = ELRPGManager.generateItem(meta.second, meta.first.second, meta.first.first)
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
            (it.entity as Monster).toEquipped(random < elConfig.random.mob_spawn.named)
        }

        listen<EntityDamageByEntityEvent> {
            val damagerEntity = when (it.damager) {
                is Player -> it.damager as? Player
                is Projectile -> (it.damager as Projectile).shooter as? LivingEntity
                is TNTPrimed -> (it.damager as TNTPrimed).source as? LivingEntity
                is ThrownPotion -> (it.damager as ThrownPotion).shooter as? LivingEntity
                else -> return@listen
            } ?: return@listen

            debug("${damagerEntity.name} -> ${it.entity.name}: -${it.finalDamage} HP || ${it.entity.name} HP is now ${(it.entity as? LivingEntity)?.health ?: "NULL"}")
        }

        val cmd = object : BukkitCommand(
                name = "elrpg",
                description = "elrpg 指令",
                permission = "elrpg.admin",
                child = arrayOf(
                        BukkitCommand(
                                name = "generate",
                                description = "生成隨機物品"
                        ) { sender, _ ->
                            val player = sender as? Player ?: let {
                                sender.sendMessage("you are not player")
                                return@BukkitCommand
                            }
                            player.sendMessage("§a隨機物品已生成")
                            player.inventory.addItem(ELRPGManager.generateItem(2, 2))
                        },
                        BukkitCommand(
                                name = "reload",
                                description = "重載指令"
                        ) { sender, _ ->
                            elConfig.reload()
                            sender.sendMessage("§a重載成功")
                        },
                        BukkitCommand(
                                name = "named",
                                description = "指定五格內指向的怪物成為具名"
                        ) { sender, _ ->
                            val player = sender as? Player ?: let {
                                sender.sendMessage("you are not player")
                                return@BukkitCommand
                            }
                            val en = player.getTargetEntity(5)
                            val b = (en as? Monster)?.toEquipped(true) ?: let {
                                player.sendMessage("§c沒有怪物被指定")
                                return@BukkitCommand
                            }
                            player.sendMessage("§e指定 ${if (b) "§a成功" else "§c失敗"}")
                        }
                )
        ) {}

        registerCmd(cmd)

        info("ELRpgItem enabled, fuck you")
    }

}