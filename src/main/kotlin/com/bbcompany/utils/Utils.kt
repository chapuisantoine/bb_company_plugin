package com.bbcompany.utils

import org.bukkit.Bukkit
import org.bukkit.Keyed
import org.bukkit.Material
import org.bukkit.inventory.BlastingRecipe
import org.bukkit.inventory.CookingRecipe
import org.bukkit.inventory.FurnaceRecipe
import org.bukkit.inventory.SmokingRecipe

object CookingRecipeIndex {
    private val recipeKeyByInput: Map<String, String> by lazy {
        val map = mutableMapOf<String, String>()
        Bukkit.recipeIterator().forEach { recipe ->
            if (recipe is FurnaceRecipe || recipe is SmokingRecipe || recipe is BlastingRecipe) {
                val cooking = recipe as CookingRecipe<*>
                map[cooking.input.type.key.toString()] = (recipe as Keyed).key.toString()
            }
        }
        map
    }

    fun recipeKeyFor(material: Material): String? = recipeKeyByInput[material.key.toString()]
}