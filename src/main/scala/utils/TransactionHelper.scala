package utils

import org.ergoplatform.appkit.{
  Address,
  BlockchainContext,
  InputBox,
  Mnemonic,
  OutBox,
  SignedTransaction,
  UnsignedTransaction
}
import org.ergoplatform.sdk.{ErgoToken, SecretString}

class TransactionHelper(
    ctx: BlockchainContext,
    walletMnemonic: String,
    mnemonicPassword: String = "",
    proverIndex: Int = 0,
    fee: Long = 1000000L
) {
  private val mnemonic = Mnemonic.create(
    SecretString.create(walletMnemonic),
    SecretString.create(mnemonicPassword)
  )
  private val txBuilder = this.ctx.newTxBuilder()

  val senderAddress: Address = Address.createEip3Address(
    proverIndex,
    ctx.getNetworkType,
    SecretString.create(walletMnemonic),
    SecretString.create(mnemonicPassword),
    false
  )

  private val minAmount = 1000000L

  def buildUnsignedTransaction(
      inputs: Seq[InputBox],
      outputs: Seq[OutBox],
      dataInputs: Seq[InputBox] = Seq.empty,
      tokensToBurn: Seq[ErgoToken] = Seq.empty,
      fee: Long = fee
  ): UnsignedTransaction = {
    val builder = this.ctx
      .newTxBuilder()
      .addInputs(inputs: _*)
      .addOutputs(outputs: _*)
      .fee(fee)
      .sendChangeTo(this.senderAddress)

    if (dataInputs.nonEmpty) builder.addDataInputs(dataInputs: _*)
    if (tokensToBurn.nonEmpty) builder.tokensToBurn(tokensToBurn: _*)

    builder.build()
  }

  def signTransaction(
      unsignedTransaction: UnsignedTransaction,
      proverIndex: Int = proverIndex
  ): SignedTransaction = {
    val prover = this.ctx
      .newProverBuilder()
      .withMnemonic(mnemonic, false)
      .withEip3Secret(proverIndex)
      .build()
    prover.sign(unsignedTransaction)
  }
  def sendTx(signedTransaction: SignedTransaction): String = {
    this.ctx.sendTransaction(signedTransaction)
  }

  def createToken(
      receiver: Address,
      amountList: Seq[Long],
      extraTokens: Option[Seq[ErgoToken]] = None,
      inputBox: Option[Seq[InputBox]] = None,
      sender: Address = this.senderAddress,
      isCollection: Boolean = false,
      name: String,
      description: String,
      tokenAmount: Long,
      tokenDecimals: Int
  ): SignedTransaction = {
    val inBox: Seq[InputBox] = inputBox.getOrElse(
      new InputBoxes(ctx).getInputs(amountList, sender)
    )
    val outBoxObj = new OutBoxes(this.ctx)

    val token = if (isCollection) {
      outBoxObj.collectionTokenHelper(
        inBox.head,
        name,
        description,
        tokenAmount,
        tokenDecimals
      )
    } else {
      outBoxObj.tokenHelper(
        inBox.head,
        name,
        description,
        tokenAmount,
        tokenDecimals
      )
    }

    val outBox = {
      if (extraTokens.isEmpty) {
        outBoxObj.tokenMintOutBox(token, receiver, amount = amountList.head)
      } else {
        outBoxObj.tokenMintCloneOutBox(
          token,
          extraTokens.get,
          receiver,
          amount = amountList.head
        )
      }
    }
    val unsignedTransaction =
      this.buildUnsignedTransaction(inBox, Seq(outBox))

    this.signTransaction(unsignedTransaction)
  }

  def createCollectionTokenTransaction(
      inputs: Seq[InputBox],
      boxValue: Long,
      recipient: Address,
      collectionName: String,
      collectionDescription: String,
      collectionSize: Long
  ): SignedTransaction = {
    this.createToken(
      recipient,
      List(boxValue),
      inputBox = Some(inputs),
      isCollection = true,
      name = collectionName,
      description = collectionDescription,
      tokenAmount = collectionSize,
      tokenDecimals = 0
    )
  }

}
