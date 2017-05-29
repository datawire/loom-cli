package io.datawire.loomctl

import io.datawire.loom.core.Json
import io.datawire.loom.fabric.FabricSpec
import io.datawire.loom.terraform.Template
import io.datawire.loom.terraform.TemplateView
import org.slf4j.LoggerFactory
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths


/**
 * Interface for interacting with the filesystem where fabric configuration is manipulated and stored for processing.
 */
data class Workspace(val path: Path) {

  private val log = LoggerFactory.getLogger(Workspace::class.java)

  private val json = Json()

  val config  = json.read<LoomctlConfig>(path.resolve("loom.json"))

  val fabrics: Path = Files.createDirectories(resolve("fabrics"))

  private fun deleteDirectory(path: Path) {
    if (Files.isDirectory(path)) {
      Files.walk(path).sorted(Comparator.reverseOrder()).forEach { Files.deleteIfExists(it) }
    }
  }

  fun fabricPath(name: String): Path = fabrics.resolve(name)

  fun isFabric(name: String) = Files.exists(fabricPath(name))

  fun deleteFabric(name: String) {
    deleteDirectory(fabricPath(name))
  }

  fun getFabricSpec(name: String): FabricSpec? {
    return if (isFabric(name)) json.read(fabricPath(name).resolve("spec.json")) else null
  }

  fun createFabric(name: String) {
    Files.createDirectories(fabricPath(name))
  }

  fun loadTerraformTemplate(): Template? =
      try { json.read(read(resolve("terraform/main.tf.json"))) } catch (ioe: IOException) { null }

  fun exists(path: String): Boolean = Files.exists(this.path.resolve(path))

  fun resolve(path: String): Path = this.path.resolve(path)

  fun resolve(path: Path): Path = this.path.resolve(path)

  fun writeTerraformTemplate(fabric: String, name: String, template: Template) {
    val terraformJson = json.writeUsingView<TemplateView>(template)
    write(fabricPath(fabric).resolve("$name.tf.json"), terraformJson)
  }

  fun write(path: String, data: String) {
    Files.write(Paths.get(path), data.toByteArray())
  }

  fun write(path: Path, data: String) {
    Files.write(resolve(path), data.toByteArray())
    log.debug("""Wrote data to file: '{}'
{}""", path, data)
  }

  fun read(path: Path): String {
    val result = Files.newBufferedReader(resolve(path)).readText()
    log.debug("""Read data from file: '{}'" +
{}
""", path, result)

    return result
  }
}

fun getWorkspace(): Workspace? {
  val cwd = Paths.get(
      System.getProperty("loom.workspace", System.getProperty("user.dir")))

  return if (Files.exists(cwd.resolve("loom.json"))) Workspace(cwd) else null
}
