package com.bbcompany.listener

import com.bbcompany.cache.PlayerProfessionCache
import com.bbcompany.cache.RestrictedCraftCache
import org.bukkit.Keyed
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.PrepareItemCraftEvent

class RestrictedCraftListener(
    private val restrictedCraftCache: RestrictedCraftCache,
    private val playerProfessionCache: PlayerProfessionCache
) : Listener {

    @EventHandler
    fun onPrepareCraft(event: PrepareItemCraftEvent) {
        val recipe = event.recipe as? Keyed ?: return
        val requiredProfessions = restrictedCraftCache.authorizedProfessionsFor(recipe.key.toString())
        if (requiredProfessions.isEmpty()) return
        
        val player = event.viewers.filterIsInstance<Player>().firstOrNull() ?: return
        if (player.hasPermission("bbcompany.craft.bypass")) return

        val playerUuid = player.uniqueId.toString()
        val isAuthorized = requiredProfessions.any { playerProfessionCache.hasProfession(playerUuid, it) }
        
        if (!isAuthorized) {
            event.inventory.result = null
        }
    }
}