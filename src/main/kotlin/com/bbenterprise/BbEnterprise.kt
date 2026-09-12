package com.bbenterprise

import com.bbenterprise.config.PluginConfig
import com.bbenterprise.database.DatabaseManager
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

        logger.info("BbEnterprise plugin enabled!")
    }

    override fun onDisable() {
        if (::databaseManager.isInitialized) {
            databaseManager.disconnect()
        }
        logger.info("BbEnterprise plugin disabled!")
    }
}
