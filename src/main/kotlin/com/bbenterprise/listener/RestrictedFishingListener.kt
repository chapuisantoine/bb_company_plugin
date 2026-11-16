package com.bbenterprise.listener

import com.bbenterprise.cache.PlayerProfessionCache
import org.bukkit.Material
import org.bukkit.entity.Item
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerFishEvent

class RestrictedFishingListener(
    private val playerProfessionCache: PlayerProfessionCache
): Listener {
    
    private val fishMaterials = setOf(
        Material.COD, Material.SALMON, Material.TROPICAL_FISH, Material.PUFFERFISH
)
    
    @EventHandler
    fun onFish(event: PlayerFishEvent) {
        if (event.state != PlayerFishEvent.State.CAUGHT_FISH) return
        
        val caughtEntity = event.caught as? Item ?: return
        val player = event.player
        
        if (caughtEntity.itemStack.type !in fishMaterials) {
            event.isCancelled = true
            return
        }
        
        val playerUuid = player.uniqueId.toString()
        val isAuthorized = playerProfessionCache.hasProfession(playerUuid, "PECHEUR")
        
        if (!isAuthorized) {
            event.isCancelled = true
            player.sendMessage("§cTu n'as pas le droit de pecher.")
        } 
    }
}