package configs

import com.google.gson.GsonBuilder

import java.io.{FileWriter, Writer}
import scala.io.Source

case class AvlJson(
    latestInsert: ErgoNamesInsert,
    manifestHex: String,
    digestHex: String,
    subTreeHex: Array[String]
)

case class ErgoNamesInsert(
    tokenId: String,
    index: Long,
    name: String
)

class AVLJsonHelper(
    tokenId: String,
    index: Long,
    name: String,
    manifestHex: String,
    digestHex: String,
    subTreeHex: Array[String]
) {
  private val gson = new GsonBuilder()
    .setPrettyPrinting()
    .create()

  private val latestInsert = ErgoNamesInsert(tokenId, index, name)

  private val conf = AvlJson(
    latestInsert,
    manifestHex,
    digestHex,
    subTreeHex
  )

  def getJsonString: String = {
    return gson.toJson(conf)
  }

  def read(filePath: String): AvlJson = {
    val jsonString: String = Source.fromFile(filePath).mkString
    gson.fromJson(jsonString, classOf[AvlJson])
  }

  def read: AvlJson = {
    gson.fromJson(gson.toJson(conf), classOf[AvlJson])
  }

  def write(filePath: String): Unit = {
    val writer: Writer = new FileWriter(filePath)
    writer.write(this.getJsonString)
    writer.close()
  }
}

object AVLJsonHelper {
  private val gson = new GsonBuilder()
    .setPrettyPrinting()
    .create()

  def read(filePath: String): AvlJson = {
    val jsonString: String = Source.fromFile(filePath).mkString
    gson
      .fromJson(jsonString, classOf[AvlJson])
  }

  def readJsonString(jsonString: String): AvlJson = {
    gson
      .fromJson(jsonString, classOf[AvlJson])
  }
}
