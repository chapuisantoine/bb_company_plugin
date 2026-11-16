package com.bbenterprise.database.repository

import com.bbenterprise.database.DatabaseManager
import org.bukkit.Bukkit
import org.bukkit.inventory.ItemStack
import java.sql.SQLException

data class RestrictedSmelt(
    val itemKey: String,
    val professionCode: String
)

class RestrictedSmeltRepository(private val db: DatabaseManager) {
    fun findAll(): List<RestrictedSmelt> {
        val sql = "SELECT item_key, profession_code FROM bb_restricted_smelts"
        try {
            db.getConnection().use { conn ->
                conn.prepareStatement(sql).use { ps ->
                    ps.executeQuery().use { rs ->
                        val result = mutableListOf<RestrictedSmelt>()
                        while (rs.next()) {
                            result += RestrictedSmelt(
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
            SELECT 1 FROM bb_restricted_smelts
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
    
    fun addRestrictedSmelt(item: ItemStack, professionCode: String) {
        val sql = """
            INSERT INTO bb_restricted_smelts
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
            Bukkit.getLogger().severe("[BbEnterprise] addRestrictedSmelt: ${e.message}")
        }
    }
}