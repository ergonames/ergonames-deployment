package configs

import com.google.gson.GsonBuilder
import scala.io.Source

case class Data(
    name: String,
    description: String,
    image: String,
    imageSHA256: String,
    dna: String,
    edition: Int,
    assetType: String,
    explicit: Boolean,
    attributes: Array[Attribute],
    levels: Array[Level],
    stats: Array[Stats]
)
case class Attribute(trait_type: String, value: String)

case class Level(trait_type: String, max_value: Int, value: Int)

case class Stats(trait_type: String, max_value: Int, value: Int)
case class Royalty(address: String, amount: Int)

case class RegistryCreationData(
    subNameContractAddress: String,
    issuerContractAddress: String,
    feeContractAddress: String,
    configContractAddress: String,
    configSingletonName: String,
    configSingletonDescription: String,
    sigUsdOracleSingletonTokenId: String,
    minCommitBoxAge: Int,
    maxCommitBoxAge: Int,
    singletonName: String,
    singletonDescription: String,
    collectionInfo: CollectionInfo,
    socialMedia: java.util.Map[String, String],
    royalty: Array[Royalty],
    mintingExpiry: Long
)

case class CollectionInfo(
    collectionName: String,
    collectionDescription: String,
    collectionLogoURL: String,
    collectionFeaturedImageURL: String,
    collectionBannerImageURL: String,
    collectionCategory: String
)

object RegistryMetadata {
  private val gson = new GsonBuilder()
    .setPrettyPrinting()
    .create()

  def read(filePath: String): Data = {
    val jsonString: String = Source.fromFile(filePath).mkString
    gson.fromJson(jsonString, classOf[Data])
  }
}

object registryCreationDataParser {
  private val gson = new GsonBuilder()
    .setPrettyPrinting()
    .create()

  def read(filePath: String): RegistryCreationData = {
    val jsonString: String = Source.fromFile(filePath).mkString
    gson.fromJson(jsonString, classOf[RegistryCreationData])
  }

  def readJsonString(jsonString: String): RegistryCreationData = {
    gson
      .fromJson(jsonString, classOf[RegistryCreationData])
  }

}
