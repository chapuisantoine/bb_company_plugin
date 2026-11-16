package com.bbcompany.cache

import com.bbcompany.database.repository.CompanyMembersRepository

class PlayerProfessionCache(private val companyMembersRepo: CompanyMembersRepository) {

    @Volatile
    private var companiesByPlayer: Map<String, Map<Int, String>> = emptyMap()

    fun load() {
        companiesByPlayer = companyMembersRepo.getAllPlayerCompanyProfessions()
            .groupBy({ it.first }) { it.second to it.third }
            .mapValues { it.value.toMap() }
    }

    fun hasProfession(playerUuid: String, professionCode: String): Boolean =
        companiesByPlayer[playerUuid]?.values?.contains(professionCode) ?: false

    fun onMemberAdded(playerUuid: String, companyId: Int, professionCode: String) {
        val current = companiesByPlayer[playerUuid].orEmpty()
        companiesByPlayer = companiesByPlayer + (playerUuid to (current + (companyId to professionCode)))
    }

    fun onMemberRemoved(playerUuid: String, companyId: Int) {
        val current = companiesByPlayer[playerUuid] ?: return
        companiesByPlayer = companiesByPlayer + (playerUuid to (current - companyId))
    }

    fun onCompanyDisbanded(companyId: Int) {
        companiesByPlayer = companiesByPlayer.mapValues { (_, companies) -> companies - companyId }
    }
}