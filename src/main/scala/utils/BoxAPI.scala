package utils

import com.google.gson.GsonBuilder
import org.apache.http.HttpHeaders
import org.apache.http.client.methods.{HttpGet, HttpPost}
import org.apache.http.entity.StringEntity
import org.apache.http.impl.client.HttpClients
import org.apache.http.util.EntityUtils
import org.ergoplatform.appkit.InputBox
import org.ergoplatform.appkit.impl.InputBoxImpl
import org.ergoplatform.restapi.client.{Asset, ErgoTransactionOutput, Registers}
import org.ergoplatform.sdk.ErgoToken

import scala.collection.JavaConverters._
import scala.collection.mutable.ArrayBuffer
import scala.util.Try
import scala.util.control.Breaks.{break, breakable}

case class RawResponse(
    items: Array[BoxJson],
    total: Long
)

case class NodeBoxJson(
    globalIndex: String,
    inclusionHeight: Int,
    address: String,
    spentTransactionId: String,
    boxId: String,
    value: Long,
    ergoTree: String,
    assets: Array[AssetJson],
    creationHeight: Int,
    additionalRegisters: NodeAdditionalRegistersJson,
    transactionId: String,
    index: Int
)

case class BoxJson(
    boxId: String,
    transactionId: String,
    blockId: String,
    value: Long,
    index: Int,
    globalIndex: Int,
    creationHeight: Int,
    settlementHeight: Int,
    ergoTree: String,
    ergoTreeConstants: String,
    ergoTreeScript: String,
    address: String,
    assets: Array[AssetJson],
    additionalRegisters: AdditionalRegistersJson,
    spentTransactionId: String,
    mainChain: Boolean
)

case class UnspentBoxJson(
    boxId: String,
    value: Long,
    ergoTree: String,
    assets: Array[SimpleAssetsJson],
    creationHeight: Int,
    additionalRegistersJson: AdditionalRegistersJson,
    transactionId: String,
    index: Int
)

case class AdditionalRegistersJson(
    R4: RegisterJson,
    R5: RegisterJson,
    R6: RegisterJson,
    R7: RegisterJson,
    R8: RegisterJson,
    R9: RegisterJson
)

case class RegisterJson(
    serializedValue: String,
    sigmaType: String,
    renderedValue: String
)

case class NodeAdditionalRegistersJson(
    R4: String,
    R5: String,
    R6: String,
    R7: String,
    R8: String,
    R9: String
)

case class AssetJson(
    tokenId: String,
    index: Long,
    amount: Long,
    name: String,
    decimals: Long,
    `type`: String
)

case class SimpleAssetsJson(
    tokenId: String,
    amount: Long
)

class BoxAPI(apiUrl: String, nodeUrl: String) {

  private val client = HttpClients.custom().build()
  private val gson = new GsonBuilder().setPrettyPrinting().create()

  def convertJsonBoxToInputBox(boxJson: BoxJson): InputBox = {
    val tokens = new java.util.ArrayList[Asset](boxJson.assets.length)

    for (asset <- boxJson.assets.toSeq) {
      tokens.add(new Asset().tokenId(asset.tokenId).amount(asset.amount))
    }

    val regList = List(
      ("R4", boxJson.additionalRegisters.R4),
      ("R5", boxJson.additionalRegisters.R5),
      ("R6", boxJson.additionalRegisters.R6),
      ("R7", boxJson.additionalRegisters.R7),
      ("R8", boxJson.additionalRegisters.R8),
      ("R9", boxJson.additionalRegisters.R9)
    ).flatMap { case (name, value) =>
      Try((name, value.serializedValue)).toOption
    }.filterNot(_._2 == null)

    val registers = new Registers
    registers.putAll(regList.toMap.asJava)

    val boxConversion = new ErgoTransactionOutput()
      .ergoTree(boxJson.ergoTree)
      .boxId(boxJson.boxId)
      .index(boxJson.index)
      .value(boxJson.value)
      .transactionId(boxJson.transactionId)
      .creationHeight(boxJson.creationHeight)
      .assets(tokens)
      .additionalRegisters(registers)

    new InputBoxImpl(boxConversion).asInstanceOf[InputBox]
  }

  def convertJsonBoxToInputBox(boxJson: NodeBoxJson): InputBox = {
    val tokens = new java.util.ArrayList[Asset](boxJson.assets.length)

    for (asset <- boxJson.assets.toSeq) {
      tokens.add(new Asset().tokenId(asset.tokenId).amount(asset.amount))
    }

    val regList = List(
      ("R4", boxJson.additionalRegisters.R4),
      ("R5", boxJson.additionalRegisters.R5),
      ("R6", boxJson.additionalRegisters.R6),
      ("R7", boxJson.additionalRegisters.R7),
      ("R8", boxJson.additionalRegisters.R8),
      ("R9", boxJson.additionalRegisters.R9)
    ).flatMap { case (name, value) =>
      Try((name, value)).toOption
    }.filterNot(_._2 == null)

    val registers = new Registers
    registers.putAll(regList.toMap.asJava)

    val boxConversion = new ErgoTransactionOutput()
      .ergoTree(boxJson.ergoTree)
      .boxId(boxJson.boxId)
      .index(boxJson.index)
      .value(boxJson.value)
      .transactionId(boxJson.transactionId)
      .creationHeight(boxJson.creationHeight)
      .assets(tokens)
      .additionalRegisters(registers)

    new InputBoxImpl(boxConversion).asInstanceOf[InputBox]
  }

