package com.bbcompany.cache

import com.bbcompany.database.repository.RestrictedDropRepository

class RestrictedDropCache(private val repo: RestrictedDropRepository) {

    @Volatile
    private var restrictions: Map<String, Map<String, Set<String>>> = emptyMap()

    fun load() {
        restrictions = repo.findAll()
            .groupBy { it.entityType }
            .mapValues { (_, drops) ->
                drops.groupBy({ it.itemKey }, { it.professionCode })
                    .mapValues { it.value.toSet() }
            }
    }

    fun authorizedProfessionsFor(entityType: String, itemKey: String): Set<String> =
        restrictions[entityType]?.get(itemKey).orEmpty()

    fun onRestrictedDropAdded(entityType: String, itemKey: String, professionCode: String) {
        val itemMap = restrictions[entityType].orEmpty()
        val current = itemMap[itemKey].orEmpty()
        restrictions = restrictions + (entityType to (itemMap + (itemKey to (current + professionCode))))
    }
}