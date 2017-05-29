package io.datawire.loom.persistence


interface Dao<T: Any> {
  fun create(id: String, model: T): T
  fun delete(id: String): Boolean
  fun getById(id: String): T?
  fun update(id: String, model: T): T
  fun exists(id: String): Boolean
  fun notExists(id: String): Boolean
  fun listObjectIds(prefix: String): List<String>
  fun listObjects(prefix: String): List<Record<T>>
}

data class Record<out T: Any>(val name: String, val data: T)