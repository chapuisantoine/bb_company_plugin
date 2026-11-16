package com.bbcompany.listener

import com.bbcompany.cache.PlayerProfessionCache
import org.bukkit.entity.AbstractVillager
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerInteractEntityEvent

class RestrictedTradeListener(
    private val playerProfessionCache: PlayerProfessionCache
): Listener {
    
    private val allowedProfessions = setOf("")
    
    @EventHandler
    fun onVillagerInteract(event: PlayerInteractEntityEvent) {
        if (event.rightClicked !is AbstractVillager) return
        
        val player = event.player
        if (player.hasPermission("bbcompany.trade.bypass")) return
        
        val playerUuid = player.uniqueId.toString()
        val isAuthorized = allowedProfessions.any {playerProfessionCache.hasProfession(playerUuid, it)}
        
        if (!isAuthorized) {
            event.isCancelled = true
            player.sendMessage("§cTu n'as pas le droit de commercer avec les villageois.")
        }
    }   
}