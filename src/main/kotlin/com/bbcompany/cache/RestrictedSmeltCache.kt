package com.bbcompany.cache

import com.bbcompany.database.repository.RestrictedSmeltRepository

class RestrictedSmeltCache(private val repo: RestrictedSmeltRepository) {
    
    @Volatile
    private var restrictions: Map<String, Set<String>> = emptyMap()

    fun load() {
        restrictions = repo.findAll()
            .groupBy({ it.itemKey }, { it.professionCode })
            .mapValues { it.value.toSet() }
    }

    fun authorizedProfessionsFor(itemKey: String): Set<String> = restrictions[itemKey].orEmpty()
    
    fun onRestrictedSmeltAdded(itemKey: String, professionCode: String) {
        val current = restrictions[itemKey].orEmpty()
        restrictions = restrictions + (itemKey to (current + professionCode))
    }
}