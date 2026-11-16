package com.bbcompany.command.admin

import com.bbcompany.BbCompany
import com.bbcompany.cache.*
import com.bbcompany.database.repository.CompanyMembersRepository
import com.bbcompany.database.repository.RestrictedCraftRepository
import com.bbcompany.database.repository.RestrictedDropRepository
import com.bbcompany.database.repository.RestrictedSmeltRepository
import com.bbcompany.service.AccessLevel
import com.bbcompany.service.PermissionService
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.command.CommandSender
import org.bukkit.entity.EntityType
import org.bukkit.entity.Player

class RefreshCacheCommand(
    private val plugin: BbCompany,
    private val restrictedCraftCache: RestrictedCraftCache,
    private val playerProfessionCache: PlayerProfessionCache,
    private val companyCache: CompanyCache
    ) : SubCommand {
    
    private val permissionService = PermissionService(CompanyMembersRepository(plugin.databaseManager))

    override fun execute(sender: CommandSender, args: List<String>): Boolean {
        if (sender !is Player) {
            sender.sendMessage("§cCette commande doit être exécutée en jeu.")
            return true
        }
        if (args.isNotEmpty()) {
            sender.sendMessage("§cUsage : /company refreshcraftcache")
            return true
        }
        Bukkit.getScheduler().runTaskAsynchronously(plugin, Runnable {
            val hasAccess = permissionService.hasAccess(sender, null, AccessLevel.ADMIN)
            val message = if (!hasAccess) {
                "§cVous n'avez pas le droit d'utiliser cette commande."
            } else {
                restrictedCraftCache.load()
                playerProfessionCache.load()
                companyCache.load()
                "§a✔ Cache des crafts restreints rechargé depuis la base."
            }
            Bukkit.getScheduler().runTask(plugin, Runnable {
                sender.sendMessage(message)
            })
        })
        return true
    }
}

class AddRestrictedCraftItemCommand(
    private val plugin: BbCompany,
    private val restrictedCraftCache: RestrictedCraftCache,
    private val companyCache: CompanyCache
    ): SubCommand {
    
    private val restrictedCraftRepo = RestrictedCraftRepository(plugin.databaseManager)
    private val permissionService = PermissionService(CompanyMembersRepository(plugin.databaseManager))
    
    override fun execute(sender: CommandSender, args: List<String>): Boolean {
        if (sender !is Player) {
            sender.sendMessage("§cCette commande doit être exécutée en jeu.")
            return true
        }
        if (args.isEmpty()) {
            sender.sendMessage("§cUsage : /company addrestrictedcraft <profession>")
            return true
        }
        
        val inHandItem = sender.itemInHand
        if (inHandItem.type == Material.AIR) {
            sender.sendMessage("§cTu dois tenir un item en main.")
            return true
        }
        
        val professionCode = args[0]
        
        Bukkit.getScheduler().runTaskAsynchronously(plugin, Runnable {
            val hasAccess = permissionService.hasAccess(sender, null, AccessLevel.ADMIN)
            val isAlreadyExists = restrictedCraftRepo.isAlreadyExists(inHandItem, professionCode)
            
            var cacheUpdate: (() -> Unit)? = null
            
            val message = when {
                !hasAccess -> "§cVous n'avez pas le droit d'utiliser cette commande."
                isAlreadyExists -> "§cCet item est déjà autorisé pour ce métier."
                else -> {
                    restrictedCraftRepo.addRestrictedCraft(inHandItem, professionCode)
                    val itemKey = inHandItem.type.key.toString()
                    cacheUpdate = { restrictedCraftCache.onRestrictedCraftAdded(itemKey, professionCode) }
                    "§a✔ L'item §e${inHandItem.type.key} §aa bien été ajouté pour §e$professionCode."
                }
            }
            Bukkit.getScheduler().runTask(plugin, Runnable {
                cacheUpdate?.invoke()
                sender.sendMessage(message)
            })
        })
        
        return true
    }
    
    override fun tabComplete(sender: CommandSender, args: List<String>): List<String> {
        return if (args.size == 1) companyCache.professionCodes(args[0]) else emptyList()
    }
}


