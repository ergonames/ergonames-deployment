package configs

import com.google.gson.GsonBuilder
import scala.io.Source

case class RegistryCompileMetadata(
    subnameRegistryContractAddress: String,
    issuerContractAddress: String,
    feeContractAddress: String,
    configContractAddress: String,
    configSingletonTokenId: String,
    sigUsdOracleSingletonTokenId: String,
    singletonTokenId: String,
    minCommitBoxAge: Int,
    maxCommitBoxAge: Int
)

object RegistryCompileMetadata {
  private val gson = new GsonBuilder()
    .setPrettyPrinting()
    .create()

  def read(filePath: String): RegistryCompileMetadata = {
    val jsonString: String = Source.fromFile(filePath).mkString
    gson.fromJson(jsonString, classOf[RegistryCompileMetadata])
  }

  def readJsonString(jsonString: String): RegistryCompileMetadata = {
    gson.fromJson(jsonString, classOf[RegistryCompileMetadata])
  }
}
