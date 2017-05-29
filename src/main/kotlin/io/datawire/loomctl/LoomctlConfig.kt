package io.datawire.loomctl


data class LoomctlConfig(
    val configRepositoryUrl: String,
    val stateStore: String,
    val defaults: Map<String, String?> = emptyMap()
)
