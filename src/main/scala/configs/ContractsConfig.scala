package configs

import com.google.gson.GsonBuilder
import java.io.{FileWriter, Writer}
import scala.io.Source

case class ContractsConfig(
    contracts: ContractInfo
)

case class ContractInfo(
    registryContract: RegistryContract,
    configContract: String,
    issuerContract: String,
    subnameRegistryContract: String,
    commitContract: String,
    revealContract: String
)

case class RegistryContract(
    contract: String,
    singleton: String,
    collectionToken: String,
    configSingletonToken: String,
    sigUsdOracleSingletonTokenId: String,
    minCommitBoxAge: Int,
    maxCommitBoxAge: Int
)

class ContractsConfigManager(
    registryContract: String,
    singleton: String,
    collectionToken: String,
    configContract: String,
    configSingletonToken: String,
    sigUsdOracleSingletonTokenId: String,
    minCommitBoxAge: Int,
    maxCommitBoxAge: Int,
    issuerContract: String,
    subnameRegistryContract: String,
    commitContract: String,
    revealContract: String
) {
  private val registryContractInstance: RegistryContract =
    RegistryContract(
      registryContract,
      singleton,
      collectionToken,
      configSingletonToken,
      sigUsdOracleSingletonTokenId,
      minCommitBoxAge,
      maxCommitBoxAge
    )

  val conf: ContractInfo = ContractInfo(
    registryContractInstance,
    configContract,
    issuerContract,
    subnameRegistryContract,
    commitContract,
    revealContract
  )
  private val newConfig: ContractsConfig = ContractsConfig(conf)
  private val gson = new GsonBuilder().setPrettyPrinting().create()

  def write(filePath: String): Unit = {
    val writer: Writer = new FileWriter(filePath)
    writer.write(this.gson.toJson(this.newConfig))
    writer.close()
  }

  def read(filePath: String): ContractsConfig = {
    val jsonString: String = Source.fromFile(filePath).mkString
    gson.fromJson(jsonString, classOf[ContractsConfig])
  }
}

object ContractsConfigManager {
  private val gson = new GsonBuilder().setPrettyPrinting().create()

  def read(filePath: String): ContractsConfig = {
    val jsonString: String = Source.fromFile(filePath).mkString
    gson.fromJson(jsonString, classOf[ContractsConfig])
  }

  def write(filePath: String, newConfig: ContractsConfig): Unit = {
    val writer: Writer = new FileWriter(filePath)
    writer.write(gson.toJson(newConfig))
    writer.close()
  }
}
