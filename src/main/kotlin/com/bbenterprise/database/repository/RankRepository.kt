package com.bbenterprise.database.repository

import com.bbenterprise.database.DatabaseManager
import org.bukkit.Bukkit
import java.sql.SQLException

data class Rank(
    val id: Int = 0,
    val professionCode: String,
    val code: String,
    val label: String,
    val level: Int
)


class RankRepository(private val db: DatabaseManager) {
    
    fun getRankByProfessionCodes(company: Company, rankCode: String): Rank? {
        val sql = """
            SELECT id, code, label, level FROM bb_ranks
            WHERE profession_code = ? AND code = ?
        """.trimIndent()
        try {
            db.getConnection().use {c ->
                c.prepareStatement(sql)
                    .use {ps ->
                        ps.setString(1, company.professionCode)
                        ps.setString(2, rankCode)
                        ps.executeQuery().use { rs ->
                            if (!rs.next()) return null
                            return Rank(
                                id = rs.getInt("id"),
                                professionCode = company.professionCode,
                                code = rankCode,
                                label = rs.getString("label"),
                                level = rs.getInt("level")
                            )
                        }
                    } 
            }
        } catch (e: SQLException) {
            Bukkit.getLogger().severe("[BbEnterprise] getRankByCompanyCode: ${e.message}")
            return null
        }
    }
    
    fun getRanksForProfession(professionCode: String): List<Rank> {
        val sql = "SELECT id, code, label, level FROM bb_ranks WHERE profession_code = ?"
        try {
            db.getConnection().use {c ->
                c.prepareStatement(sql)
                    .use {ps ->
                        ps.setString(1, professionCode)
                        ps.executeQuery().use { rs ->
                            val ranks = mutableListOf<Rank>()
                            while (rs.next()) {
                                ranks += Rank(
                                    id = rs.getInt("id"),
                                    professionCode = professionCode,
                                    code = rs.getString("code"),
                                    label = rs.getString("label"),
                                    level = rs.getInt("level")
                                )
                            }  
                            return ranks
                        }
                    } 
            }
        } catch (e: SQLException) {
            Bukkit.getLogger().severe("[BbEnterprise] getRankByCompanyCode: ${e.message}")
            return emptyList()
        }
    }
}