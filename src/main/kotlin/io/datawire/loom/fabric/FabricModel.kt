package io.datawire.loom.fabric

import com.fasterxml.jackson.annotation.JsonFormat
import java.time.Instant

data class FabricModel(
    val active: Boolean = true,
    val domain: String,

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ssZ", timezone = "UTC")
    val creationTime: Instant?,

    val masterNodes: NodeGroup,
    val name: String,
    val externalServicesNetwork: ExternalServicesNetworkModel,
    val region: String,
    val sshPublicKey: String,
    val workerNodes: List<NodeGroup>
)