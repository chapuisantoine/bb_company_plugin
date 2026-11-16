package com.bbenterprise.database.repository

import com.bbenterprise.database.DatabaseManager
import org.bukkit.Bukkit
import org.bukkit.inventory.ItemStack
import java.sql.SQLException

data class RestrictedCraft(
    val itemKey: String,
    val professionCode: String
)

class RestrictedCraftRepository(private val db: DatabaseManager) {
    fun findAll(): List<RestrictedCraft> {
        val sql = "SELECT item_key, profession_code FROM bb_restricted_crafts"
        try {
            db.getConnection().use { conn ->
                conn.prepareStatement(sql).use { ps ->
                    ps.executeQuery().use { rs ->
                        val result = mutableListOf<RestrictedCraft>()
                        while (rs.next()) {
                            result += RestrictedCraft(
                                itemKey = rs.getString("item_key"),
                                professionCode = rs.getString("profession_code")
                            )
                        }
                        return result
                    }
                }
            }
        } catch (e: SQLException) {
            Bukkit.getLogger().severe("[BbEnterprise] findAllRestrictedCrafts: ${e.message}")
            return emptyList()
        }
    }
    
    fun isAlreadyExists(item: ItemStack, professionCode: String ): Boolean {
        val sql = """
            SELECT 1 FROM bb_restricted_crafts
            WHERE item_key = ? AND profession_code = ?
        """.trimIndent()
        try {   
            db.getConnection().use { conn ->
                conn.prepareStatement(sql)
                .use { ps ->
                    ps.setString(1, item.type.key.toString())
                    ps.setString(2, professionCode)
                    ps.executeQuery().use { rs ->
                        return rs.next()
                    }
                }
            }
        } catch (e: SQLException) {
            Bukkit.getLogger().severe("[BbEnterprise] isAlreadyExists: ${e.message}")
            return false
        }
    }
    
    fun addRestrictedCraft(item: ItemStack, professionCode: String) {
        val sql = """
            INSERT INTO bb_restricted_crafts
            (item_key, profession_code)
            VALUES (?, ?)
        """.trimIndent()
    try {
            db.getConnection().use {c ->
                c.prepareStatement(sql)
                .use {ps ->
                    ps.setString(1, item.type.key.toString())
                    ps.setString(2, professionCode)
                    ps.executeUpdate()
                } 
            }
        } catch (e: SQLException) {
            Bukkit.getLogger().severe("[BbEnterprise] addRestrictedItem: ${e.message}")
        }
    }
    
}