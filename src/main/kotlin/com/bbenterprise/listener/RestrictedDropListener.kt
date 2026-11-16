package com.bbenterprise.listener

import com.bbenterprise.cache.PlayerProfessionCache
import com.bbenterprise.cache.RestrictedDropCache
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDeathEvent

class RestrictedDropListener (
    private val restrictedDropCache: RestrictedDropCache,
    private val playerProfessionCache: PlayerProfessionCache
): Listener {
    
    @EventHandler
    fun onEntityDeath(event: EntityDeathEvent) {
        val player = event.entity.killer ?: return
        if (player.hasPermission("bbenterprise.drop.bypass")) return
        
        val entityType = event.entityType.name
        val playerUuid = player.uniqueId.toString()
        
        event.drops.removeIf {drop ->
            val requiredProfessions = restrictedDropCache.authorizedProfessionsFor(entityType, drop.type.key.toString())
            requiredProfessions.isNotEmpty() &&
                requiredProfessions.none {playerProfessionCache.hasProfession(playerUuid, it)}
        }
    }
}