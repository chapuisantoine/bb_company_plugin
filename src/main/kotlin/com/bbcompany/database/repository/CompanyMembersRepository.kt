package com.bbcompany.database.repository

import com.bbcompany.database.DatabaseManager
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import java.sql.SQLException
import java.sql.Timestamp
import java.time.LocalDateTime

data class CompanyMember(
    val companyId: Int,
    val playerUuid: String,
    val rank: String,
    val rankLevel: Int,
    val joinedAt: LocalDateTime
)


class CompanyMembersRepository(private val db: DatabaseManager) {
    
    fun addMemberToCompany(company: Company, rank: Rank, player: Player) {
        val sql = """
            INSERT INTO bb_company_members
            (company_id, player_uuid, rank_id, joined_at)
            VALUES (?, ?, ?, ?)
        """.trimIndent()
        try {
            db.getConnection().use {c ->
                c.prepareStatement(sql)
                .use {ps ->
                    ps.setInt(1, company.id)
                    ps.setString(2, player.uniqueId.toString())
                    ps.setInt(3, rank.id)
                    ps.setTimestamp(4, Timestamp.valueOf(LocalDateTime.now()))
                    ps.executeUpdate()
                } 
            }
        } catch (e: SQLException) {
            Bukkit.getLogger().severe("[BbCompany] addMemberToCompany: ${e.message}")
        }
    }
    
    fun getAllPlayerCompanyProfessions(): List<Triple<String, Int, String>> {
        val sql = """
            SELECT bb_company_members.player_uuid, bb_company_members.company_id, bb_companies.profession_code
            FROM bb_company_members
            JOIN bb_companies ON bb_company_members.company_id = bb_companies.id
        """.trimIndent()
        try {
            db.getConnection().use { conn ->
                conn.prepareStatement(sql).use { ps ->
                    ps.executeQuery().use { rs ->
                        val result = mutableListOf<Triple<String, Int, String>>()
                        while (rs.next()) {
                            result += Triple(
                                rs.getString("player_uuid"),
                                rs.getInt("company_id"),
                                rs.getString("profession_code")
                            ) 
                        }
                        return result
                    }
                }
            }
        } catch (e: SQLException) {
            Bukkit.getLogger().severe("[BbCompany] getAllPlayerCompanyProfessions: ${e.message}")
            return emptyList()
        }
    }
    
    fun getMembersInCompany(company: Company): List<CompanyMember> {
        val sql ="""
            SELECT bb_company_members.player_uuid, bb_ranks.label, bb_ranks.level, bb_company_members.joined_at FROM bb_company_members
            LEFT JOIN bb_ranks
                ON bb_company_members.rank_id = bb_ranks.id
            WHERE company_id = ?
        """.trimIndent()
        try {   
            db.getConnection().use { conn ->
                conn.prepareStatement(sql)
                .use { ps ->
                    ps.setInt(1, company.id)
                    ps.executeQuery().use { rs ->
                        val members = mutableListOf<CompanyMember>()
                        while (rs.next()) {
                            members += CompanyMember(
                                companyId = company.id,
                                playerUuid = rs.getString("player_uuid"),
                                rank = rs.getString("label"),
                                rankLevel = rs.getInt("level"),
                                joinedAt = rs.getTimestamp("joined_at").toLocalDateTime(),
                            )
                        }
                        return members
                    }
                }
            }
        } catch (e: SQLException) {
            Bukkit.getLogger().severe("[BbCompany] getMembersInCompany: ${e.message}")
            return emptyList()
        }
    }
    
    fun getNumberMembersInCompany(company: Company): Int {
        val sql  ="""
            SELECT COUNT (*) FROM bb_company_members
            WHERE company_id = ?
        """.trimIndent()
        try {   
            db.getConnection().use { conn ->
                conn.prepareStatement(sql)
                .use { ps ->
                    ps.setInt(1, company.id)
                    ps.executeQuery().use { rs ->
                        return if (rs.next()) rs.getInt(1) else 0
                    }
                }
            }
        } catch (e: SQLException) {
            Bukkit.getLogger().severe("[BbCompany] getNumberMembersInCompany: ${e.message}")
            return -1
        }
    }
    
    fun getCompaniesToMember(player: Player): List<CompanyMember> {
        val sql  ="""
            SELECT bb_company_members.company_id, bb_ranks.label, bb_ranks.level, bb_company_members.joined_at FROM bb_company_members
            LEFT JOIN bb_ranks
                ON bb_company_members.rank_id = bb_ranks.id
            WHERE player_uuid = ?
        """.trimIndent()
        try {   
            db.getConnection().use { conn ->
                conn.prepareStatement(sql)
                .use { ps ->
                    ps.setString(1, player.uniqueId.toString())
                    ps.executeQuery().use { rs ->
                        val members = mutableListOf<CompanyMember>()
                        while (rs.next()) {
                            members += CompanyMember(
                                companyId = rs.getInt("company_id"),
                                playerUuid = player.uniqueId.toString(),
                                rank = rs.getString("label"),
                                rankLevel = rs.getInt("level"),
                                joinedAt = rs.getTimestamp("joined_at").toLocalDateTime(),
                            )
                        }
                        return members
                    }
                }
            }
        } catch (e: SQLException) {
            Bukkit.getLogger().severe("[BbCompany] getCompaniesToMember: ${e.message}")
            return emptyList()
        }
    }
    
    fun updateMemberRankInCompany(player: Player, company: Company, rank: Rank) {
        val sql = """
        UPDATE bb_company_members
        SET rank_id = ?
        WHERE player_uuid = ? AND company_id = ?
        """.trimIndent()
        try {
            db.getConnection().use { conn ->
                conn.prepareStatement(sql).use { ps ->
                    ps.setInt(1, rank.id)
                    ps.setString(2, player.uniqueId.toString())
                    ps.setInt(3, company.id)
                    ps.executeUpdate()
                }
            }
        } catch (e: SQLException) {
            Bukkit.getLogger().severe("[BbCompany] updateMemberRankInCompany: ${e.message}")
        }
    }
    
    fun deleteMemberInCompany(player: Player, company: Company) {
        val sql = """
            DELETE FROM bb_company_members
            WHERE player_uuid = ? AND company_id = ?
        """.trimIndent()
        try {   
            db.getConnection().use { conn ->
                conn.prepareStatement(sql)
                .use { ps ->
                    ps.setString(1, player.uniqueId.toString())
                    ps.setInt(2, company.id)
                    ps.executeUpdate()
                }
            }
        } catch (e: SQLException) {
            Bukkit.getLogger().severe("[BbCompany] deleteMemberInCompany: ${e.message}")
        }
    }
}