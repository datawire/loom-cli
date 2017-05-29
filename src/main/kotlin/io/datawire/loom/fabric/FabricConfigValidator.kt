package io.datawire.loom.fabric

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.JsonNodeType
import io.datawire.loom.core.validation.*
import io.datawire.loom.persistence.LoomRepository
import org.apache.commons.net.util.SubnetUtils


private val NAME_REGEX = Regex("[a-z][a-z0-9_]{0,31}")

class FabricParametersValidator(private val storage: LoomRepository) : Validator() {

  override fun validate(root: JsonNode) {
    val issues = mutableListOf<ValidationIssue>()

    root.matches(field("/name"), NAME_REGEX)?.let { issues += it }

    root.validate(
        field("/name"),
        nullable = false,
        type     = JsonNodeType.STRING,
        check    = { !storage.fabricSpecExists(textValue()) },
        failed   = issue("Name Already Claimed", "Fabric name is already claimed")
    )?.let { issues += it }

    root.validate(
        field("/clusterCidr"),
        nullable = true,
        type     = JsonNodeType.STRING,
        check    = { try { SubnetUtils(textValue()); true } catch(illegal: IllegalArgumentException) { false } },
        failed   = issue("Invalid VPC CIDR", "The cluster VPC CIDR is invalid.")
    )?.let { issues += it }

    root.validate(
        field("/resourcesCidr"),
        nullable = true,
        type     = JsonNodeType.STRING,
        check    = { try { SubnetUtils(textValue()); true } catch(illegal: IllegalArgumentException) { false } },
        failed   = issue("Invalid VPC CIDR", "The resources VPC CIDR is invalid.")
    )?.let { issues += it }

    root.validate(
        field("/model"),
        nullable = false,
        type     = JsonNodeType.STRING,
        check    = { storage.fabricModelExists(textValue()) },
        failed   = issue("Model Does Not Exist", "Specified fabric model does not exist")
    )?.let { issues += it }

    if (issues.isNotEmpty()) {
      throw ValidationException(issues)
    }
  }
}