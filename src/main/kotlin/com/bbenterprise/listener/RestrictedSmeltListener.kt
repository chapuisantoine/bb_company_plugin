package com.bbenterprise.listener

import com.bbenterprise.BbEnterprise
import com.bbenterprise.cache.PlayerProfessionCache
import com.bbenterprise.cache.RestrictedSmeltCache
import org.bukkit.Bukkit
import org.bukkit.block.Furnace
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryDragEvent
import org.bukkit.event.inventory.InventoryMoveItemEvent
import org.bukkit.event.inventory.InventoryType
import org.bukkit.inventory.Inventory

class RestrictedSmeltListener(
    private val plugin: BbEnterprise,
    private val restrictedSmeltCache: RestrictedSmeltCache,
    private val playerProfessionCache: PlayerProfessionCache
): Listener {
    
    @EventHandler
    fun onHopperFeedBlock(event: InventoryMoveItemEvent) {
        if (event.destination.holder !is Furnace) return
        event.isCancelled = true        
    }
    
    @EventHandler
    fun onFurnaceClick(event: InventoryClickEvent) {
        if (event.inventory.type != InventoryType.FURNACE) return
        val player = event.whoClicked as? Player ?: return
        checkNextTick(player, event.inventory)
    }

    @EventHandler
    fun onFurnaceDrag(event: InventoryDragEvent) {
        if (event.inventory.type != InventoryType.FURNACE) return
        val player = event.whoClicked as? Player ?: return
        checkNextTick(player, event.inventory)
    }

    private fun checkNextTick(player: Player, inventory: Inventory) {
        Bukkit.getScheduler().runTask(plugin, Runnable {
            val item = inventory.getItem(0) ?: return@Runnable
            val itemKey = item.type.key.toString()

            val requiredProfessions = restrictedSmeltCache.authorizedProfessionsFor(itemKey)
            if (requiredProfessions.isEmpty()) return@Runnable
            if (player.hasPermission("bbenterprise.smelt.bypass")) return@Runnable

            val playerUuid = player.uniqueId.toString()
            val isAuthorized = requiredProfessions.any { playerProfessionCache.hasProfession(playerUuid, it) }

            if (!isAuthorized) {
                inventory.setItem(0, null)
                player.inventory.addItem(item)
                player.sendMessage("§cTu n'as pas le métier requis pour mettre cet item dans le four.")
            }
        })
    }
    
}