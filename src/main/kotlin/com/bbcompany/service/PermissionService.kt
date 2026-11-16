package com.bbcompany.service

import com.bbcompany.database.repository.Company
import com.bbcompany.database.repository.CompanyMembersRepository
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

enum class AccessLevel {
    JOUEUR, SALARIE, ADJOINT, CHEF, ADMIN
}

class PermissionService(private val companyMembersRepo: CompanyMembersRepository) {

    companion object {
        private const val ADJOINT_LEVEL = 1
        private const val CHEF_LEVEL = 2
    }

    fun hasAccess(sender: CommandSender, company: Company?, required: AccessLevel): Boolean {
        if (sender.hasPermission("bbcompany.admin")) return true
        if (required == AccessLevel.ADMIN) return false
        if (required == AccessLevel.JOUEUR) return true
        if (company == null || sender !is Player) return false

        val member = companyMembersRepo.getMembersInCompany(company)
            .find { it.playerUuid == sender.uniqueId.toString() } ?: return false

        return when (required) {
            AccessLevel.SALARIE -> true
            AccessLevel.ADJOINT -> member.rankLevel >= ADJOINT_LEVEL
            AccessLevel.CHEF -> member.rankLevel >= CHEF_LEVEL
        }
    }
}