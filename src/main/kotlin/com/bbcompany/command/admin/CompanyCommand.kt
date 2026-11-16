package com.bbcompany.command.admin

import com.bbcompany.BbCompany
import com.bbcompany.cache.*
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter


class CompanyCommand(
    plugin: BbCompany,
    restrictedCraftCache: RestrictedCraftCache,
    restrictedDropCache: RestrictedDropCache,
    restrictedSmeltCache: RestrictedSmeltCache,
    playerProfessionCache: PlayerProfessionCache,
    companyCache: CompanyCache
    ) : CommandExecutor, TabCompleter {

    private val subCommands: Map<String, SubCommand> = mapOf(
        "create" to CreateCompanyCommand(plugin, companyCache, playerProfessionCache),
        "info" to InfoCompanyCommand(plugin, companyCache),
        "list" to ListCompanyCommand(plugin),
        "members" to ListMembersCompanyCommand(plugin, companyCache),
        "whoami" to WhoAmICompanyCommand(plugin),
        "disband" to DisbandCompanyCommand(plugin, companyCache, playerProfessionCache),
        "rename" to RenameCompanyCommand(plugin, companyCache),
        "transfer" to TransferCompanyCommand(plugin, companyCache),
        "addmember" to AddCompanyMemberCommand(plugin, playerProfessionCache, companyCache),
        "removemember" to RemoveCompanyMemberCommand(plugin, playerProfessionCache, companyCache),
        "setrankmember" to UpdateRankMemberCommand(plugin, companyCache),
        "refreshcache" to RefreshCacheCommand(plugin, restrictedCraftCache, playerProfessionCache, companyCache),
        "addrestrictedcraft" to AddRestrictedCraftItemCommand(plugin, restrictedCraftCache, companyCache),
        "addrestricteddrop" to AddRestrictedDropItemCommand(plugin, restrictedDropCache, companyCache),
        "addrestrictedsmelt" to AddRestrictedSmeltItemCommand(plugin, restrictedSmeltCache, companyCache)
    )
    
    private val adminOnlyCommands = setOf(
        "create", "disband", "rename", "transfer", "refreshcache", "addrestrictedcraft", "addrestricteddrop", "addrestrictedsmelt"
    )
    
    private val helpLines: Map<String, String> = mapOf(
        "create" to "/company create <name> <profession> <owner>",
        "info" to "/company info <companyName>",
        "list" to "/company list",
        "members" to "/company members <companyName>",
        "whoami" to "/company whoami",
        "disband" to "/company disband <companyName>",
        "rename" to "/company rename <companyName> <newName>",
        "transfer" to "/company transfer <companyName> <newOwner>",
        "addmember" to "/company addmember <player> <company> <rank>",
        "removemember" to "/company removemember <player> <company>",
        "setrankmember" to "/company setrankmember <player> <company> <rank>",
        "refreshcache" to "/company refreshcache",
        "addrestrictedcraft" to "/company addrestrictedcraft <profession>",
        "addrestricteddrop" to "/company addrestricteddrop <entity> <profession>",
        "addrestrictedsmelt" to "/company addrestrictedsmelt <profession>"
    )
    
    private fun visibleSubCommands(sender: CommandSender): List<String> =
        subCommands.keys.filter { it !in adminOnlyCommands || sender.hasPermission("bbcompany.admin") }

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        val sub = subCommands[args.getOrNull(0)?.lowercase()]
        if (sub == null) { sendHelp(sender); return true }
        return sub.execute(sender, args.drop(1))
    }

    override fun onTabComplete(sender: CommandSender, command: Command, label: String, args: Array<out String>): List<String> {
        if (args.size == 1) return subCommands.keys.filter { it.startsWith(args[0].lowercase()) }
        val sub = subCommands[args[0].lowercase()] ?: return emptyList()
        return sub.tabComplete(sender, args.drop(1))
    }

    private fun sendHelp(sender: CommandSender) {
        sender.sendMessage("§6§l=== Company ===")
        visibleSubCommands(sender).forEach { key ->
            sender.sendMessage("§e${helpLines[key]}")
        }
    }
}