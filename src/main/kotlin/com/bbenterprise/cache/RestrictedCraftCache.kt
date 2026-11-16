package com.bbenterprise.cache

import com.bbenterprise.database.repository.RestrictedCraftRepository

class RestrictedCraftCache(private val repo: RestrictedCraftRepository) {

    @Volatile
    private var restrictions: Map<String, Set<String>> = emptyMap()

    fun load() {
        restrictions = repo.findAll()
            .groupBy({ it.itemKey }, { it.professionCode })
            .mapValues { it.value.toSet() }
    }

    fun authorizedProfessionsFor(itemKey: String): Set<String> = restrictions[itemKey].orEmpty()
    
    fun onRestrictedCraftAdded(itemKey: String, professionCode: String) {
        val current = restrictions[itemKey].orEmpty()
        restrictions = restrictions + (itemKey to (current + professionCode))
    }
}