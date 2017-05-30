package io.datawire.loom.persistence

import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import kotlin.streams.toList


class FilesystemDao(private val contentRoot: Path) : Dao<String> {

  override fun create(id: String, model: String) = update(id, model)

  override fun delete(id: String): Boolean {
    val target = contentRoot.resolve(id)
    return try {
      if (Files.isDirectory(target)) {
        Files.walk(target).sorted(Comparator.reverseOrder()).forEach { Files.deleteIfExists(it) }
      } else {
        Files.deleteIfExists(target)
      }
      true
    } catch (ioe: IOException) {
      false
    }
  }

  override fun getById(id: String): String? = readObjectOrNull(id)

  override fun update(id: String, model: String): String {
    Files.write(contentRoot.resolve(id), model.toByteArray())
    return model
  }

  override fun exists(id: String) = Files.exists(contentRoot.resolve(id))

  override fun notExists(id: String) = !exists(id)

  override fun listObjectIds(prefix: String): List<String> {
    return Files.walk(contentRoot.resolve(prefix)).toList().map { it.toString() }
  }

  override fun listObjects(prefix: String) = emptyList<Record<String>>()

  private fun formatObjectKey(id: String) = id.toLowerCase()

  private fun readObjectOrNull(id: String) =
      try { Files.newBufferedReader(contentRoot.resolve(id)).use { it.readText() } } catch (any: Throwable) { null }

}