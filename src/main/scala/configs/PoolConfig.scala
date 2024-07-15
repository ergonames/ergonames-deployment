package configs

import com.google.gson.reflect.TypeToken
import com.google.gson.{Gson, GsonBuilder}
import scala.io.Source
import java.io.{FileWriter, Writer}
import scala.jdk.CollectionConverters._

case class PoolPair(
    tokenId: String,
    poolTokenId: String
)

object PoolConfig {
  private val gson: Gson = new GsonBuilder().setPrettyPrinting().create()

  def read(filePath: String): List[PoolPair] = {
    val jsonString: String = Source.fromFile(filePath).mkString
    readJsonString(jsonString)
  }

  def readJsonString(jsonString: String): List[PoolPair] = {
    val listType = new TypeToken[java.util.List[PoolPair]]() {}.getType
    gson.fromJson[java.util.List[PoolPair]](jsonString, listType).asScala.toList
  }

  def write(filePath: String, poolPairs: List[PoolPair]): Unit = {
    val writer: Writer = new FileWriter(filePath)
    writer.write(gson.toJson(poolPairs))
    writer.close()
  }

  def writeJsonString(poolPairs: List[PoolPair]): String = {
    gson.toJson(poolPairs)
  }
}
