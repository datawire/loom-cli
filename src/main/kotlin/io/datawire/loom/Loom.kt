package io.datawire.loom

import io.datawire.loom.core.Json
import io.datawire.loom.core.Jsonifier
import io.datawire.loom.core.LoomException
import io.datawire.loom.core.aws.AwsBootstrap
import io.datawire.loom.core.aws.AwsCloud
import io.datawire.loom.core.aws.createAwsCloud
import io.datawire.loom.core.validation.ValidationException
import io.datawire.loom.core.validation.Validator
import io.datawire.loom.fabric.*
import io.datawire.loom.fabric.FabricModelValidator
import io.datawire.loom.fabric.FabricParametersValidator
import io.datawire.loom.persistence.AmazonS3Dao
import io.datawire.loom.persistence.LoomRepository
import org.slf4j.LoggerFactory
import spark.Request
import spark.Response
import spark.Route
import java.io.FileInputStream
import java.nio.file.Files
import java.nio.file.Paths
import java.util.*
import spark.Spark.*

class Loom(
    private val port: Int,
    private val host: String = "",
    private val aws: AwsCloud,
    private val json: Json
) {

  private val log = LoggerFactory.getLogger(Loom::class.java)
  private val storage = LoomRepository(AmazonS3Dao(aws.stateStorageBucketName, aws.stateStorageClient), json)

  private val fabsrv = FabricService(aws, storage)

  private val JSON_CONTENT_TYPE = "application/json"
  private val jsonifier  = Jsonifier(json)

  private val fabricModelValidator  = FabricModelValidator(aws)
  private val fabricParamsValidator = FabricParametersValidator(storage)

  fun run() {
    port(port)
    ipAddress(host)

    initExceptionHandlers()
    initApi()

    log.info("== Loom has started ...")
    log.info(">> Listening on $host:${port()}")
  }

  // -------------------------------------------------------------------------------------------------------------------
  // Fabrics
  // -------------------------------------------------------------------------------------------------------------------

  private fun listFabricSpecs(req: Request, res: Response): List<FabricSpec> {
    res.header("Content-Type", "application/json")
    return fabsrv.listFabricSpecs()
  }

  private fun removeFabric(req: Request, res: Response) {

  }

  private fun addFabric(req: Request, res: Response): FabricSpec {
    val config = fromJson<FabricConfig>(req.body(), fabricParamsValidator)

    val spec = fabsrv.createFabric(config)
    res.header("Content-Type", "application/json")
    return spec
  }

  private fun fetchFabric(req: Request, res: Response): FabricSpec? {
    res.header("Content-Type", "application/json")
    return fabsrv.getFabricSpec(req.params(":name"))
  }

  private fun fetchKubernetesContext(req: Request, res: Response): String? {
    res.header("Content-Type", "application/yaml")
    return fabsrv.getClusterKubeconfig(req.params(":name"))
  }

  private fun registerResourceModel(req: Request, res: Response): ResourceModel? {
    val model = fromJson<ResourceModel>(req.body())
    res.header("Content-Type", "application/json")
    return storage.createBackingResourceModel(model)
  }

  private fun addResourceToFabric(req: Request, res: Response): FabricSpec {
    val config = fromJson<ResourceConfig>(req.body())
    res.header("Content-Type", "application/json")
    return fabsrv.addResourceToFabric(req.params(":name"), config)
  }

  private fun removeResourceFromFabric(req: Request, res: Response): FabricSpec {
    res.header("Content-Type", "application/json")
    return fabsrv.removeResourceFromFabric(req.params(":name"), req.params(":resource_name"))
  }

  // -------------------------------------------------------------------------------------------------------------------
  // Fabric Models
  // -------------------------------------------------------------------------------------------------------------------

  private fun createFabricModel(req: Request, res: Response): FabricModel {
    val model = fromJson<FabricModel>(req.body(), fabricModelValidator)
    res.header("Content-Type", "application/json")
    return fabsrv.createFabricModel(model)
  }

  private fun deactivateFabricModel(req: Request, res: Response): FabricModel {
    res.header("Content-Type", JSON_CONTENT_TYPE)
    return fabsrv.deactivateFabricModel(req.params(":name"))
  }

  private fun getFabricModel(req: Request, res: Response): FabricModel? {
    return storage.getFabricModelByName(req.params(":name"))?.apply {
      res.header("Content-Type", "application/json")
    }
  }

  private fun listFabricModels(req: Request, res: Response): List<FabricModel> {
    res.header("Content-Type", "application/json")
    return fabsrv.listFabricModels()
  }

  // -------------------------------------------------------------------------------------------------------------------
  // HTTP API
  // -------------------------------------------------------------------------------------------------------------------

  private fun initApi() {
    get("/health") { _, _ -> "" }
    log.info("== Registered health check endpoint (/health)")

    path("/api") {
      delete ("/fabrics",       JSON_CONTENT_TYPE, Route(this::removeFabric))
      get    ("/fabrics",       JSON_CONTENT_TYPE, Route(this::listFabricSpecs), jsonifier)
      get    ("/fabrics/:name", JSON_CONTENT_TYPE, Route(this::fetchFabric), jsonifier)
      post   ("/fabrics",       JSON_CONTENT_TYPE, Route(this::addFabric), jsonifier)

      get    ("/fabrics/:name/cluster/config", "application/yaml", Route(this::fetchKubernetesContext))

      post   ("/fabrics/:name/resources", JSON_CONTENT_TYPE, Route(this::addResourceToFabric))
      delete ("/fabrics/:name/resources/:resource_name", JSON_CONTENT_TYPE, Route(this::removeResourceFromFabric), jsonifier)

      delete ("/models/:name", JSON_CONTENT_TYPE, Route(this::deactivateFabricModel), jsonifier)
      get    ("/models/:name", JSON_CONTENT_TYPE, Route(this::getFabricModel), jsonifier)
      get    ("/models",       JSON_CONTENT_TYPE, Route(this::listFabricModels), jsonifier)
      post   ("/models",       JSON_CONTENT_TYPE, Route(this::createFabricModel), jsonifier)

      post   ("/resource-models", JSON_CONTENT_TYPE, Route(this::registerResourceModel), jsonifier)
    }

    log.info("== Registered API endpoints (/api*)")
  }

  private fun initExceptionHandlers() {
    notFound { _, res ->
      res.status(404)
      res.body()
    }

    exception(ValidationException::class.java) { ex, _, res ->
      log.error("Error validating incoming data", ex)

      res.apply {
        status(422)
        header("Content-Type", "application/json")
        body(json.write((ex as ValidationException).issues))
      }
    }

    internalServerError { req, res ->
      res.status(500)
    }

    exception(LoomException::class.java) { ex, _, res ->
      log.error("Error handling request", ex)

      //val (httpStatus, errors) = loomException.getStatusCodeAndErrorDetails()

      res.apply {
        status(ex.statusCode)


      }
    }
  }

  private inline fun <reified T: Any> fromJson(text: String, validator : Validator? = null): T {
    val jsonNode = json.toJsonNode(text)
    validator?.apply { validate(jsonNode) }
    return json.read(jsonNode)
  }
}

fun main(args: Array<String>) {
  configureProperties()

  val configFile = if (args.isNotEmpty()) Paths.get(args[0]) else Paths.get("config/loom.json")
  val config = Json().read<LoomConfig>(configFile)

  initLoomWorkspace()
  bootstrapLoom(config).run()
}

fun bootstrapLoom(config: LoomConfig): Loom {
  val aws = createAwsCloud(config.amazon)

  AwsBootstrap(aws).bootstrap()

  val loom = Loom(
      host = config.host,
      port = config.port,
      aws  = aws,
      json = Json()
  )

  return loom
}

private fun initLoomWorkspace() {
  Files.createDirectories(Paths.get(System.getProperty("user.home"), "loom-workspace"))
}

private fun configureProperties() {
  val props = Properties()
  props.load(FileInputStream("config/server.properties"))
  for ((name, value) in props) {
    System.setProperty(name.toString(), value.toString())
  }
}