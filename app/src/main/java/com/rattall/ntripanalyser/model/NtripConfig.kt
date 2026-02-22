package com.rattall.ntripanalyser.model

data class NtripConfig(
    val host: String,
    val port: Int,
    val username: String,
    val password: String,
    val mountpoint: String,
    val useTls: Boolean,
    val protocol: NtripProtocol
)

enum class NtripProtocol {
    REV1,
    REV2
}
