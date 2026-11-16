package com.bbenterprise.command.admin

import com.bbenterprise.BbEnterprise
import com.bbenterprise.cache.CompanyCache
import com.bbenterprise.cache.PlayerProfessionCache
import com.bbenterprise.database.repository.CompanyMembersRepository
import com.bbenterprise.database.repository.CompanyRepository
import com.bbenterprise.database.repository.RankRepository
import com.bbenterprise.service.AccessLevel
import com.bbenterprise.service.PermissionService
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

// Commande pour ajouter un membre à une entreprise
class AddCompanyMemberCommand(
    private val plugin: BbEnterprise,
    private val playerProfessionCache: PlayerProfessionCache,
    private val companyCache: CompanyCache
    ) : SubCommand {

    private val companyRepo = CompanyRepository(plugin.databaseManager)
    private val rankRepo = RankRepository(plugin.databaseManager)
    private val companyMemberRepo = CompanyMembersRepository(plugin.databaseManager)
    private val permissionService = PermissionService(CompanyMembersRepository(plugin.databaseManager))


    override fun execute(sender: CommandSender, args: List<String>): Boolean {
        if (sender !is Player) {
            sender.sendMessage("§cCette commande doit être exécutée en jeu.")
            return true
        }
        if (args.size < 3) {
            sender.sendMessage("§cUsage : /company addmember <player> <company> <rank>")
            return true
        }
        
        val playerName = args[0]
        val companyName = args[1]
        val rankCode = args[2]
        val target = Bukkit.getPlayer(playerName)
        
        if (target == null){
            sender.sendMessage("§cCe joueur n'existe pas.")
            return true
        }
        
        Bukkit.getScheduler().runTaskAsynchronously(plugin, Runnable {
            val company = companyRepo.getCompanyByName(companyName)
            val hasAccess = permissionService.hasAccess(sender, company, AccessLevel.ADJOINT)
            val rank = company?.let {rankRepo.getRankByProfessionCodes(it, rankCode)}
            val alreadyMember = company?.let {c ->
                companyMemberRepo.getMembersInCompany(c).any { it.playerUuid == target.uniqueId.toString() }
            } ?: false
            
            var cacheUpdate: (() -> Unit)? = null
            
            val message = when {
                !hasAccess -> "§cVous n'avez pas le droit d'utiliser cette commande."
                company == null -> "§cCette entreprise n'existe pas."
                rank == null -> "§cPas de rang de ce nom trouvé."
                alreadyMember -> "§e$playerName §cest déjà dans une entreprise."
                else -> {
                    companyMemberRepo.addMemberToCompany(company, rank, target)
                    cacheUpdate = {
                        playerProfessionCache.onMemberAdded(target.uniqueId.toString(), company.id, company.professionCode)
                    }
                    "§a✔ Le joueur §e$playerName §aa été ajouté à l'entreprise §e$companyName §aau rang de §e${rank.label} §a!"
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
            1 -> Bukkit.getOnlinePlayers().map { it.name }.filter { it.startsWith(args[0], ignoreCase = true) }
            2 -> companyCache.companyNames(args[1])
            3 -> {
                val professionCode = companyCache.professionCodeOf(args[1]) ?: return emptyList()
                companyCache.ranksFor(professionCode, args[2])
            }
            else -> emptyList()
        }
    }
}

//Commande pour supprimer un membre d'une entreprise
class RemoveCompanyMemberCommand(
    private val plugin: BbEnterprise,
    private val playerProfessionCache: PlayerProfessionCache,
    private val companyCache: CompanyCache
    ) : SubCommand {
    
    private val companyMemberRepo = CompanyMembersRepository(plugin.databaseManager)
    private val companyRepo = CompanyRepository(plugin.databaseManager)
    private val permissionService = PermissionService(CompanyMembersRepository(plugin.databaseManager))

    override fun execute(sender: CommandSender, args: List<String>): Boolean {
        if (sender !is Player) {
            sender.sendMessage("§cCette commande doit être exécutée en jeu.")
            return true
        }
        
        if (args.size < 2) {
            sender.sendMessage("§cUsage : /company removemember <player> <company>")
            return true
        }
        
        val playerName = args[0]
        val companyName = args[1]
        val target = Bukkit.getPlayer(playerName)
        
        if (target == null){
            sender.sendMessage("§cCe joueur n'existe pas.")
            return true
        }
        
        Bukkit.getScheduler().runTaskAsynchronously(plugin, Runnable {
            val company = companyRepo.getCompanyByName(companyName)
            val hasAccess = permissionService.hasAccess(sender, company, AccessLevel.ADJOINT)
            val isMember = company?.let {companyMemberRepo.getCompaniesToMember(target).any {it.companyId == company.id}   
            } ?: false
            
            var cacheUpdate: (() -> Unit)? = null
            
            val message = when {
                !hasAccess -> "§cVous n'avez pas le droit d'utiliser cette commande."
                company == null -> "§cCette entreprise n'existe pas."
                !isMember -> "§e$playerName §cne fait pas partie de cette entreprise."
                else -> {
                    companyMemberRepo.deleteMemberInCompany(target, company)
                    cacheUpdate = {
                        playerProfessionCache.onMemberRemoved(target.uniqueId.toString(), company.id)
                    }
                    "§a✔ Le joueur §e$playerName §ane fait plus parti de l'entreprise §e$companyName §a!"
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
            1 -> Bukkit.getOnlinePlayers().map { it.name }.filter { it.startsWith(args[0], ignoreCase = true) }
            2 -> companyCache.companyNames(args[1])
            else -> emptyList()
        }
    }
}

class UpdateRankMemberCommand(
    private val plugin: BbEnterprise,
    private val companyCache: CompanyCache
    ): SubCommand {
    
    private val companyRepo = CompanyRepository(plugin.databaseManager)
    private val rankRepo = RankRepository(plugin.databaseManager)
    private val companyMemberRepo = CompanyMembersRepository(plugin.databaseManager)
    private val permissionService = PermissionService(CompanyMembersRepository(plugin.databaseManager))
    
    override fun execute(sender: CommandSender, args: List<String>): Boolean {
        if (sender !is Player) {
            sender.sendMessage("§cCette commande doit être exécutée en jeu.")
            return true
        }
        
        if (args.size < 3) {
            sender.sendMessage("§cUsage : /company setrankmember <player> <company> <rank>")
            return true
        }
        
        val playerName = args[0]
        val companyName = args[1]
        val rankCode = args[2]
        val target = Bukkit.getPlayer(playerName)
        
        if (target == null){
            sender.sendMessage("§cCe joueur n'existe pas.")
            return true
        }
        
        Bukkit.getScheduler().runTaskAsynchronously(plugin, Runnable {
            val company = companyRepo.getCompanyByName(companyName)
            val hasAccess = permissionService.hasAccess(sender, company, AccessLevel.CHEF)
            val isMember = company?.let {companyMemberRepo.getCompaniesToMember(target).any {it.companyId == company.id}
            } ?: false
            val rank = company?.let {rankRepo.getRankByProfessionCodes(company, rankCode)}
            
            val message = when {
                !hasAccess -> "§cVous n'avez pas le droit d'utiliser cette commande."
                company == null -> "§cCette entreprise n'existe pas."
                !isMember -> "§e$playerName §cne fait pas partie de cette entreprise."
                rank == null -> "§cPas de rang de ce nom trouvé."
                else -> {
                    companyMemberRepo.updateMemberRankInCompany(target, company, rank)
                    "§a✔ Le rang de §e$playerName §adans §e$companyName §aest maintenant §e${rank.label} §a!"
                }
            }
            Bukkit.getScheduler().runTask(plugin, Runnable {
                sender.sendMessage(message)
            })
        })
    
        return true
    }
    
    override fun tabComplete(sender: CommandSender, args: List<String>): List<String> {
        return when (args.size) {
            1 -> Bukkit.getOnlinePlayers().map { it.name }.filter { it.startsWith(args[0], ignoreCase = true) }
            2 -> companyCache.companyNames(args[1])
            3 -> {
                val professionCode = companyCache.professionCodeOf(args[1]) ?: return emptyList()
                companyCache.ranksFor(professionCode, args[2])
            }
            else -> emptyList()
        }
    }
}
