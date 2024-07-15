package configs

import com.google.gson.GsonBuilder

import scala.io.Source

case class Config(
    txOperatorMnemonic: String,
    txOperatorMnemonicPw: String,
    txOperatorFee: Long,
    minBoxValue: Long,
    addressIndex: Int,
    minerFeeNanoERG: Long,
    nodeUrl: String,
    apiUrl: String,
    explorerBaseURI: String
)

object Conf {
  private val gson = new GsonBuilder().setPrettyPrinting().create()

  def read(filePath: String): Config = {
    val jsonString: String = Source.fromFile(filePath).mkString
    gson.fromJson(jsonString, classOf[Config])
  }
}
