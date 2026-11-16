package com.bbcompany.command.admin

import com.bbcompany.BbCompany
import com.bbcompany.cache.CompanyCache
import com.bbcompany.cache.PlayerProfessionCache
import com.bbcompany.database.repository.*
import com.bbcompany.service.AccessLevel
import com.bbcompany.service.PermissionService
import net.md_5.bungee.api.ChatColor
import net.md_5.bungee.api.chat.ClickEvent
import net.md_5.bungee.api.chat.TextComponent
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.*

class CreateCompanyCommand(
    private val plugin: BbCompany,
    private val companyCache: CompanyCache,
    private val playerProfessionCache: PlayerProfessionCache
) : SubCommand {
    
    private val companyRepo = CompanyRepository(plugin.databaseManager)
    private val companyMembersRepo = CompanyMembersRepository(plugin.databaseManager)
    private val professionRepo = ProfessionRepository(plugin.databaseManager)
    private val rankRepo = RankRepository(plugin.databaseManager)
    private val permissionService = PermissionService(CompanyMembersRepository(plugin.databaseManager))
    
    override fun execute(sender: CommandSender, args: List<String>): Boolean {
        if (sender !is Player) {
            sender.sendMessage("§cCette commande doit être exécutée en jeu.")
            return true
        }
        
        if (args.size < 3) {
            sender.sendMessage("§cUsage : /company create <name> <profession> <owner>")
            return true
        }
        
        val name = args[0]
        val professionCode = args[1]
        val playerName = args[2]
        val target = Bukkit.getPlayer(playerName)
        
        if (target == null){
            sender.sendMessage("§cCe joueur n'existe pas.")
            return true
        }

        Bukkit.getScheduler().runTaskAsynchronously(plugin, Runnable {
            val hasAccess = permissionService.hasAccess(sender, null, AccessLevel.ADMIN)
            val profession = professionRepo.getProfessionByCode(professionCode)
            val rank = rankRepo.getRanksForProfession(professionCode).maxByOrNull { it.level }
            val alreadyExists = companyRepo.getCompanyByName(name) != null
            
            var cacheUpdate: (() -> Unit)? = null
            
            val message = when {
                !hasAccess -> "§cVous n'avez pas le droit d'utiliser cette commande."
                profession == null -> "§cCette profession n'existe pas."
                rank == null -> "§cAucun rang trouvé pour cette profession."
                alreadyExists -> "§cUne entreprise porte déjà le nom §e$name§c."
                else -> {
                    val company =
                        Company(name = name, professionCode = professionCode, ownerUuid = target.uniqueId.toString())
                    companyRepo.createCompany(company)
                    companyMembersRepo.addMemberToCompany(company, rank, target)
                    cacheUpdate = {
                        companyCache.onCompanyCreated(name, professionCode)
                        playerProfessionCache.onMemberAdded(target.uniqueId.toString(), company.id, company.professionCode) 
                        }
                    "§a✔ La companie §e${company.name} §aappartenant à $playerName a bien été créée !"
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
            1 -> emptyList()
            2 -> companyCache.professionCodes(args[1])
            3 -> Bukkit.getOnlinePlayers().map { it.name }.filter { it.startsWith(args[2], ignoreCase = true) }
            else -> emptyList()
        }
    }
}

class InfoCompanyCommand(
    private val plugin: BbCompany,
    private val companyCache: CompanyCache
    ): SubCommand {
    
    private val companyRepo = CompanyRepository(plugin.databaseManager)
    private val companyMembersRepo = CompanyMembersRepository(plugin.databaseManager)
    private val permissionService = PermissionService(CompanyMembersRepository(plugin.databaseManager))
    
    companion object {
        private val DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
    }
    
    override fun execute(sender: CommandSender, args: List<String>): Boolean {
        if (sender !is Player) {
            sender.sendMessage("§cCette commande doit être exécutée en jeu.")
            return true
        }
        
        if (args.isEmpty()) {
            sender.sendMessage("§cUsage : /company info <companyName>")
            return true
        }
        
        val companyName = args[0]
        
        Bukkit.getScheduler().runTaskAsynchronously(plugin, Runnable {
            val company = companyRepo.getCompanyByName(companyName)
            val hasAccess = permissionService.hasAccess(sender, company, AccessLevel.SALARIE)
            val nbMembers = company?.let {companyMembersRepo.getNumberMembersInCompany(company)}
            val message = when {
                !hasAccess -> "§cVous n'avez pas le droit d'utiliser cette commande."
                company == null -> "§cCette entreprise n'existe pas."
                else -> {
                    val ownerName = Bukkit.getOfflinePlayer(UUID.fromString(company.ownerUuid)).name ?: "inconnu"
                    "§aNom : §e${company.name} §a| Profession : §e${company.professionCode} §a| Propriétaire : §e$ownerName\n" +
                    "§aCréé le : §e${company.createdAt.format(DATE_FORMAT)} §a| Nombre de membres : §e$nbMembers"
                }
            }
            Bukkit.getScheduler().runTask(plugin, Runnable {
                sender.sendMessage(message)
            })
        })
        
        return true
    }
    
    override fun tabComplete(sender: CommandSender, args: List<String>): List<String> {
        return if (args.size == 1) companyCache.companyNames(args[0]) else emptyList()
    }
}

class ListCompanyCommand(private val plugin: BbCompany): SubCommand {
    
    private val companyRepo = CompanyRepository(plugin.databaseManager)
    private val permissionService = PermissionService(CompanyMembersRepository(plugin.databaseManager))
    
    override fun execute(sender: CommandSender, args: List<String>): Boolean {
        if (sender !is Player) {
            sender.sendMessage("§cCette commande doit être exécutée en jeu.")
            return true
        }
        
        if (args.isNotEmpty()) {
            sender.sendMessage("§cUsage : /company list")
            return true
        }
        
        Bukkit.getScheduler().runTaskAsynchronously(plugin, Runnable {
            val companyList = companyRepo.findAll()
            val hasAccess = permissionService.hasAccess(sender, null, AccessLevel.JOUEUR)
            val lines = when {
                !hasAccess -> listOf("§cVous n'avez pas le droit d'utiliser cette commande.")
                companyList.isEmpty() -> listOf("§cIl n'y a pas d'entreprise.")
                else -> {
                    companyList.map {company ->
                        val ownerName = Bukkit.getOfflinePlayer(UUID.fromString(company.ownerUuid)).name ?: "inconnu"
                        "§aNom : §e${company.name} §a| Profession : §e${company.professionCode} §a| Propriétaire : §e$ownerName"
                    }
                }
            }
            Bukkit.getScheduler().runTask(plugin, Runnable {
                lines.forEach{sender.sendMessage(it)}
            })
        })
        
        return true
    }
}

class ListMembersCompanyCommand(
    private val plugin: BbCompany,
    private val companyCache: CompanyCache
    ): SubCommand {
    
    private val companyRepo = CompanyRepository(plugin.databaseManager)
    private val companyMembersRepo = CompanyMembersRepository(plugin.databaseManager)
    private val permissionService = PermissionService(CompanyMembersRepository(plugin.databaseManager))

    
    override fun execute(sender: CommandSender, args: List<String>): Boolean {
        if (sender !is Player) {
            sender.sendMessage("§cCette commande doit être exécutée en jeu.")
            return true
        }
        
        if (args.isEmpty()) {
            sender.sendMessage("§cUsage : /company members <companyName>")
            return true
        }
        
        val companyName = args[0]
        
        Bukkit.getScheduler().runTaskAsynchronously(plugin, Runnable {
            val company = companyRepo.getCompanyByName(companyName)
            val hasAccess = permissionService.hasAccess(sender, company, AccessLevel.SALARIE)
            val membersList = company?.let {companyMembersRepo.getMembersInCompany(company)}
            
            val lines = when {
                !hasAccess -> listOf("§cVous n'avez pas le droit d'utiliser cette commande.")
                company == null -> listOf("§cIl n'y a pas d'entreprise.")
                membersList!!.isEmpty() -> listOf("§cCette entreprise n'a pas de membre.")
                else -> listOf("§aListe des membres de l'entreprise : §e${company.name}§a") +
                    membersList.map {member ->
                        val memberName = Bukkit.getOfflinePlayer(UUID.fromString(member.playerUuid)).name ?: "inconnu"
                        "§aNom : §e$memberName §a| Profession : §e${member.rank}"
                    }
            }
            Bukkit.getScheduler().runTask(plugin, Runnable {
                lines.forEach{sender.sendMessage(it)}
            })
        })
        
        return true
    }
    
    override fun tabComplete(sender: CommandSender, args: List<String>): List<String> {
        return if (args.size == 1) companyCache.companyNames(args[0]) else emptyList()
    }
}

class WhoAmICompanyCommand(private val plugin: BbCompany): SubCommand {
    
    private val companyRepo = CompanyRepository(plugin.databaseManager)
    private val companyMembersRepo = CompanyMembersRepository(plugin.databaseManager)
    private val permissionService = PermissionService(CompanyMembersRepository(plugin.databaseManager))

    
    override fun execute(sender: CommandSender, args: List<String>): Boolean {
        if (sender !is Player) {
            sender.sendMessage("§cCette commande doit être exécutée en jeu.")
            return true
        }
        
        if (args.isNotEmpty()) {
            sender.sendMessage("§cUsage : /company whoami")
            return true
        }
        
        Bukkit.getScheduler().runTaskAsynchronously(plugin, Runnable {
            val hasAccess = permissionService.hasAccess(sender, null, AccessLevel.JOUEUR)
            val companies = companyMembersRepo.getCompaniesToMember(sender)
            val lines = when {
                !hasAccess -> listOf("§cVous n'avez pas le droit d'utiliser cette commande.")
                companies.isEmpty() -> listOf("§cVous n'êtes dans aucune entreprise.")
                else -> {
                    companies.map {companyMember ->
                        val company = companyRepo.getCompanyById(companyMember.companyId)
                        val companyName = company?.name ?: "inconnue"
                        "§aNom de l'entreprise : §e$companyName §a| Rang : §e${companyMember.rank}"
                    }
                }
            }
            Bukkit.getScheduler().runTask(plugin, Runnable {
                lines.forEach{sender.sendMessage(it)}
            })
        })
        
        return true
    }
}


class DisbandCompanyCommand(
    private val plugin: BbCompany,
    private val companyCache: CompanyCache,
    private val playerProfessionCache: PlayerProfessionCache
    ) : SubCommand {
    
    private val companyRepo = CompanyRepository(plugin.databaseManager)
    private val pendingConfirmations = mutableMapOf<UUID, PendingDisband>()
    private val permissionService = PermissionService(CompanyMembersRepository(plugin.databaseManager))

    private data class PendingDisband(val companyName: String, val expiresAt: Instant)
    
    override fun execute(sender: CommandSender, args: List<String>): Boolean {
        if (sender !is Player) {
            sender.sendMessage("§cCette commande doit être exécutée en jeu.")
            return true
        }
        
        if (args.isEmpty()) {
            sender.sendMessage("§cUsage : /company disband <companyName>")
            return true
        }
        
        val companyName = args[0]
        val isConfirmation = args.size > 1 && args[1].equals("confirm", ignoreCase = true)
        
        if (!isConfirmation) {
            pendingConfirmations[sender.uniqueId] = PendingDisband(companyName, Instant.now().plusSeconds(10))
            val confirmButton = TextComponent("[Oui]").apply {
                color = ChatColor.GREEN
                clickEvent = ClickEvent(ClickEvent.Action.RUN_COMMAND, "/company disband $companyName confirm")
            }
            val cancelButton = TextComponent("[Non]").apply {
                color = ChatColor.RED
                clickEvent = ClickEvent(ClickEvent.Action.RUN_COMMAND, "/company canceldisband")
            }

            val message = TextComponent("§eEs-tu sûr de vouloir dissoudre §c$companyName§e ? ")
            message.addExtra(confirmButton)
            message.addExtra(TextComponent(" "))
            message.addExtra(cancelButton)

            sender.spigot().sendMessage(message)
            return true
        }
        val pending = pendingConfirmations[sender.uniqueId]
        if (pending == null || pending.companyName != companyName || Instant.now().isAfter(pending.expiresAt)) {
            sender.sendMessage("§cAucune demande de confirmation valide, retape la commande.")
            return true
        }
        pendingConfirmations.remove(sender.uniqueId)
        
        Bukkit.getScheduler().runTaskAsynchronously(plugin, Runnable {
            val company = companyRepo.getCompanyByName(companyName)
            val hasAccess = permissionService.hasAccess(sender, company, AccessLevel.ADMIN)
            
            var cacheUpdate: (() -> Unit)? = null
            
            val message = when {
                !hasAccess -> "§cVous n'avez pas le droit d'utiliser cette commande."
                company == null -> "§cCette entreprise n'existe pas."
                else -> {
                    companyRepo.deleteCompany(company)
                    cacheUpdate = {
                        companyCache.onCompanyDeleted(company.name)
                        playerProfessionCache.onCompanyDisbanded(company.id)
                    }
                    "§a✔ L'entreprise §e$companyName §aa été dissoute."
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
            1 -> companyCache.companyNames(args[0])
            2 -> listOf("confirm").filter { it.startsWith(args[1], ignoreCase = true) }
            else -> emptyList()
        }
    }
}

class RenameCompanyCommand(
    private val plugin: BbCompany,
    private val companyCache: CompanyCache
    ) : SubCommand {
    
    private val companyRepo = CompanyRepository(plugin.databaseManager)
    private val permissionService = PermissionService(CompanyMembersRepository(plugin.databaseManager))

    override fun execute(sender: CommandSender, args: List<String>): Boolean {
        if (sender !is Player) {
            sender.sendMessage("§cCette commande doit être exécutée en jeu.")
            return true
        }
        
        if (args.size < 2) {
            sender.sendMessage("§cUsage : /company rename <companyName> <newName>")
            return true
        }
        
        val companyName = args[0]
        val newName = args[1]
        
        Bukkit.getScheduler().runTaskAsynchronously(plugin, Runnable {
            val company = companyRepo.getCompanyByName(companyName)
            val hasAccess = permissionService.hasAccess(sender, company, AccessLevel.ADMIN)
            val alreadyExists = companyRepo.getCompanyByName(newName) != null
            
            var cacheUpdate: (() -> Unit)? = null
            
            val message = when {
                !hasAccess -> "§cVous n'avez pas le droit d'utiliser cette commande."
                company == null -> "§cCette entreprise n'existe pas."
                alreadyExists -> "§cCe nom est déjà utilisé."
                else -> {
                companyRepo.renameCompany(company, newName)
                cacheUpdate = {
                    companyCache.onCompanyRenamed(companyName, newName, company.professionCode)
                }
                "§aL'entreprise a été renommée par §e$newName §a."
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
        return if (args.size == 1) companyCache.companyNames(args[0]) else emptyList()
    }
    
}

class TransferCompanyCommand(
    private val plugin: BbCompany,
    private val companyCache: CompanyCache) : SubCommand {
    
    private val companyRepo = CompanyRepository(plugin.databaseManager)
    private val permissionService = PermissionService(CompanyMembersRepository(plugin.databaseManager))

    
    override fun execute(sender: CommandSender, args: List<String>): Boolean {
        if (sender !is Player) {
            sender.sendMessage("§cCette commande doit être exécutée en jeu.")
            return true
        }
        
        if (args.size < 2) {
            sender.sendMessage("§cUsage : /company transfer <companyName> <newOwner>")
            return true
        }
        
        val companyName = args[0]
        val newOwner = args[1]
        val target = Bukkit.getPlayer(newOwner)
        
        if (target == null){
            sender.sendMessage("§cCe joueur n'existe pas.")
            return true
        }
        
        Bukkit.getScheduler().runTaskAsynchronously(plugin, Runnable {
            val company = companyRepo.getCompanyByName(companyName)
            val hasAccess = permissionService.hasAccess(sender, company, AccessLevel.ADMIN)
            val message = when {
                !hasAccess -> "§cVous n'avez pas le droit d'utiliser cette commande."
                company == null -> "§cCette entreprise n'existe pas."
                else -> {
                    companyRepo.changeCompanyOwner(company, target.uniqueId.toString())
                    "§e$newOwner §aest maintenant le propriétaire de §e${company.name}§a."
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
            1 -> companyCache.companyNames(args[0])
            2 -> Bukkit.getOnlinePlayers().map { it.name }.filter { it.startsWith(args[1], ignoreCase = true) }
            else -> emptyList()
        }
    }
    
}