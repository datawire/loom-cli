package io.datawire.loom.persistence


import com.amazonaws.services.s3.AmazonS3


class AmazonS3Dao(
    private val bucket: String,
    private val s3: AmazonS3
) : Dao<String> {

  override fun create(id: String, model: String) = update(id, model)

  override fun delete(id: String): Boolean {
    s3.deleteObject(bucket, formatObjectKey(id))
    return true
  }

  override fun getById(id: String): String? = readObjectOrNull(id)

  override fun update(id: String, model: String): String {
    s3.putObject(bucket, formatObjectKey(id), model)
    return model
  }

  override fun exists(id: String) = s3.doesObjectExist(bucket, id)

  override fun notExists(id: String) = !exists(id)

  override fun listObjectIds(prefix: String): List<String> {
    val result = s3.listObjectsV2(bucket, formatObjectKey(prefix))
    return result.objectSummaries.map { it.key }
  }

  override fun listObjects(prefix: String) =
      s3.listObjectsV2(bucket, formatObjectKey(prefix)).objectSummaries.mapNotNull { obj ->
        readObjectOrNull(obj.key)?.let { Record(obj.key, it) }
      }


  private fun formatObjectKey(id: String) = id.toLowerCase()

  private fun readObjectOrNull(id: String) =
      try { s3.getObjectAsString(bucket, formatObjectKey(id)) } catch (any: Throwable) { null }

}