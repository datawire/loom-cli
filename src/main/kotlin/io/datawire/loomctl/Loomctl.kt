package io.datawire.loomctl


import io.datawire.loom.core.Json
import io.datawire.loom.core.Yaml
import io.datawire.loom.persistence.FilesystemDao
import io.datawire.loom.terraform.Difference
import net.sourceforge.argparse4j.inf.ArgumentParser
import net.sourceforge.argparse4j.inf.ArgumentParserException
import net.sourceforge.argparse4j.inf.Namespace
import org.eclipse.jgit.api.Git
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.system.exitProcess

private val loomctlContentRoot   = Paths.get(System.getProperty("user.home"), ".loomctl")
private val loomctlExternalTools = loomctlContentRoot.resolve("bin")

fun main(args: Array<String>) {
  downloadTools(loomctlExternalTools)

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

  val workspace = namespace.get<Workspace>("workspace")

  val configDao = FilesystemDao(namespace.get<ConfigWorkspace>("config").path)
  val fabricDao = FilesystemDao(workspace.path)

  val loomRepo = LoomRepository(configDao, fabricDao, Json(), Yaml())

  loomRepo.getBackingResourceModelByName("rds-postgresql-standalone-v1")

  when(namespace.getString("command")) {
    "plan" -> {
      val fabricName = namespace.getString("name")
      val fabricWorkspace = FabricWorkspace(workspace.resolve("fabrics/$fabricName"))
      val tf = Terraform.newTerraform(fabricWorkspace)
      val plan = tf.plan()
      println(plan)
    }

    "apply"                  -> {
      val fabricName = namespace.getString("name")
      val fabricWorkspace = FabricWorkspace(workspace.resolve("fabrics/$fabricName"))
      val tf = Terraform.newTerraform(fabricWorkspace)
      val plan = tf.plan()
      when(plan) {
        is Difference -> tf.apply(plan)
        else          -> { println(plan) }
      }
    }

    "create-fabric"          -> createFabric(namespace)
    "delete-fabric"          -> deleteFabric(namespace)
    "get-backing-service-model" -> {
      val modelName = namespace.getString("name")
      val msg = loomRepo.getBackingResourceModelByName(modelName) ?: "backing service model not found: $modelName"
      println(msg)
    }
    "add-backing-service"    -> {}
    "remove-backing-service" -> {}
    "get-cluster-kubeconfig" -> getClusterKubeconfig(namespace)
    "get-cluster-status"     -> getClusterStatus(namespace)
  }
}

private fun getClusterKubeconfig(ns: Namespace) {
  val fabricName = ns.getString("name")
  val workspace  = ns.get<Workspace>("workspace")

  val res = workspace.getFabricSpec(fabricName)?.let { spec ->
    val kops = Kops.newKops(workspace.path, workspace.config.stateStore)
    kops.exportClusterContext(spec.clusterDomain)?.let { kubeconfig -> println(kubeconfig) }
  } ?: "Fabric not found: $fabricName"

  println(res)
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

  println("Cloning repository (from: $repositoryUrl, into: $clonePath)")
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

  val applyCommand = commands.addParser("apply").apply {
    addArgument("name")
  }

  val planCommand = commands.addParser("plan").apply {
    addArgument("name")
  }

  val deleteCommand = commands.addParser("delete-fabric").apply {
    addArgument("name")
  }

  val createCommand = commands.addParser("create-fabric").apply {
    addArgument("name")

    addArgument("--model")
    setDefault("model", workspace.config.defaults["fabricModel"])
  }

  val getBackingServiceModelInfo = commands.addParser("get-backing-service-model").apply {
    addArgument("name")
  }

  val clusterStatus = commands.addParser("get-cluster-status").apply { addArgument("name") }
  val clusterConfig = commands.addParser("get-cluster-kubeconfig").apply { addArgument("name") }

  val addBackingService = commands.addParser("add-backing-service").apply { addArgument("name") }
  val removeBackingService = commands.addParser("remove-backing-service").apply { addArgument("name") }

  return parser
}

