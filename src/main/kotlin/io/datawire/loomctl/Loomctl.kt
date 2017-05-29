package io.datawire.loomctl


import net.sourceforge.argparse4j.inf.ArgumentParser
import net.sourceforge.argparse4j.inf.ArgumentParserException
import net.sourceforge.argparse4j.inf.Namespace
import org.eclipse.jgit.api.Git
import java.nio.file.Files
import java.nio.file.Path
import kotlin.system.exitProcess

enum class Command {
  DELETE_FABRIC
}

fun main(args: Array<String>) {
  getWorkspace()
      ?.let { ws ->
        val config = ConfigWorkspace(cloneGit(ws.config.configRepositoryUrl))
        val argParser = createArgParser(ws, config)
        try {
          processCommand(argParser.parseArgs(args))
        } catch (ape: ArgumentParserException) {
          argParser.handleError(ape)
        }
      }
      ?: handleWorkspaceNotFound()
}

private fun processCommand(namespace: Namespace) {
  println("-- DEBUG => $namespace")

  when(namespace.getString("command")) {
    "create-fabric"       -> createFabric(namespace)
    "delete-fabric"       -> deleteFabric(namespace)
    "get-cluster-kubecfg" -> getClusterKubeconfig(namespace)
    "get-cluster-status"  -> getClusterStatus(namespace)
  }
}

private fun getClusterKubeconfig(ns: Namespace) {
  val fabricName = ns.getString("name")
  val workspace  = ns.get<Workspace>("workspace")

  workspace.getFabricSpec(fabricName)?.let { spec ->
    val kops = Kops.newKops(workspace.path, workspace.config.stateStore)
    kops.exportClusterContext(spec.clusterDomain)?.let { kubeconfig -> println(kubeconfig) }
  }
}

private fun getClusterStatus(ns: Namespace) {
  val fabricName = ns.getString("name")
  val workspace  = ns.get<Workspace>("workspace")

  workspace.getFabricSpec(fabricName)?.let { spec ->
    val kops = Kops.newKops(workspace.path, workspace.config.stateStore)
    kops.exportClusterContext(spec.clusterDomain)?.let { kubeconfig ->

    }
  }
}

private fun deleteFabric(ns: Namespace) {
  val fabricName = ns.getString("name")
  val workspace  = ns.get<Workspace>("workspace")

  if (workspace.isFabric(fabricName)) {
    println("Fabric named '$fabricName' found. Deleting configuration...")
    workspace.deleteFabric(fabricName)
    println("Fabric named '$fabricName' removed. Commit and push changes to Git to complete the operation.")
  } else {
    println("Fabric named '$fabricName' not found. No work to perform and no further user interaction is required.")
  }
}

private fun createFabric(ns: Namespace) {
  val fabricName  = ns.getString("name")
  val workspace   = ns.get<Workspace>("workspace")
  val configWs    = ns.get<ConfigWorkspace>("config")

  val fabricModel = ns.getString("model")

  if (fabricModel == null) {
    println("Fabric model not configured. Either pass in --model or set default in loom.json")
    exitProcess(1)
  }

  if (!configWs.isFabricModel(fabricModel)) {
    println("Fabric model named '$fabricModel' does not exist.")
    exitProcess(1)
  }

  if (!workspace.isFabric(fabricName)) {
    println("Fabric named '$fabricName' not found. Creating fabric using configuration model: $fabricModel")
    workspace.createFabric(fabricName)

    println("Fabric named '$fabricName' configured. Commit and push changes to Git to complete the operation.")
  } else {
    println("Fabric named '$fabricName' found. No work to perform and no further user interaction is required.")
  }
}

private fun handleWorkspaceNotFound() {
  println("Loom workspace not found at '${System.getProperty("user.dir")}'. The loom workspace is the directory that contains a Loom config 'loom.json'.")
  System.exit(1)
}

private fun cloneGit(repositoryUrl: String): Path {
  val name = repositoryUrl.substringAfter("/").substringBefore(".")
  val clonePath = Files.createTempDirectory("$name-")

  println("Cloning repository (from: $repositoryUrl, into: {})")
  val clone = Git.cloneRepository()
      .setURI(repositoryUrl)
      .setDirectory(clonePath.toFile())
      .setBranch("master")

  clone.call().use { git -> git.checkout().setName("master").call() }

  return clonePath
}

private fun createArgParser(workspace: Workspace, configWorkspace: ConfigWorkspace): ArgumentParser {
  val parser = net.sourceforge.argparse4j.ArgumentParsers
      .newArgumentParser("loomctl")
      .description("Manage Loom fabric configuration")

  parser.apply {
    addArgument("--workspace").default = workspace
    addArgument("--config").default    = configWorkspace
  }

  val commands = parser.addSubparsers().dest("command")

  val deleteCommand = commands.addParser("delete-fabric").apply {
    addArgument("name")
  }

  val createCommand = commands.addParser("create-fabric").apply {
    addArgument("name")

    addArgument("--model")
    setDefault("model", workspace.config.defaults["fabricModel"])
  }

  val clusterStatus = commands.addParser("get-cluster-status").apply { addArgument("name") }
  val clusterConfig = commands.addParser("get-cluster-kubeconfig").apply { addArgument("name") }

  val addBackingService = commands.addParser("add-backing-service").apply { addArgument("name") }
  val removeBackingService = commands.addParser("remove-backing-service").apply { addArgument("name") }

  return parser
}

