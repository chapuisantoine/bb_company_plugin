package com.bbcompany.listener

import com.bbcompany.cache.PlayerProfessionCache
import org.bukkit.Material
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.EquipmentSlot


class RestrictedBlockListener(
    private val playerProfessionCache: PlayerProfessionCache
) : Listener{
    
    private val restrictedBlocks: Map<Material, Set<String>> = mapOf(
        Material.BLAST_FURNACE to setOf("FORGERON"),
        Material.ENCHANTING_TABLE   to setOf("BIBLIOTHECAIRE"),
        Material.BREWING_STAND      to setOf("ALCHIMISTE"),
        Material.CAMPFIRE           to setOf("RESTAURANT"),
        Material.SMOKER             to setOf("RESTAURANT"),
        Material.STONECUTTER        to setOf("SCULPTEUR"),
        Material.SMITHING_TABLE     to setOf("ARMURIER"),
        Material.GRINDSTONE         to setOf("FORGERON"),
        Material.CARTOGRAPHY_TABLE  to setOf("AGENCE_IMMO"),
        Material.LOOM                to setOf("PECHEUR"),
        Material.COMPOSTER          to setOf("AGRICULTEUR")
    )
    
    @EventHandler
    fun onEnchantEvent(event: PlayerInteractEvent) {
        if (event.hand != EquipmentSlot.HAND) return
        if (event.action != Action.RIGHT_CLICK_BLOCK) return
        
        val block = event.clickedBlock ?: return
        val requiredProfessions = restrictedBlocks[block.type] ?: return
        
        val player = event.player
        if (player.hasPermission("bbcompany.trade.bypass")) return
        
        val playerUuid = player.uniqueId.toString()
        val isAuthorized = requiredProfessions.any {playerProfessionCache.hasProfession(playerUuid, it)}

        if (!isAuthorized) {
            event.isCancelled = true
            player.sendMessage("§cTu n'as pas le métier requis.")
        }
    }
}