package utils

import org.ergoplatform.appkit.impl.{Eip4TokenBuilder, ErgoTreeContract}
import org.ergoplatform.appkit._
import org.ergoplatform.appkit.scalaapi.ErgoValueBuilder
import org.ergoplatform.sdk.JavaHelpers.SigmaDsl
import org.ergoplatform.sdk.{ErgoId, ErgoToken}
import sigma.{Coll, Colls}
import work.lithos.plasma.collections.PlasmaMap

import java.nio.charset.StandardCharsets
import scala.collection.JavaConversions._
import scala.jdk.CollectionConverters.asScalaBufferConverter

class OutBoxes(ctx: BlockchainContext) {

  private val minAmount = 1000000L
  private val txBuilder = this.ctx.newTxBuilder()

  def simpleOutBox(
      senderAddress: Address,
      amount: Long = minAmount
  ): OutBox = {
    this.txBuilder
      .outBoxBuilder()
      .value(amount)
      .contract(
        new ErgoTreeContract(
          senderAddress.getErgoAddress.script,
          this.ctx.getNetworkType
        )
      )
      .build()
  }

  def commitBox(
      contract: ErgoContract,
      commitmentHash: Array[Byte],
      buyerAddress: Address,
      minerFee: Long,
      amount: Long = minAmount
  ): OutBox = {
    this.txBuilder
      .outBoxBuilder()
      .value(amount)
      .registers(
        ErgoValue.of(commitmentHash),
        ErgoValue.of(buyerAddress.getPublicKeyGE),
        ErgoValue.of(minerFee)
      )
      .contract(contract)
      .build()
  }

  def revealBox(
      contract: ErgoContract,
      name: String,
      commitmentHash: Array[Byte],
      boxId: ErgoId,
      buyerAddress: Address,
      minerFee: Long,
      txOperatorFee: Long,
      minBoxValue: Long,
      amount: Long
  ): OutBox = {
    this.txBuilder
      .outBoxBuilder()
      .value(amount)
      .registers(
        ErgoValue.of(name.getBytes(StandardCharsets.UTF_8)),
        ErgoValue.of(buyerAddress.getPublicKeyGE),
        ErgoValue.of(commitmentHash),
        ErgoValue.of(boxId.getBytes),
        ErgoValue.of(Array(minerFee, txOperatorFee, minBoxValue))
      )
      .contract(contract)
      .build()
  }

  def initConfigBox[K, V](
      contract: ErgoContract,
      configAvl: PlasmaMap[K, V],
      configSingleton: ErgoToken,
      amount: Long = minAmount
  ): OutBox = {
    this.txBuilder
      .outBoxBuilder()
      .value(amount)
      .registers(
        configAvl.ergoValue
      )
      .tokens(configSingleton)
      .contract(contract)
      .build()
  }

  def tokenMintCloneOutBox(
      token: Eip4Token,
      tokens: Seq[ErgoToken],
      receiver: Address,
      amount: Long = minAmount
  ): OutBox = {
    val box = this.txBuilder
      .outBoxBuilder()
      .value(amount)
      .mintToken(token)
      .contract(
        new ErgoTreeContract(
          receiver.getErgoAddress.script,
          this.ctx.getNetworkType
        )
      )

    if (tokens.isEmpty) {
      box.build()
    } else {
      box.tokens(tokens: _*).build()
    }
  }

  def boxCloner(box: InputBox, tokens: Seq[ErgoToken], value: Long): OutBox = {
    val boxAddress = Address.fromPropositionBytes(
      this.ctx.getNetworkType,
      box.toErgoValue.getValue.propositionBytes.toArray
    )
    val outputCandidate = this.txBuilder
      .outBoxBuilder()
      .value(value)
      .contract(boxAddress.toErgoContract)
    if (tokens.nonEmpty) {
      outputCandidate.tokens(tokens: _*)
    }
    if (!box.getRegisters.isEmpty) {
      outputCandidate.registers(box.getRegisters.asScala: _*)
    }
    outputCandidate.build()
  }

  def ergoNamesInitOutBox[K, V](
      mintContract: ErgoContract,
      singleton: ErgoToken,
      collectionToken: ErgoToken,
      minCommitBoxAge: Int,
      maxCommitBoxAge: Int,
      priceMap: Array[BigInt],
      amount: Long = minAmount
  ): OutBox = {
    val previousState: (Coll[Byte], Long) =
      (Colls.fromArray(singleton.getId.getBytes), 0L)
    val ageThreshold: (Int, Int) = (minCommitBoxAge, maxCommitBoxAge)
    val priceColl =
      Colls.fromArray(
        priceMap.map(price => SigmaDsl.BigInt(price.underlying()))
      )

    this.txBuilder
      .outBoxBuilder()
      .value(amount)
      .contract(mintContract)
      .tokens(singleton, collectionToken)
      .registers(
        ErgoValue.fromHex(
          "644ec61f485b98eb87153f7c57db4f5ecd75556fddbc403b41acf8441fde8e160900072000"
        ),
        ErgoValueBuilder.buildFor(previousState),
        ErgoValueBuilder.buildFor(ageThreshold),
        ErgoValueBuilder.buildFor(priceColl)
      )
      .build()
  }

  def collectionTokenHelper(
      inputBox: InputBox,
      name: String,
      description: String,
      tokenAmount: Long,
      tokenDecimals: Int
  ): Eip4Token = {

    Eip4TokenBuilder.buildNftArtworkCollectionToken(
      inputBox.getId.toString,
      tokenAmount,
      name,
      description,
      tokenDecimals
    )
  }

  def buildIssuerBox(
      senderAddress: Address,
      registers: Array[ErgoValue[_]],
      amount: Long = minAmount
  ): OutBox = {
    this.txBuilder
      .outBoxBuilder()
      .value(amount)
      .registers(registers: _*)
      .contract(
        new ErgoTreeContract(
          senderAddress.getErgoAddress.script,
          this.ctx.getNetworkType
        )
      )
      .build()
  }

  def tokenHelper(
      inputBox: InputBox,
      name: String,
      description: String,
      tokenAmount: Long,
      tokenDecimals: Int
  ): Eip4Token = {
    new Eip4Token(
      inputBox.getId.toString,
      tokenAmount,
      name,
      description,
      tokenDecimals
    )
  }

  def tokenMintOutBox(
      token: Eip4Token,
      receiver: Address,
      amount: Long = minAmount
  ): OutBox = {
    this.txBuilder
      .outBoxBuilder()
      .value(amount)
      .mintToken(token)
      .contract(
        new ErgoTreeContract(
          receiver.getErgoAddress.script,
          this.ctx.getNetworkType
        )
      )
      .build()
  }

  def simpleTokenBox(
      tokens: Seq[ErgoToken],
      receiver: Address,
      amount: Long = minAmount
  ): OutBox = {
    this.txBuilder
      .outBoxBuilder()
      .value(amount)
      .tokens(tokens: _*)
      .contract(
        new ErgoTreeContract(
          receiver.getErgoAddress.script,
          this.ctx.getNetworkType
        )
      )
      .build()
  }

}
