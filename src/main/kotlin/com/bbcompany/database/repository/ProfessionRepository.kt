package com.bbcompany.database.repository

import com.bbcompany.database.DatabaseManager
import org.bukkit.Bukkit
import java.sql.SQLException

data class Profession(
    val code: String,
    val label: String
)

class ProfessionRepository(private val db: DatabaseManager) {
    
    fun findAll(): List<Profession> {
        val sql = """
            SELECT * FROM bb_professions
        """.trimIndent()
        try {
            db.getConnection().use {c ->
                c.prepareStatement(sql)
                .use {ps ->
                    ps.executeQuery().use { rs ->
                        val professions = mutableListOf<Profession>()
                        while (rs.next()) {
                            professions += Profession(
                                code = rs.getString("code"),
                                label = rs.getString("label"))
                        }
                        return professions
                    }
                }
            }
        } catch (e: SQLException) {
            Bukkit.getLogger().severe("[BbCompany] findAllProfessions: ${e.message}")
            return emptyList()
        }
    }

    
    fun getProfessionByCode(professionCode: String): Profession? {
        val sql = """
          SELECT code, label FROM bb_professions WHERE code = ?
        """.trimIndent()
        try {
            db.getConnection().use {c ->
                c.prepareStatement(sql)
                .use {ps ->
                    ps.setString(1, professionCode)
                    ps.executeQuery().use { rs ->
                        if (!rs.next()) return null
                        return Profession(
                            code = professionCode,
                            label = rs.getString("label"),
                        )
                    }
                }
            }
        } catch (e: SQLException) {
            Bukkit.getLogger().severe("[BbCompany] getProfessionByCode: ${e.message}")
            return null
        }
    }
}