package io.datawire.loom.persistence

import io.datawire.loom.core.Json
import io.datawire.loom.fabric.FabricModel
import io.datawire.loom.fabric.FabricSpec
import io.datawire.loom.fabric.ResourceModel


class LoomRepository(
    private val dao: Dao<String>,
    private val json: Json
): Dao<String> by dao {

  private val fabricModelsPrefix          = "models/fabrics/"
  private val backingResourceModelsPrefix = "models/backing-resources/"
  private val fabricSpecsPrefix           = "fabrics/"

  fun createFabricModel(model: FabricModel): FabricModel {
    if (!fabricModelExists(model.name)) {
      dao.create(fabricModelKey(model.name), json.write(model))
    }
    
    return model
  }
  
  fun createFabricSpec(spec: FabricSpec): FabricSpec {
    if (!fabricSpecExists(spec.name)) {
      dao.create(fabricSpecKey(spec.name), json.write(spec))
    }

    return spec
  }
  
  fun createBackingResourceModel(model: ResourceModel): ResourceModel {
    if (!backingResourceModelExists(model.name)) {
      dao.create(backingResourcesModelKey(model.name), json.write(model))
    }

    return model
  }

  fun deleteFabricModel(name: String) = delete(fabricModelKey(name))

  fun deleteFabricSpec(name: String) = delete(fabricSpecKey(name))
  
  fun deleteBackingServiceModel(name: String) = delete(backingResourcesModelKey(name))

  fun fabricModelExists(name: String) = exists(fabricModelKey(name))
  
  fun fabricSpecExists(name: String) = exists(name)
  
  fun backingResourceModelExists(name: String) = exists(name)
  
  fun getFabricModelByName(name: String): FabricModel? = getById(fabricModelKey(name))?.let { json.read(it) }

  fun getFabricSpecByName(name: String): FabricSpec? = getById(fabricSpecKey(name))?.let { json.read(it) }

  fun getBackingResourceModelByName(name: String): ResourceModel?
      = getById(backingResourcesModelKey(name))?.let { json.read(it) }

  fun listFabricModels() = listObjects(prefix = fabricModelsPrefix).map { json.read<FabricModel>(it.data) }

  fun listFabricSpecs() = listObjects(prefix = fabricSpecsPrefix)
      .filter { it.name.endsWith(".spec") }
      .map    { json.read<FabricSpec>(it.data) }

  fun listBackingResourceModels()
      = listObjects(prefix = backingResourceModelsPrefix).map { json.read<ResourceModel>(it.data) }

  fun listFabricModelNames()
      = listObjectIds(prefix = fabricModelsPrefix).map { it.removePrefix(fabricModelsPrefix) }

  fun listFabricSpecNames()
      = listObjectIds(prefix = fabricSpecsPrefix).map { it.removePrefix(fabricSpecsPrefix) }

  fun listBackingResourceModelNames()
      = listObjectIds(prefix = backingResourceModelsPrefix).map { it.removePrefix(backingResourceModelsPrefix) }

  fun updateFabricModel(model: FabricModel): Boolean {
    return if (fabricModelExists(model.name)) {
      dao.update(fabricModelKey(model.name), json.write(model))
      true
    } else {
      false
    }
  }

  fun updateFabricSpec(spec: FabricSpec): Boolean {
    return if (fabricSpecExists(spec.name)) {
      dao.update(fabricSpecKey(spec.name), json.write(spec))
      true
    } else {
      false
    }
  }

  fun updateBackingResourceModel(model: ResourceModel): Boolean {
    return if (backingResourceModelExists(model.name)) {
      dao.update(backingResourcesModelKey(model.name), json.write(model))
      true
    } else {
      false
    }
  }

  private fun formatObjectKey(id: String, prefix: String = "", suffix: String = "") = "$prefix$id$suffix".toLowerCase()
  
  private fun fabricModelKey(name: String) 
      = formatObjectKey(name, prefix = fabricModelsPrefix)
  
  private fun backingResourcesModelKey(name: String)
      = formatObjectKey(name, prefix = backingResourceModelsPrefix)
  
  private fun fabricSpecKey(name: String) 
      = formatObjectKey(name, prefix = fabricSpecsPrefix, suffix = ".spec")
}