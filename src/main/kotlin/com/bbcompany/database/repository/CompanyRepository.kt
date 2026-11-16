package com.bbcompany.database.repository

import com.bbcompany.database.DatabaseManager
import org.bukkit.Bukkit
import java.sql.SQLException
import java.sql.Statement
import java.time.LocalDateTime

data class Company(
    val id: Int = 0,
    val name: String,
    val professionCode: String,
    val ownerUuid: String,
    val createdAt: LocalDateTime = LocalDateTime.now()
)

class CompanyRepository(private val db: DatabaseManager) {
    
    fun findAll(): List<Company> {
        val sql = """
            SELECT id, name, profession_code, owner_uuid, created_at FROM bb_companies
        """.trimIndent()
        try {
            db.getConnection().use {c ->
                c.prepareStatement(sql)
                .use {ps ->
                    ps.executeQuery().use { rs ->
                        val companies = mutableListOf<Company>()
                        while (rs.next()) {
                            companies += Company(
                                id = rs.getInt("id"),
                                name = rs.getString("name"),
                                professionCode = rs.getString("profession_code"),
                                ownerUuid = rs.getString("owner_uuid"),
                                createdAt = rs.getTimestamp("created_at").toLocalDateTime(),
                                )
                        }
                        return companies
                    }
                }
            }
        } catch (e: SQLException) {
            Bukkit.getLogger().severe("[BbCompany] findAll: ${e.message}")
            return emptyList()
        }
    }
    
    
    fun createCompany(company: Company) {
        val sql = """
            INSERT INTO bb_companies 
            (name, profession_code, owner_uuid) 
            VALUES (?, ?, ?)
        """.trimIndent()
        try {
            db.getConnection().use {c -> 
                c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)
                    .use {ps ->
                        ps.setString(1, company.name)
                        ps.setString(2, company.professionCode)
                        ps.setString(3, company.ownerUuid)
                        ps.executeUpdate()
                        ps.generatedKeys.use { keys ->
                            if (!keys.next()) throw SQLException("No generated key returned")
                    }
                }               
            }
        } catch (e: SQLException) {
            Bukkit.getLogger().severe("[BbCompany] createCompany: ${e.message}")
        }
    }
    
    fun getCompanyByName(companyName: String): Company? {
        val sql = """
            SELECT id, profession_code, owner_uuid, created_at FROM bb_companies
            WHERE name = ?
        """.trimIndent()
        try {
            db.getConnection().use {c -> 
                c.prepareStatement(sql)
                    .use {ps ->
                        ps.setString(1, companyName)
                        ps.executeQuery().use { rs ->
                            if (!rs.next()) return null
                            return Company(
                                id = rs.getInt("id"),
                                name = companyName,
                                professionCode = rs.getString("profession_code"),
                                ownerUuid = rs.getString("owner_uuid"),
                                createdAt = rs.getTimestamp("created_at").toLocalDateTime()
                            )
                        }
                }               
            }
        } catch (e: SQLException) {
            Bukkit.getLogger().severe("[BbCompany] getCompanyByName: ${e.message}")
            return null
        }
    }
    
    fun getCompanyById(companyId: Int): Company? {
        val sql = """
            SELECT name, profession_code, owner_uuid, created_at FROM bb_companies
            WHERE id = ?
        """.trimIndent()
        try {
            db.getConnection().use {c -> 
                c.prepareStatement(sql)
                    .use {ps ->
                        ps.setInt(1, companyId)
                        ps.executeQuery().use { rs ->
                            if (!rs.next()) return null
                            return Company(
                                id = companyId,
                                name = rs.getString("name"),
                                professionCode = rs.getString("profession_code"),
                                ownerUuid = rs.getString("owner_uuid"),
                                createdAt = rs.getTimestamp("created_at").toLocalDateTime()
                            )
                        }
                }               
            }
        } catch (e: SQLException) {
            Bukkit.getLogger().severe("[BbCompany] getCompanyById: ${e.message}")
            return null
        }
    }
    
    fun renameCompany(company: Company, newName: String) {
        val sql = """
            UPDATE bb_companies
            SET name = ?
            WHERE id = ?
        """.trimIndent()
        try {
            db.getConnection().use { conn ->
                conn.prepareStatement(sql).use { ps ->
                    ps.setString(1, newName)
                    ps.setInt(2, company.id)
                    ps.executeUpdate()
                }
            }
        } catch (e: SQLException) {
            Bukkit.getLogger().severe("[BbCompany] renameCompany: ${e.message}")
        }
    }
    
    fun changeCompanyOwner(company: Company, newOwner: String) {
        val sql = """
            UPDATE bb_companies
            SET owner_uuid = ?
            WHERE id = ?
        """.trimIndent()
        try {
            db.getConnection().use { conn ->
                conn.prepareStatement(sql).use { ps ->
                    ps.setString(1, newOwner)
                    ps.setInt(2, company.id)
                    ps.executeUpdate()
                }
            }
        } catch (e: SQLException) {
            Bukkit.getLogger().severe("[BbCompany] changeCompanyOwner: ${e.message}")
        }
        
    }
    
    fun deleteCompany(company: Company) {
        val sql = """
            DELETE FROM bb_companies
            WHERE id = ?
        """.trimIndent()
        try {
            db.getConnection().use {conn ->
                conn.prepareStatement(sql)
                .use {ps ->
                    ps.setInt(1, company.id)
                    ps.executeUpdate()
                }
            }
        } catch (e: SQLException) {
            Bukkit.getLogger().severe("[BbCompany] deleteCompany: ${e.message}")
        }
    }
}