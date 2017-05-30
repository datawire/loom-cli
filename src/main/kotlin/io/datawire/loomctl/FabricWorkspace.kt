package io.datawire.loomctl

import com.fasterxml.jackson.databind.JsonNode
import io.datawire.loom.core.Json
import io.datawire.loom.fabric.FabricSpec
import io.datawire.loom.terraform.Template
import org.slf4j.LoggerFactory
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths


data class FabricWorkspace(val root: Path) {

  private val log = LoggerFactory.getLogger(FabricWorkspace::class.java)

  private val json = Json()

  fun readTerraformState(): JsonNode = json.read(read(Paths.get("terraform.tfstate")))

  fun readTerraformTemplate(path: Path): Template = json.read(read(path))

  fun readFabricSpecification(): FabricSpec = json.read(root.resolve("fabric.json"))

  fun deleteFile(path: Path): Boolean {
    val target = root.resolve(path)
    return if (!Files.isDirectory(target)) {
      Files.deleteIfExists(target)
    } else {
      throw IllegalArgumentException("Target path '$target' is a directory but must be a file.")
    }
  }

  fun deleteDirectory(path: Path) {
    val target = root.resolve(path)
    Files.walk(target).sorted(Comparator.reverseOrder()).forEach { Files.deleteIfExists(it) }
  }

  fun write(path: String, data: String) {
    Files.write(Paths.get(path), data.toByteArray())
  }

  fun write(path: Path, data: String) {
    Files.write(root.resolve(path), data.toByteArray())
    log.debug("""Wrote data to file: '{}'
{}""", path, data)
  }

  fun read(path: Path): String {
    val result = Files.newBufferedReader(root.resolve(path)).readText()
    log.debug("""Read data from file: '{}'" +
{}
""", path, result)

    return result
  }
}