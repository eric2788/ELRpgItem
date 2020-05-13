package com.ericlam.mc.elrpgitem

import com.ericlam.mc.kotlib.config.Resource
import com.ericlam.mc.kotlib.config.dto.ConfigFile
import org.bukkit.attribute.Attribute
import org.bukkit.entity.EntityType

@Resource(locate = "config.yml")
data class ELConfig(
        val attributes: Map<Attribute, AttributeMeta>,
        val drops: Quality,
        val random: RPGRandom,
        val named_boss_list: List<String>,
        val disabled_world: List<String>,
        val named_boss_settings: BossSettings
) : ConfigFile(){

    data class Quality(
            val n: Meta,
            val r: Meta,
            val sr: Meta,
            val ssr: Meta
    ){
        val ALL = n.list + r.list + sr.list

        data class Meta(
                val enchants: Int,
                val attributes: Int,
                val list: List<EntityType> = listOf()
        )
    }

    data class AttributeMeta(
            val display: String,
            val maxPercent: Int
    )

    data class RPGRandom(
            val start: Double,
            val maxDrops: Int,
            val mob_spawn: MobSpawnSettings
    )

    data class MobSpawnSettings(
            val named: Double,
            val equipped: Double
    )

    data class BossSettings(
            val money: Double,
            val health: Range<Int>,
            val follow_range: Range<Int>,
            val knockback_resistance: Range<Double>,
            val movement_speed: Range<Double>,
            val armor: Range<Int>,
            val atk_dmg: Range<Int>
    ){
        data class Range<N : Number>(
                val min: N,
                val max: N
        )
    }

}