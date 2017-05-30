package io.datawire.loomctl

import io.datawire.loom.core.Json
import io.datawire.loom.core.Yaml
import io.datawire.loom.fabric.FabricModel
import io.datawire.loom.fabric.FabricSpec
import io.datawire.loom.fabric.ResourceModel
import io.datawire.loom.persistence.Dao


class LoomRepository(
    private val configDao: Dao<String>,
    private val fabricDao: Dao<String>,
    private val json: Json,
    private val yaml: Yaml
) {

  private val fabricModelsPrefix          = "models/fabrics/"
  private val backingResourceModelsPrefix = "models/backing-services/"
  private val fabricSpecsPrefix           = "fabrics/"

  fun createFabricModel(model: FabricModel): FabricModel {
    if (!fabricModelExists(model.name)) {
      configDao.create(fabricModelKey(model.name), yaml.write(model))
    }
    
    return model
  }
  
  fun createFabricSpec(spec: FabricSpec): FabricSpec {
    if (!fabricSpecExists(spec.name)) {
      fabricDao.create(fabricSpecKey(spec.name), json.write(spec))
    }

    return spec
  }
  
  fun createBackingResourceModel(model: ResourceModel): ResourceModel {
    if (!backingResourceModelExists(model.name)) {
      configDao.create(backingResourcesModelKey(model.name), yaml.write(model))
    }

    return model
  }

  fun deleteFabricModel(name: String) = configDao.delete(fabricModelKey(name))

  fun deleteFabricSpec(name: String) = fabricDao.delete(fabricSpecKey(name))
  
  fun deleteBackingServiceModel(name: String) = configDao.delete(backingResourcesModelKey(name))

  fun fabricModelExists(name: String) = configDao.exists(fabricModelKey(name))
  
  fun fabricSpecExists(name: String) = fabricDao.exists(name)
  
  fun backingResourceModelExists(name: String) = configDao.exists(name)
  
  fun getFabricModelByName(name: String): FabricModel? = configDao.getById(fabricModelKey(name))?.let { json.read(it) }

  fun getFabricSpecByName(name: String): FabricSpec? = configDao.getById(fabricSpecKey(name))?.let { json.read(it) }

  fun getBackingResourceModelByName(name: String): ResourceModel?
      = configDao.getById(backingResourcesModelKey(name))?.let { yaml.read(it) }

  fun listFabricModels() = configDao.listObjects(prefix = fabricModelsPrefix).map { json.read<FabricModel>(it.data) }

  fun listFabricSpecs() = fabricDao.listObjects(prefix = fabricSpecsPrefix)
      .filter { it.name.endsWith(".spec") }
      .map    { json.read<FabricSpec>(it.data) }

  fun listBackingResourceModels()
      = configDao.listObjects(prefix = backingResourceModelsPrefix).map { json.read<ResourceModel>(it.data) }

  fun listFabricModelNames()
      = configDao.listObjectIds(prefix = fabricModelsPrefix).map { it.removePrefix(fabricModelsPrefix) }

  fun listFabricSpecNames()
      = fabricDao.listObjectIds(prefix = fabricSpecsPrefix).map { it.removePrefix(fabricSpecsPrefix) }

  fun listBackingResourceModelNames()
      = configDao.listObjectIds(prefix = backingResourceModelsPrefix).map { it.removePrefix(backingResourceModelsPrefix) }

  fun updateFabricModel(model: FabricModel): Boolean {
    return if (fabricModelExists(model.name)) {
      configDao.update(fabricModelKey(model.name), yaml.write(model))
      true
    } else {
      false
    }
  }

  fun updateFabricSpec(spec: FabricSpec): Boolean {
    return if (fabricSpecExists(spec.name)) {
      fabricDao.update(fabricSpecKey(spec.name), json.write(spec))
      true
    } else {
      false
    }
  }

  fun updateBackingResourceModel(model: ResourceModel): Boolean {
    return if (backingResourceModelExists(model.name)) {
      configDao.update(backingResourcesModelKey(model.name), yaml.write(model))
      true
    } else {
      false
    }
  }

  private fun formatObjectKey(id: String, prefix: String = "", suffix: String = "") = "$prefix$id$suffix".toLowerCase()
  
  private fun fabricModelKey(name: String) 
      = formatObjectKey(name, prefix = fabricModelsPrefix, suffix = ".yaml")
  
  private fun backingResourcesModelKey(name: String)
      = formatObjectKey(name, prefix = backingResourceModelsPrefix, suffix = ".yaml")
  
  private fun fabricSpecKey(name: String) 
      = formatObjectKey(name, prefix = fabricSpecsPrefix, suffix = ".json")
}