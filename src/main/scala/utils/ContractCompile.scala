package utils

import org.ergoplatform.appkit._
import org.ergoplatform.sdk.ErgoToken

class ContractCompile(ctx: BlockchainContext) {

  def compileDummyContract(
      contract: String = "sigmaProp(true)"
  ): ErgoContract = {
    this.ctx.compileContract(
      ConstantsBuilder.empty(),
      contract
    )
  }
  def compileConfigContract(
      contract: String,
      minRequiredSignatures: Int,
      addresses: Seq[Address]
  ): ErgoContract = {
    this.ctx.compileContract(
//      ConstantsBuilder
//        .create()
//        .item(
//          "$minRequiredSignatures",
//          minRequiredSignatures
//        )
//        .item(
//          "$addresses",
//          Colls.fromArray(addresses.map(_.getPublicKey).map(key => SigmaProp(key)).toArray)
//        )
//        .build(),
      ConstantsBuilder.empty(),
      contract
    )
  }

  def compileCommitContract(
      contract: String,
      singleton: ErgoToken
  ): ErgoContract = {
    this.ctx.compileContract(
      ConstantsBuilder
        .create()
        .item(
          "$registrySingletonTokenId",
          singleton.getId.getBytes
        )
        .build(),
      contract
    )
  }

  def compileRevealContract(
      contract: String,
      commitContract: ErgoContract
  ): ErgoContract = {
    this.ctx.compileContract(
      ConstantsBuilder
        .create()
        .item(
          "$commitContractBytes",
          commitContract.toAddress.asP2S().scriptBytes
        )
        .build(),
      contract
    )
  }

  def compileIssuerContract(
      contract: String,
      ergoNameCollectionToken: ErgoToken
  ): ErgoContract = {
    this.ctx.compileContract(
      ConstantsBuilder
        .create()
        .item(
          "$ergoNameCollectionTokenId",
          ergoNameCollectionToken.getId.getBytes
        )
        .build(),
      contract
    )
  }

  def compileRegistryContract(
      contract: String,
      configSingleton: ErgoToken,
      sigUsdSingleton: ErgoToken,
      subNameContract: ErgoContract,
      issuerContract: ErgoContract,
      ergoNameFeeContract: ErgoContract,
      configControllerContract: ErgoContract
  ): ErgoContract = {
    this.ctx.compileContract(
      ConstantsBuilder
        .create()
        .item(
          "$subNameContractBytes",
          subNameContract.toAddress.asP2S().scriptBytes
        )
        .item(
          "$ergoNameIssuerContractBytes",
          issuerContract.toAddress.asP2S().scriptBytes
        )
        .item(
          "$ergoNameFeeContractBytes",
          ergoNameFeeContract.toAddress.asP2S().scriptBytes
        )
        .item(
          "$configSingletonTokenId",
          configSingleton.getId.getBytes
        )
        .item(
          "$sigUsdOracleSingletonTokenId",
          sigUsdSingleton.getId.getBytes
        )
//        .item(
//          "$ergonameMultiSigSigmaProp",
//          configControllerContract.getErgoTree.toSigmaBooleanOpt.get
//        )
        .build(),
      contract
    )
  }

  def compileSubnamesContract(
      contract: String
  ): ErgoContract = {
    this.ctx.compileContract(
      ConstantsBuilder.empty(),
      contract
    )
  }

}
