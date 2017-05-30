package io.datawire.loomctl

import io.datawire.loom.core.Yaml
import io.datawire.loom.fabric.FabricModel
import java.nio.file.Files
import java.nio.file.Path


class ConfigWorkspace(val path: Path) {

  fun isFabricModel(name: String): Boolean =
      Files.isRegularFile(path.resolve("loom/fabric-models/$name.yaml"))

  fun getFabricModel(name: String): FabricModel? =
      Yaml().read(path.resolve("loom/fabric-models/$name.yaml"))

}