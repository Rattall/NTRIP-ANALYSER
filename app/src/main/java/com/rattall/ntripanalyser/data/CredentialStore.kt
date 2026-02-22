package com.rattall.ntripanalyser.data

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.rattall.ntripanalyser.model.NtripConfig
import com.rattall.ntripanalyser.model.NtripProtocol

class CredentialStore(context: Context) {
    private val prefs = EncryptedSharedPreferences.create(
        context,
        "ntrip_secure_prefs",
        MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun save(config: NtripConfig) {
        prefs.edit()
            .putString("host", config.host)
            .putInt("port", config.port)
            .putString("username", config.username)
            .putString("password", config.password)
            .putString("mountpoint", config.mountpoint)
            .putBoolean("useTls", config.useTls)
            .putString("protocol", config.protocol.name)
            .apply()
    }

    fun load(): NtripConfig? {
        val host = prefs.getString("host", null) ?: return null
        val username = prefs.getString("username", null) ?: return null
        val password = prefs.getString("password", null) ?: return null
        val mountpoint = prefs.getString("mountpoint", null) ?: return null

        return NtripConfig(
            host = host,
            port = prefs.getInt("port", 2101),
            username = username,
            password = password,
            mountpoint = mountpoint,
            useTls = prefs.getBoolean("useTls", true),
            protocol = runCatching {
                NtripProtocol.valueOf(prefs.getString("protocol", NtripProtocol.REV2.name)!!)
            }.getOrDefault(NtripProtocol.REV2)
        )
    }
}
