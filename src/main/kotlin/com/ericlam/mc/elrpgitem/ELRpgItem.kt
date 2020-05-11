package com.ericlam.mc.elrpgitem

import com.ericlam.mc.kotlib.KotLib
import com.ericlam.mc.kotlib.bukkit.BukkitPlugin
import org.bukkit.entity.Player
import org.bukkit.event.enchantment.EnchantItemEvent
import org.bukkit.event.entity.EntityDeathEvent
import xuan.cat.XuanCatAPI.NBT

class ELRpgItem : BukkitPlugin() {

    companion object {
        lateinit var elConfig: ELConfig
    }

    override fun enable() {
        val manager = KotLib.getConfigFactory(this).register(ELConfig::class).dump()
        elConfig = manager.getConfig(ELConfig::class)

        listen<EntityDeathEvent> {
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