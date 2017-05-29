package io.datawire.loom.core



import org.eclipse.jetty.http.HttpStatus.*


enum class Error(
    val id: Int,
    val description: String,
    val httpStatusCode: Int
) {

  // -----------------------------------------------------------------------------------------------------------------
  // Error related to internal processing have the lowest range (1..999)
  // -----------------------------------------------------------------------------------------------------------------

  GENERAL_ERROR   (1, "Error of unspecified origin or type occurred", INTERNAL_SERVER_ERROR_500),
  NOT_IMPLEMENTED (2, "Unimplemented resource or operation", NOT_IMPLEMENTED_501),

  // -----------------------------------------------------------------------------------------------------------------
  // Fabric Model errors
  // -----------------------------------------------------------------------------------------------------------------

  MODEL_NOT_EXISTS (1000, "The fabric model '%s' does not exist", NOT_FOUND_404),
  MODEL_EXISTS     (1001, "The fabric model '%s' already exists", CONFLICT_409),
  MODEL_INVALID    (1002, "The fabric model '%s' definition provided is invalid", UNPROCESSABLE_ENTITY_422),

  // -----------------------------------------------------------------------------------------------------------------
  // Fabric errors
  // -----------------------------------------------------------------------------------------------------------------

  FABRIC_NOT_EXISTS (1100, "The fabric '%s' does not exist", NOT_FOUND_404),
  FABRIC_EXISTS     (1101, "The fabric '%s' already exists", CONFLICT_409),
  FABRIC_INVALID    (1102, "The fabric '%s' definition provided is invalid", UNPROCESSABLE_ENTITY_422),

  // -----------------------------------------------------------------------------------------------------------------
  // Cluster errors
  // -----------------------------------------------------------------------------------------------------------------

  CLUSTER_NOT_FOUND (1200, "The cluster '%s' does not exist", NOT_FOUND_404),
  CLUSTER_EXISTS    (1201, "The cluster '%s' already exists", NOT_FOUND_404);
}

