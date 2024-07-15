package configs

import com.google.gson.GsonBuilder

import scala.io.Source

case class Tx(
    poolId: String,
    poolFeeNum: Long,
    tokenIdIn: String,
    tokenIdOut: String,
    amountToSwap: Long
)

object Tx {
  private val gson = new GsonBuilder().setPrettyPrinting().create()

  def read(filePath: String): Tx = {
    val jsonString: String = Source.fromFile(filePath).mkString
    gson.fromJson(jsonString, classOf[Tx])
  }
}
