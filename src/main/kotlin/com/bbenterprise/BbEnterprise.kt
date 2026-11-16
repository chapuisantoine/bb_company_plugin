package com.bbenterprise

import com.bbenterprise.cache.*
import com.bbenterprise.command.admin.CompanyCommand
import com.bbenterprise.config.PluginConfig
import com.bbenterprise.database.DatabaseManager
import com.bbenterprise.database.repository.*
import com.bbenterprise.listener.*
import org.bukkit.plugin.java.JavaPlugin

class BbEnterprise : JavaPlugin() {

    lateinit var databaseManager: DatabaseManager
        private set

    lateinit var pluginConfig: PluginConfig
        private set

    companion object {
        lateinit var instance: BbEnterprise
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
        
        
        logger.info("BbEnterprise plugin enabled!")
    }

    override fun onDisable() {
        if (::databaseManager.isInitialized) {
            databaseManager.disconnect()
        }
        logger.info("BbEnterprise plugin disabled!")
    }
}