class AddRestrictedDropItemCommand(
    private val plugin: BbCompany,
    private val restrictedDropCache: RestrictedDropCache,
    private val companyCache: CompanyCache
): SubCommand {
    
    private val restrictedDropRepo = RestrictedDropRepository(plugin.databaseManager)
    private val permissionService = PermissionService(CompanyMembersRepository(plugin.databaseManager))
    
    override fun execute(sender: CommandSender, args: List<String>): Boolean {
        if (sender !is Player) {
            sender.sendMessage("§cCette commande doit être exécutée en jeu.")
            return true
        }
        if (args.isEmpty()) {
            sender.sendMessage("§cUsage : /company addrestricteddrop <entity> <profession>")
            return true
        }
        
        val entityType = try {
            EntityType.valueOf(args[0].uppercase())
        } catch (e: IllegalArgumentException) {
            sender.sendMessage("§cEntité inconnue : ${args[0]}")
            return true
        }
        
        val inHandItem = sender.itemInHand
        if (inHandItem.type == Material.AIR) {
            sender.sendMessage("§cTu dois tenir un item en main.")
            return true
        }
        
        val professionCode = args[1]
        
        Bukkit.getScheduler().runTaskAsynchronously(plugin, Runnable {
            val hasAccess = permissionService.hasAccess(sender, null, AccessLevel.ADMIN)
            val isAlreadyExists = restrictedDropRepo.isAlreadyExists(entityType.name, inHandItem, professionCode)

            var cacheUpdate: (() -> Unit)? = null
            
            val message = when {
                !hasAccess -> "§cVous n'avez pas le droit d'utiliser cette commande."
                isAlreadyExists -> "§cCet item est déjà autorisé pour ce métier."
                else -> {
                    restrictedDropRepo.addRestrictedDrop(entityType.name, inHandItem, professionCode)
                    val itemKey = inHandItem.type.key.toString()
                    cacheUpdate = { restrictedDropCache.onRestrictedDropAdded(entityType.name,itemKey, professionCode) }
                    "§a✔ L'item §e${inHandItem.type.key} §adu mob §e${entityType.name} §aa bien été ajouté pour §e$professionCode."
                }
            }
            Bukkit.getScheduler().runTask(plugin, Runnable {
                cacheUpdate?.invoke()
                sender.sendMessage(message)
            })

        })
        
        return true
    }
    
    override fun tabComplete(sender: CommandSender, args: List<String>): List<String> {
        return when (args.size) {
            1 -> EntityType.entries.filter { it.isAlive }.map { it.name }.filter { it.startsWith(args[0].uppercase()) }
            2 -> companyCache.professionCodes(args[1])
            else -> emptyList()
        }
    }
}

class AddRestrictedSmeltItemCommand(
    private val plugin: BbCompany,
    private val restrictedSmeltCache: RestrictedSmeltCache,
    private val companyCache: CompanyCache
    ): SubCommand {
    
    private val restrictedSmeltRepo = RestrictedSmeltRepository(plugin.databaseManager)
    private val permissionService = PermissionService(CompanyMembersRepository(plugin.databaseManager))
    
    override fun execute(sender: CommandSender, args: List<String>): Boolean {
        if (sender !is Player) {
            sender.sendMessage("§cCette commande doit être exécutée en jeu.")
            return true
        }
        if (args.isEmpty()) {
            sender.sendMessage("§cUsage : /company addrestrictedsmelt <profession>")
            return true
        }
        
        val inHandItem = sender.itemInHand
        if (inHandItem.type == Material.AIR) {
            sender.sendMessage("§cTu dois tenir un item en main.")
            return true
        }
        
        val professionCode = args[0]
        
        Bukkit.getScheduler().runTaskAsynchronously(plugin, Runnable {
            val hasAccess = permissionService.hasAccess(sender, null, AccessLevel.ADMIN)
            val isAlreadyExists = restrictedSmeltRepo.isAlreadyExists(inHandItem, professionCode)
            
            var cacheUpdate: (() -> Unit)? = null
            
            val message = when {
                !hasAccess -> "§cVous n'avez pas le droit d'utiliser cette commande."
                isAlreadyExists -> "§cCet item est déjà autorisé pour ce métier."
                else -> {
                    restrictedSmeltRepo.addRestrictedSmelt(inHandItem, professionCode)
                    val itemKey = inHandItem.type.key.toString()
                    cacheUpdate = { restrictedSmeltCache.onRestrictedSmeltAdded(itemKey, professionCode) }
                    "§a✔ L'item §e${inHandItem.type.key} §aa bien été ajouté pour §e$professionCode."
                }
            }
            Bukkit.getScheduler().runTask(plugin, Runnable {
                cacheUpdate?.invoke()
                sender.sendMessage(message)
            })
        })
        
        return true
    }
    
    override fun tabComplete(sender: CommandSender, args: List<String>): List<String> {
        return if (args.size == 1) companyCache.professionCodes(args[0]) else emptyList()
    }
}

