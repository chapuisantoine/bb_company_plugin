package com.bbcompany.cache

import com.bbcompany.database.repository.CompanyRepository
import com.bbcompany.database.repository.ProfessionRepository
import com.bbcompany.database.repository.RankRepository

class CompanyCache(
    private val companyRepo: CompanyRepository,
    private val professionRepo: ProfessionRepository,
    private val rankRepo: RankRepository
) {

    @Volatile
    private var companiesByName: Map<String, String> = emptyMap()

    @Volatile
    private var professionCodes: List<String> = emptyList()

    @Volatile
    private var ranksByProfession: Map<String, List<String>> = emptyMap()

    fun load() {
        companiesByName = companyRepo.findAll().associate { it.name to it.professionCode }
        professionCodes = professionRepo.findAll().map { it.code }
        ranksByProfession = professionCodes.associateWith { code ->
            rankRepo.getRanksForProfession(code).map { it.code }
        }
    }

    fun companyNames(prefix: String): List<String> =
        companiesByName.keys.filter { it.startsWith(prefix, ignoreCase = true) }

    fun professionCodeOf(companyName: String): String? = companiesByName[companyName]

    fun professionCodes(prefix: String): List<String> =
        professionCodes.filter { it.startsWith(prefix, ignoreCase = true) }

    fun ranksFor(professionCode: String, prefix: String): List<String> =
        ranksByProfession[professionCode].orEmpty().filter { it.startsWith(prefix, ignoreCase = true) }
        
    fun onCompanyCreated(name: String, professionCode: String) {
        companiesByName = companiesByName + (name to professionCode)
    }

    fun onCompanyRenamed(oldName: String, newName: String, professionCode: String) {
        companiesByName = (companiesByName - oldName) + (newName to professionCode)
    }

    fun onCompanyDeleted(name: String) {
        companiesByName = companiesByName - name
    }
}