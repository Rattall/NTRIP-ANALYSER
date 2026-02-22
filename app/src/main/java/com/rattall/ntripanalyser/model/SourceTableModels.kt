package com.rattall.ntripanalyser.model

data class SourceTableEntry(
    val mountpoint: String,
    val identifier: String,
    val format: String,
    val navSystem: String,
    val country: String,
    val latitude: Double?,
    val longitude: Double?
)
