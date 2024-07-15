package utils

import configs.Conf
import explorer.Explorer
import org.ergoplatform.ErgoBox
import org.ergoplatform.appkit.InputBox
import org.ergoplatform.appkit.impl.{InputBoxImpl, ScalaBridge}
import org.ergoplatform.explorer.client.model.{InputInfo, OutputInfo, TokenInfo, TransactionInfo}
import org.ergoplatform.explorer.client.{DefaultApi, ExplorerApiClient}
import org.ergoplatform.restapi.client._

import java.util
import scala.collection.JavaConversions._
import scala.jdk.CollectionConverters.asScalaBufferConverter

class explorerApi(
    apiUrl: String = Conf.read("config.json").apiUrl,
    nodeUrl: String = Conf.read("config.json").nodeUrl
) extends Explorer(
      nodeInfo = execute.DefaultNodeInfo(
        nodeUrl,
        apiUrl,
        new network(
          Conf.read("config.json").nodeUrl
        ).getNetworkType
      )
    ) {

  def getExplorerApi(apiUrl: String): DefaultApi = {
    new ExplorerApiClient(apiUrl).createService(classOf[DefaultApi])
  }

  def buildNodeService(nodeUrl: String): ApiClient = {
    new ApiClient(nodeUrl)
  }

  def getUnspentBoxFromTokenID(tokenId: String): OutputInfo = {
    val api = this.getExplorerApi(this.apiUrl)
    val res =
      api.getApiV1BoxesUnspentBytokenidP1(tokenId, 0, 1).execute().body()
    try {
      res.getItems.get(0)
    } catch {
      case e: Exception => null
    }
  }

  def getAllPools: Seq[OutputInfo] = {
    val nodeApi: DefaultApi = new ExplorerApiClient(this.apiUrl).createService(classOf[DefaultApi])

    val poolErgoTree = "1999030f0400040204020404040405feffffffffffffffff0105feffffffffffffffff01050004d00f040004000406050005000580dac409d819d601b2a5730000d602e4c6a70404d603db63087201d604db6308a7d605b27203730100d606b27204730200d607b27203730300d608b27204730400d6099973058c720602d60a999973068c7205027209d60bc17201d60cc1a7d60d99720b720cd60e91720d7307d60f8c720802d6107e720f06d6117e720d06d612998c720702720fd6137e720c06d6147308d6157e721206d6167e720a06d6177e720906d6189c72117217d6199c72157217d1ededededededed93c27201c2a793e4c672010404720293b27203730900b27204730a00938c7205018c720601938c7207018c72080193b17203730b9593720a730c95720e929c9c721072117e7202069c7ef07212069a9c72137e7214067e9c720d7e72020506929c9c721372157e7202069c7ef0720d069a9c72107e7214067e9c72127e7202050695ed720e917212730d907216a19d721872139d72197210ed9272189c721672139272199c7216721091720b730e"

    var offset = 0
    val limit = 100
    var hasMore = true
    val allItems = scala.collection.mutable.Buffer[OutputInfo]()

    while (hasMore) {
      val call = nodeApi.getApiV1BoxesUnspentByergotreeP1(poolErgoTree, offset, limit)
      val response= call.execute()

      if (response.isSuccessful && response.body() != null && !response.body().getItems.isEmpty) {
        val items = response.body().getItems
        allItems ++= items
        offset += limit  // Move to the next page
      } else {
        hasMore = false
      }
    }

    allItems
  }

  def getAllTokenPools(tokenId: String): Seq[OutputInfo] = {
    val pools = getAllPools
    pools.filter(p => p.getAssets.asScala.exists(_.getTokenId == tokenId))
  }

  def getTokenById(id: String): TokenInfo = {
    val api = this.getExplorerApi(this.apiUrl)
    val res = api.getApiV1TokensP1(id).execute().body()
    res
  }

  def getBoxesFromTokenID(tokenId: String): OutputInfo = { //returns latest box the token has been in
    val api = this.getExplorerApi(this.apiUrl)
    var res = api.getApiV1BoxesBytokenidP1(tokenId, 0, 1).execute().body()
    val offset = res.getTotal - 1
    res = api.getApiV1BoxesBytokenidP1(tokenId, offset, 1).execute().body()
    try {
      res.getItems.get(0)
    } catch {
      case e: Exception => println(e); null
    }
  }

  def getPoolNum(poolId: String): Long = {
    val pool = getBoxesFromTokenID(poolId)
    pool.getAdditionalRegisters.get("R4").renderedValue.toLong
  }

  def getBoxesfromTransaction(txId: String): TransactionInfo = {
    val api = this.getExplorerApi(this.apiUrl)
    api.getApiV1TransactionsP1(txId).execute().body()
  }

  def getAddressInfo(address: String): util.List[OutputInfo] = {
    val api = this.getExplorerApi(this.apiUrl)
    api.getApiV1BoxesByaddressP1(address, 0, 100).execute().body().getItems
  }

  def getUnspentBoxesByAddress(address: String): util.List[OutputInfo] = {
    val api = this.getExplorerApi(this.apiUrl)
    api.getApiV1BoxesByaddressP1(address, 0, 100).execute().body().getItems
  }

  def getBoxesfromUnconfirmedTransaction(
      txId: String
  ): Either[ErgoTransaction, TransactionInfo] = {
    val nodeService = this
      .buildNodeService(this.nodeUrl)
      .createService(classOf[TransactionsApi])

    val res = nodeService.getUnconfirmedTransactionById(txId).execute()
    if (res.code() == 404) {
      return Right(this.getBoxesfromTransaction(txId))
    }
    Left(res.body())
  }

  def getUnspentBoxFromMempool(boxId: String): InputBox = {
    val nodeService =
      this.buildNodeService(this.nodeUrl).createService(classOf[UtxoApi])
    val response = nodeService.getBoxWithPoolById(boxId).execute().body()
    if (response == null) {
      return new InputBoxImpl(this.getErgoBoxfromID(boxId))
        .asInstanceOf[InputBox]
    }
    new InputBoxImpl(response).asInstanceOf[InputBox]
  }

  def getMem(boxId: String): Boolean = {
    val nodeService =
      this.buildNodeService(this.nodeUrl).createService(classOf[UtxoApi])
    val response = nodeService.getBoxWithPoolById(boxId).execute().body()
    if (response == null) {
      return false
    }
    true
  }

  def getBoxbyIDfromExplorer(boxID: String): OutputInfo = {
    val api = this.getExplorerApi(this.apiUrl)
    api.getApiV1BoxesP1(boxID).execute().body()
  }

  def getErgoBoxfromID(boxID: String): ErgoBox = {
    val nodeService =
      this.buildNodeService(this.nodeUrl).createService(classOf[UtxoApi])
    val response: ErgoTransactionOutput =
      nodeService.getBoxWithPoolById(boxID).execute().body()

    if (response == null) {
      val box = this.getBoxbyIDfromExplorer(boxID)
      val tokens = new util.ArrayList[Asset](box.getAssets.size)
      for (asset <- box.getAssets) {
        tokens.add(
          new Asset().tokenId(asset.getTokenId).amount(asset.getAmount)
        )
      }
      val registers = new Registers
      for (registerEntry <- box.getAdditionalRegisters.entrySet) {
        registers.put(
          registerEntry.getKey,
          registerEntry.getValue.serializedValue
        )
      }
      val boxConversion: ErgoTransactionOutput = new ErgoTransactionOutput()
        .ergoTree(box.getErgoTree)
        .boxId(box.getBoxId)
        .index(box.getIndex)
        .value(box.getValue)
        .transactionId(box.getTransactionId)
        .creationHeight(box.getCreationHeight)
        .assets(tokens)
        .additionalRegisters(registers)
      return ScalaBridge.isoErgoTransactionOutput.to(boxConversion)
    }
    val tokens = new util.ArrayList[Asset](response.getAssets.size)
    for (asset <- response.getAssets) {
      tokens.add(new Asset().tokenId(asset.getTokenId).amount(asset.getAmount))
    }
    val registers = new Registers
    for (registerEntry <- response.getAdditionalRegisters.entrySet()) {
      registers.put(registerEntry.getKey, registerEntry.getValue)
    }
    val boxConversion: ErgoTransactionOutput = new ErgoTransactionOutput()
      .ergoTree(response.getErgoTree)
      .boxId(response.getBoxId)
      .index(response.getIndex)
      .value(response.getValue)
      .transactionId(response.getTransactionId)
      .creationHeight(response.getCreationHeight)
      .assets(tokens)
      .additionalRegisters(registers)
    ScalaBridge.isoErgoTransactionOutput.to(boxConversion)
  }

  def getErgoBoxfromIDNoApi(box: InputInfo): ErgoBox = {

    val tokens = new util.ArrayList[Asset](box.getAssets.size)
    for (asset <- box.getAssets) {
      tokens.add(new Asset().tokenId(asset.getTokenId).amount(asset.getAmount))
    }
    val registers = new Registers
    for (registerEntry <- box.getAdditionalRegisters.entrySet) {
      registers.put(
        registerEntry.getKey,
        registerEntry.getValue.serializedValue
      )
    }
    val boxConversion: ErgoTransactionOutput = new ErgoTransactionOutput()
      .ergoTree(box.getErgoTree)
      .boxId(box.getBoxId)
      .index(box.getIndex)
      .value(box.getValue)
      .transactionId(null)
      .creationHeight(null)
      .assets(tokens)
      .additionalRegisters(registers)
    return ScalaBridge.isoErgoTransactionOutput.to(boxConversion)

  }

}