  def getUnspentBoxesFromNode(
      Address: String,
      amountToSelect: Int = -1,
      boxesSelectedPerRequest: Int = 10000,
      selectAll: Boolean = false
  ): Array[NodeBoxJson] = {

    var limit =
      if (amountToSelect <= boxesSelectedPerRequest && amountToSelect != -1)
        amountToSelect
      else boxesSelectedPerRequest

    var offset = 0
    var allBoxes = Array[NodeBoxJson]()

    val requestEntity = new StringEntity(
      Address
    )
    val nodeWithUnspentByAddress = "" // enter node address here

    while (true) {
      val post = new HttpPost(
        s"${nodeWithUnspentByAddress}/blockchain/box/unspent/byAddress?offset=${offset}&limit=${limit}"
      )

      post.setHeader(HttpHeaders.CONTENT_TYPE, "application/json")

      post.setEntity(requestEntity)

      val response = client.execute(post)
      val resp = EntityUtils.toString(response.getEntity)

      if (resp == "[]") {
        return allBoxes
      }

      val liliumResponseEntry = gson.fromJson(resp, classOf[Array[NodeBoxJson]])
      allBoxes = allBoxes ++ liliumResponseEntry

      if (!selectAll && allBoxes.length >= amountToSelect) {
        return allBoxes
      }

      if (liliumResponseEntry.length < limit) {
        return allBoxes
      }

      offset += limit
    }

    allBoxes
  }

  def getInputBoxes(
      inputAddress: String,
      targetNanoErgs: Long,
      tokens: Option[Array[ErgoToken]] = None
  ): RawResponse = {
    val limit = 100
    val inputs = ArrayBuffer[BoxJson]()
    var addedBoxIds = Set[String]()
    val client = HttpClients.createDefault()

    def getUnspentBoxesByAddress(offset: Int): RawResponse = {
      val get = new HttpGet(
        s"${apiUrl}/api/v1/boxes/unspent/byAddress/$inputAddress?offset=$offset&limit=$limit"
      )
      val response = client.execute(get)
      val resp = EntityUtils.toString(response.getEntity)
      if (resp == "[]") {
        return RawResponse(Array[BoxJson](), 0)
      }

      gson.fromJson(resp, classOf[RawResponse])
    }

    tokens match {
      case Some(tokenList) =>
        val tokenInputs = collection.mutable.Map[String, Long]()
        val finalTokenInputs = ArrayBuffer[BoxJson]()

        tokenList.foreach(token => tokenInputs(token.getId.toString()) = 0L)

        var offset = 0
        breakable {
          while (true) {
            val boxes = getUnspentBoxesByAddress(offset)
            if (boxes.items.isEmpty) throw new Exception("Issue getting boxes")

            for (input <- boxes.items) {
              for (asset <- input.assets) {
                if (
                  tokenInputs.contains(asset.tokenId) &&
                    tokenInputs(asset.tokenId) < tokenList
                      .find(_.getId.toString() == asset.tokenId)
                      .get
                      .getValue
                ) {
                  tokenInputs(asset.tokenId) += asset.amount
                  if (!addedBoxIds.contains(input.boxId)) {
                    finalTokenInputs += input
                    addedBoxIds += input.boxId
                  }
                }
              }
            }

            offset += limit

            if (
              tokenInputs.values.forall(amount =>
                amount >= tokenList
                  .find(_.getId.toString() == tokenInputs.find(_._2 == amount).get._1)
                  .get
                  .getValue
              )
            ) {
              inputs ++= finalTokenInputs
              inputs.map(_.value).sum >= targetNanoErgs
              break
            }

            if (boxes.items.length < limit)
              throw new Exception("Insufficient inputs")
          }
        }

      case None =>
    }

    var cumInputValue = inputs.map(_.value).sum
    if (cumInputValue >= targetNanoErgs) return RawResponse(inputs.toArray, inputs.length)

    var offset = 0
    while (true) {
      val boxes = getUnspentBoxesByAddress(offset)
      if (boxes.items.isEmpty) throw new Exception("Issue getting boxes")

      for (box <- boxes.items) {
        if (!addedBoxIds.contains(box.boxId)) {
          inputs += box
          addedBoxIds += box.boxId
          cumInputValue += box.value

          if (cumInputValue >= targetNanoErgs) return RawResponse(inputs.toArray, inputs.length)
        }
      }

      offset += limit
      if (boxes.items.length < limit) throw new Exception("Insufficient inputs")
    }

    throw new Exception("Insufficient inputs")
  }

}
