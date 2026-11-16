package com.bbcompany

import com.bbcompany.cache.*
import com.bbcompany.command.admin.CompanyCommand
import com.bbcompany.config.PluginConfig
import com.bbcompany.database.DatabaseManager
import com.bbcompany.database.repository.*
import com.bbcompany.listener.*
import org.bukkit.plugin.java.JavaPlugin

class BbCompany : JavaPlugin() {

    lateinit var databaseManager: DatabaseManager
        private set

    lateinit var pluginConfig: PluginConfig
        private set

    companion object {
        lateinit var instance: BbCompany
            private set
    }

    override fun onEnable() {
        instance = this
        saveDefaultConfig()

        pluginConfig = PluginConfig(config)

        databaseManager = DatabaseManager(this)
        databaseManager.connect()

        
        val restrictedCraftCache = RestrictedCraftCache(RestrictedCraftRepository(databaseManager))
        restrictedCraftCache.load()
        
        val restrictedDropCache = RestrictedDropCache(RestrictedDropRepository(databaseManager))
        restrictedDropCache.load()

        val restrictedSmeltCache = RestrictedSmeltCache(RestrictedSmeltRepository(databaseManager))
        restrictedSmeltCache.load()
        
        val playerProfessionCache = PlayerProfessionCache(CompanyMembersRepository(databaseManager))
        playerProfessionCache.load()
        
        val companyCache = CompanyCache(
            CompanyRepository(databaseManager),
            ProfessionRepository(databaseManager),
            RankRepository(databaseManager)
        )
        playerProfessionCache.load()
        
        server.pluginManager.registerEvents(
            RestrictedBlockListener(playerProfessionCache),
            this
        )
        
        server.pluginManager.registerEvents(
            RestrictedCraftListener(restrictedCraftCache, playerProfessionCache),
            this
        )
        
        server.pluginManager.registerEvents(
            RestrictedDropListener(restrictedDropCache, playerProfessionCache),
            this
        )
        
        server.pluginManager.registerEvents(
            RestrictedSmeltListener(this, restrictedSmeltCache, playerProfessionCache),
            this
        )
        
        server.pluginManager.registerEvents(
            RestrictedTradeListener(playerProfessionCache),
            this
        )
        
        server.pluginManager.registerEvents(
            RestrictedFishingListener(playerProfessionCache),
            this
        )
        
        val companyCmd = CompanyCommand(this, restrictedCraftCache, restrictedDropCache, restrictedSmeltCache, playerProfessionCache, companyCache)
        getCommand("company")?.setExecutor(companyCmd)
        getCommand("company")?.tabCompleter = companyCmd
        
        
        logger.info("BbCompany plugin enabled!")
    }

    override fun onDisable() {
        if (::databaseManager.isInitialized) {
            databaseManager.disconnect()
        }
        logger.info("BbCompany plugin disabled!")
    }
}
