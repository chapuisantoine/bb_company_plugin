package com.bbcompany.database.repository

import com.bbcompany.database.DatabaseManager
import org.bukkit.Bukkit
import org.bukkit.inventory.ItemStack
import java.sql.SQLException

data class RestrictedDrop(
    val entityType: String,
    val itemKey: String,
    val professionCode: String
)

class RestrictedDropRepository(private val db: DatabaseManager) {
    fun findAll(): List<RestrictedDrop> {
        val sql = "SELECT entity_type, item_key, profession_code FROM bb_restricted_drops"
        try {
            db.getConnection().use { conn ->
                conn.prepareStatement(sql).use { ps ->
                    ps.executeQuery().use { rs ->
                        val result = mutableListOf<RestrictedDrop>()
                        while (rs.next()) {
                            result += RestrictedDrop(
                                entityType = rs.getString("entity_type"),
                                itemKey = rs.getString("item_key"),
                                professionCode = rs.getString("profession_code")
                            )
                        }
                        return result
                    }
                }
            }
        } catch (e: SQLException) {
            Bukkit.getLogger().severe("[BbCompany] findAllRestrictedDrops: ${e.message}")
            return emptyList()
        }
    }

    fun isAlreadyExists(entityType: String, item: ItemStack, professionCode: String): Boolean {
        val sql = """
            SELECT 1 FROM bb_restricted_drops
            WHERE entity_type = ? AND item_key = ? AND profession_code = ?
        """.trimIndent()
        try {
            db.getConnection().use { conn ->
                conn.prepareStatement(sql).use { ps ->
                    ps.setString(1, entityType)
                    ps.setString(2, item.type.key.toString())
                    ps.setString(3, professionCode)
                    ps.executeQuery().use { rs -> return rs.next() }
                }
            }
        } catch (e: SQLException) {
            Bukkit.getLogger().severe("[BbCompany] isAlreadyExistsDrop: ${e.message}")
            return false
        }
    }

    fun addRestrictedDrop(entityType: String, item: ItemStack, professionCode: String) {
        val sql = """
            INSERT INTO bb_restricted_drops
            (entity_type, item_key, profession_code)
            VALUES (?, ?, ?)
        """.trimIndent()
        try {
            db.getConnection().use { conn ->
                conn.prepareStatement(sql).use { ps ->
                    ps.setString(1, entityType)
                    ps.setString(2, item.type.key.toString())
                    ps.setString(3, professionCode)
                    ps.executeUpdate()
                }
            }
        } catch (e: SQLException) {
            Bukkit.getLogger().severe("[BbCompany] addRestrictedDrop: ${e.message}")
        }
    }
}