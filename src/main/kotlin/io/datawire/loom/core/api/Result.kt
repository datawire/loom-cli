package io.datawire.loom.core.api

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.databind.ser.std.StdSerializer
import io.datawire.loom.core.Json
import org.atteo.evo.inflector.English


@JsonSerialize(using = ResultSerializer::class)
class Result(val name: String, val item: Any?)

@JsonSerialize(using = ResultsSerializer::class)
class Results(val name: String, val items: List<*>)


class ResultSerializer : StdSerializer<Result>(Result::class.java) {
  override fun serialize(result: Result, gen: JsonGenerator, provider: SerializerProvider) {
    with(gen) {
      writeStartObject()
      writeFieldName(result.name)
      writeObject(result.item)
      writeEndObject()
    }
  }
}

class ResultsSerializer : StdSerializer<Results>(Results::class.java) {
  override fun serialize(result: Results, gen: JsonGenerator, provider: SerializerProvider) {
    with(gen) {
      writeStartObject()
      writeFieldName(English.plural(result.name))
      writeObject(result.items)
      writeEndObject()
    }
  }
}

fun main(args: Array<String>) {

  data class Model(val name: String)

  val json = Json()
  println(json.write(Result("model", emptyList<Model>())))
}
