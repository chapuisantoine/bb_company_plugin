package com.bbcompany.listener

import com.bbcompany.cache.PlayerProfessionCache
import com.bbcompany.cache.RestrictedDropCache
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
        if (player.hasPermission("bbcompany.drop.bypass")) return
        
        val entityType = event.entityType.name
        val playerUuid = player.uniqueId.toString()
        
        event.drops.removeIf {drop ->
            val requiredProfessions = restrictedDropCache.authorizedProfessionsFor(entityType, drop.type.key.toString())
            requiredProfessions.isNotEmpty() &&
                requiredProfessions.none {playerProfessionCache.hasProfession(playerUuid, it)}
        }
    }
}