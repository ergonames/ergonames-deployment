package app

import configs.{
  Conf,
  ContractsConfigManager,
  PoolConfig,
  RegistryCompileMetadata,
  registryCreationDataParser
}
import contracts.ErgoNamesContracts
import execute.Client
import org.bouncycastle.util.encoders.Hex
import org.ergoplatform.appkit.{
  Address,
  ErgoValue,
  InputBox,
  Parameters,
  SignedTransaction
}
import org.ergoplatform.sdk.{ErgoId, ErgoToken}
import sigmastate.AvlTreeFlags
import transcoder.encoderHelper
import utils.{BoxAPI, ContractCompile, OutBoxes, TransactionHelper, explorerApi}
import work.lithos.plasma.PlasmaParameters
import work.lithos.plasma.collections.PlasmaMap

import scala.annotation.tailrec
import scala.collection.mutable
import scala.jdk.CollectionConverters.asScalaBufferConverter
object Main extends App {

  val arglist = args.toList
  type OptionMap = Map[Symbol, String]

  @tailrec
  private def nextOption(map: OptionMap, list: List[String]): OptionMap = {
    list match {
      case Nil => map
      case "--conf" :: value :: tail =>
        nextOption(map ++ Map('conf -> value.toString), tail)
      case "--dryrun" :: tail =>
        nextOption(map ++ Map('dryrun -> "true"), tail)
      case "--initialize" :: tail =>
        nextOption(map ++ Map('action -> "initialize"), tail)
      case "--registry" :: tail =>
        nextOption(map ++ Map('action -> "registry"), tail)
      case "--clone" :: tail =>
        nextOption(map ++ Map('action -> "clone"), tail)
      case "--issuer" :: tail =>
        nextOption(map ++ Map('action -> "issuer"), tail)
      case "--config" :: tail =>
        nextOption(map ++ Map('action -> "config"), tail)
      case "--subnames" :: tail =>
        nextOption(map ++ Map('action -> "subnames"), tail)
      case "--commit" :: tail =>
        nextOption(map ++ Map('action -> "commit"), tail)
      case "--reveal" :: tail =>
        nextOption(map ++ Map('action -> "reveal"), tail)
      case "--metadataPath" :: value :: tail =>
        nextOption(map ++ Map('metadataPath -> value.toString), tail)
      case "--poolsPath" :: value :: tail =>
        nextOption(map ++ Map('poolsPath -> value.toString), tail)
      case "--contractSaveFilePath" :: value :: tail =>
        nextOption(map ++ Map('contractSaveFilePath -> value.toString), tail)
      case "--collectionTokenId" :: value :: tail =>
        nextOption(map ++ Map('collectionTokenId -> value.toString), tail)
      case "--registrySingletonTokenId" :: value :: tail =>
        nextOption(
          map ++ Map('registrySingletonTokenId -> value.toString),
          tail
        )
      case "--commitContractAddress" :: value :: tail =>
        nextOption(map ++ Map('commitContractAddress -> value.toString), tail)
      case "--boxId" :: value :: tail =>
        nextOption(map ++ Map('boxId -> value.toString), tail)
      case "--nanoErgs" :: value :: tail =>
        nextOption(map ++ Map('nanoErgs -> value.toString), tail)
      case option :: _ =>
        println("Unknown option " + option)
        sys.exit(1)
    }
  }

  private val options = nextOption(Map(), arglist)

  private val confFilePath = options.get('conf)
  private val dryRun = options.contains('dryrun)
  private val action = options.get('action)

  confFilePath match {
    case Some(path) => // Continue processing
    case None =>
      println("Configuration file path not provided.")
      sys.exit(1)
  }

  private val configFilePath: String = confFilePath.get
  private val Config =
    Conf.read(configFilePath)

  private val client: Client = new Client()
  client.setClient
  private val ctx = client.getContext

  val compiler = new ContractCompile(ctx)

  val boxAPIObj =
    new BoxAPI(Config.apiUrl, Config.nodeUrl)

  val api = new explorerApi()

  val outBoxObj = new OutBoxes(ctx)

  val txHelper = new TransactionHelper(
    ctx,
    Config.txOperatorMnemonic,
    proverIndex = Config.addressIndex
  )

  println("Connected Address: " + txHelper.senderAddress.toString)

  action match {
    case Some("clone") =>
      val boxId = options.get('boxId) match {
        case Some(value) => value
        case None =>
          println("box ID is required for clone action.")
          sys.exit(1)
      }

      val boxValue = options.get('nanoErgs) match {
        case Some(value) => value.toLong
        case None =>
          println("Note that the exact ERG value in box will be cloned"); 0L

      }

      val mainnetApi = new explorerApi(
        "https://api.ergoplatform.com",
        "http://213.239.193.208:9053"
      )

      val box = mainnetApi.getUnspentBoxFromMempool(boxId)

      val ergForMinting = Config.minBoxValue * box.getTokens.size()
      val ergForFees = Config.minerFeeNanoERG * box.getTokens.size()
      val boxErgValue = if (boxValue == 0) box.getValue else boxValue
      var totalErgCost =
        boxErgValue + ergForMinting + ergForFees + Config.minerFeeNanoERG + Config.minBoxValue

      val inputValueRequired = totalErgCost + Config.minerFeeNanoERG

      val inputboxes = new collection.mutable.ListBuffer[InputBox]

      println(s"Total Cost: ${inputValueRequired * math.pow(10, -9)}")

      Thread.sleep(2000)

      val inputs = boxAPIObj.getInputBoxes(
        txHelper.senderAddress.toString,
        inputValueRequired
      )

      inputboxes.append(
        inputs.items.map(boxAPIObj.convertJsonBoxToInputBox): _*
      )

      var cometTokenId = ""

      val signedTxHolder
          : mutable.ListBuffer[org.ergoplatform.appkit.SignedTransaction] =
        new mutable.ListBuffer[org.ergoplatform.appkit.SignedTransaction]()

      box.getTokens.forEach(token => {
        totalErgCost = totalErgCost - (2 * Parameters.MinFee)
        val tokenMetadata = mainnetApi.getTokenById(token.getId.toString())

        if (signedTxHolder.isEmpty) {

          val signedTx = txHelper.createToken(
            txHelper.senderAddress,
            Seq(totalErgCost),
            inputBox = Some(inputboxes),
            name =
              if (tokenMetadata.getName == null) token.getId.toString()
              else tokenMetadata.getName,
            description =
              if (tokenMetadata.getDescription == null) token.getId.toString()
              else tokenMetadata.getDescription,
            tokenAmount =
              if (tokenMetadata.getName == "COMET") token.getValue * 2
              else token.getValue,
            tokenDecimals =
              if (tokenMetadata.getDecimals == null) 0
              else tokenMetadata.getDecimals
          )

          if (tokenMetadata.getName == "COMET") {
            println("Comet found")
            cometTokenId = inputboxes.head.getId.toString()
          }

          signedTxHolder.append(signedTx)
        } else {
          val signedTx = txHelper.createToken(
            txHelper.senderAddress,
            Seq(totalErgCost),
            extraTokens = Some(
              signedTxHolder.head.getOutputsToSpend.get(0).getTokens.asScala
            ),
            inputBox = Some(Seq(signedTxHolder.head.getOutputsToSpend.get(0))),
            name =
              if (tokenMetadata.getName == null) token.getId.toString()
              else tokenMetadata.getName,
            description =
              if (tokenMetadata.getDescription == null) token.getId.toString()
              else tokenMetadata.getDescription,
            tokenAmount =
              if (tokenMetadata.getName == "COMET") token.getValue * 2
              else token.getValue,
            tokenDecimals =
              if (tokenMetadata.getDecimals == null) 0
              else tokenMetadata.getDecimals
          )

          if (tokenMetadata.getName == "COMET") {
            println("Comet found")
            cometTokenId =
              signedTxHolder.head.getOutputsToSpend.get(0).getId.toString()
          }

          signedTxHolder.clear()
          signedTxHolder.append(signedTx)
        }

        val txId = submitTransaction(signedTxHolder.head, dryRun)

        println(s"Token Mint: $txId")

      })

      val tokenBox = outBoxObj.simpleTokenBox(
        Seq(
          new ErgoToken(
            cometTokenId,
            (signedTxHolder.head.getOutputsToSpend
              .get(0)
              .getTokens
              .asScala
              .find((t) => t.getId.toString() == cometTokenId)
              .get
              .getValue) / 2
          )
        ),
        txHelper.senderAddress,
        Config.minBoxValue
      )

      val clonedBox = outBoxObj.boxCloner(
        box,
        signedTxHolder.head.getOutputsToSpend
          .get(0)
          .getTokens
          .asScala
          .map(t => {
            if (t.getId.toString() == cometTokenId) {
              ErgoToken(t.getId, t.getValue / 2)
            } else {
              t
            }
          }),
        boxErgValue
      )

      val unSignedTx =
        txHelper.buildUnsignedTransaction(
          Seq(signedTxHolder.head.getOutputsToSpend.get(0)),
          Seq(clonedBox, tokenBox)
        )

      val signedTx = txHelper.signTransaction(
        unSignedTx,
        proverIndex = Config.addressIndex
      )

      val txId = submitTransaction(signedTx, dryRun)

      println(s"Clone Tx: $txId")
      println(s"Box Id: ${signedTx.getOutputsToSpend.get(0).getId.toString()}")
      sys.exit(0)

    case Some("initialize") =>
      val registryConfigFilePath = options.get('metadataPath) match {
        case Some(value) => Some(value)
        case None =>
          println("metadataPath required!")
          sys.exit(1)
      }

      val contractSaveFilePath = options.get('contractSaveFilePath) match {
        case Some(value) => Some(value)
        case None =>
          println("saving to contracts.json by default")
          Some("contracts.json")
      }

      val poolsPath = options.get('poolsPath) match {
        case Some(value) => Some(value)
        case None =>
          println("poolsPath required!")
          sys.exit(1)
      }

      val registryCreationConfig =
        registryCreationDataParser.read(registryConfigFilePath.get)

      val poolsConfig = PoolConfig.read(poolsPath.get)

      val configContractScript =
        ErgoNamesContracts.ergonames_v1_config.contractScript
      val configContract = compiler.compileConfigContract(
        configContractScript,
        // params below are dummy, appkit cannot compile these into ergotree on the fly yet
        2,
        Array(
          Address.create("9hQT2zaqPpqqkpBR8mCtm9MSWiKEeBP1rX2hdSSPbXLAyWodBVF"),
          Address.create("9gZPevPc1jzKhtcDpGTMFUnsQFziBhtpdR53TKAVQVkVMcm8fwF")
        )
      )

      val encoder = new encoderHelper(registryCreationConfig)

      val encodedCollectionInfo = encoder.encodeCollectionInfo
      val encodedSocials = encoder.encodeSocials

      val boxValue =
        Config.minBoxValue + Config.minerFeeNanoERG + Parameters.OneErg + Config.minerFeeNanoERG + Config.minerFeeNanoERG + Config.minerFeeNanoERG

      val inputValueRequired =
        boxValue + Config.minBoxValue + Config.minerFeeNanoERG

      val inputboxes = new collection.mutable.ListBuffer[InputBox]

      println(s"Total Cost: ${inputValueRequired * math.pow(10, -9)}")

      Thread.sleep(2000)

      val inputs = boxAPIObj.getInputBoxes(
        txHelper.senderAddress.toString,
        inputValueRequired
      )

      inputboxes.append(
        inputs.items.map(boxAPIObj.convertJsonBoxToInputBox): _*
      )

      val configMap = new PlasmaMap[ErgoId, ErgoId](
        AvlTreeFlags.AllOperationsAllowed,
        PlasmaParameters.default
      )

      val configSingletonSignedTx = txHelper.createToken(
        txHelper.senderAddress,
        Seq(inputValueRequired - Config.minerFeeNanoERG),
        inputBox = Some(inputboxes),
        name = registryCreationConfig.configSingletonName,
        description = registryCreationConfig.singletonDescription,
        tokenAmount = 1,
        tokenDecimals = 0
      )

      val configSingletonTransactionId =
        submitTransaction(configSingletonSignedTx, dryRun)

      println(s"Config Singleton Tx: $configSingletonTransactionId")

      poolsConfig.foreach(pool => {

        val key = new ErgoId(Hex.decode(pool.tokenId))
        val value = new ErgoId(Hex.decode(pool.poolTokenId))

        configMap.insert(
          (key, value)
        )
      })

      val configOutbox = outBoxObj.initConfigBox(
        configContract,
        configMap,
        configSingletonSignedTx.getOutputsToSpend.get(0).getTokens.get(0),
        Config.minBoxValue
      )

      val outputForChainedTx =
        outBoxObj.simpleOutBox(
          txHelper.senderAddress,
          configSingletonSignedTx.getOutputsToSpend
            .get(0)
            .getValue - Config.minerFeeNanoERG - Config.minBoxValue
        )

      val configUnsignedTransaction =
        txHelper.buildUnsignedTransaction(
          Seq(configSingletonSignedTx.getOutputsToSpend.get(0)),
          Seq(configOutbox, outputForChainedTx)
        )

      val configSignedTransaction = txHelper.signTransaction(
        configUnsignedTransaction,
        proverIndex = Config.addressIndex
      )

      val configTransactionId =
        submitTransaction(configSignedTransaction, dryRun)

      println(s"Config Tx: $configTransactionId")

      val outbox = outBoxObj.buildIssuerBox(
        txHelper.senderAddress,
        Array(
          ErgoValue.of(1),
          ErgoValue.fromHex(encodedCollectionInfo),
          ErgoValue.fromHex(encodedSocials),
          ErgoValue.of(registryCreationConfig.mintingExpiry),
          ErgoValue.fromHex(encoder.getEmptyAdditionalInfo)
        ),
        configSignedTransaction.getOutputsToSpend
          .get(1)
          .getValue - Config.minerFeeNanoERG
      )

      val issuerUnsignedTransaction =
        txHelper.buildUnsignedTransaction(
          Seq(configSignedTransaction.getOutputsToSpend.get(1)),
          Seq(outbox)
        )

      val issuerSignedTransaction = txHelper.signTransaction(
        issuerUnsignedTransaction,
        proverIndex = Config.addressIndex
      )

      val issuerTransactionId =
        submitTransaction(issuerSignedTransaction, dryRun)

      println(s"Issuer Tx: $issuerTransactionId")

      val collectionIssuanceTx = txHelper.createCollectionTokenTransaction(
        Seq(issuerSignedTransaction.getOutputsToSpend.get(0)),
        issuerSignedTransaction.getOutputsToSpend
          .get(0)
          .getValue - Config.minerFeeNanoERG,
        txHelper.senderAddress,
        registryCreationConfig.collectionInfo.collectionName,
        registryCreationConfig.collectionInfo.collectionDescription,
        Long.MaxValue
      )

      val collectionTransactionId =
        submitTransaction(collectionIssuanceTx, dryRun)

      println(s"Collection Token Mint: $collectionTransactionId")

      val singletonSignedTransaction = txHelper.createToken(
        txHelper.senderAddress,
        Seq(
          collectionIssuanceTx.getOutputsToSpend
            .get(0)
            .getValue - Config.minerFeeNanoERG
        ),
        Some(collectionIssuanceTx.getOutputsToSpend.get(0).getTokens.asScala),
        Some(Seq(collectionIssuanceTx.getOutputsToSpend.get(0))),
        name = registryCreationConfig.singletonName,
        description = registryCreationConfig.singletonDescription,
        tokenAmount = 1,
        tokenDecimals = 0
      )

      val singletonTransactionId =
        submitTransaction(singletonSignedTransaction, dryRun)

      println(s"Singleton Mint Tx: $singletonTransactionId")

      val regContractScript =
        ErgoNamesContracts.ergonames_v1_registry.contractScript

      val subnamesContractScript =
        ErgoNamesContracts.ergonames_v1_subname_registry.contractScript
      val subnamesContract =
        compiler.compileSubnamesContract(subnamesContractScript)

      val issuerContractScript =
        ErgoNamesContracts.ergonames_v1_issuer.contractScript
      val issuerContract =
        compiler.compileIssuerContract(
          issuerContractScript,
          collectionIssuanceTx.getOutputsToSpend.get(0).getTokens.get(0)
        )

      val regContract = compiler.compileRegistryContract(
        regContractScript,
        configSignedTransaction.getOutputsToSpend.get(0).getTokens.get(0),
        new ErgoToken(registryCreationConfig.sigUsdOracleSingletonTokenId, 1),
        subnamesContract,
        issuerContract,
        configContract,
        configContract
      )

      val out = outBoxObj.ergoNamesInitOutBox(
        regContract,
        singletonSignedTransaction.getOutputsToSpend.get(0).getTokens.get(0),
        collectionIssuanceTx.getOutputsToSpend.get(0).getTokens.get(0),
        registryCreationConfig.minCommitBoxAge,
        registryCreationConfig.maxCommitBoxAge,
        Array(BigInt(1)),
        singletonSignedTransaction.getOutputsToSpend
          .get(0)
          .getValue - Config.minerFeeNanoERG
      )

      val unSignedTx =
        txHelper.buildUnsignedTransaction(
          Seq(singletonSignedTransaction.getOutputsToSpend.get(0)),
          Seq(out)
        )

      val signedTx = txHelper.signTransaction(
        unSignedTx,
        proverIndex = Config.addressIndex
      )

      val txId = submitTransaction(signedTx, dryRun)

      println(s"Registry Tx Successful: $txId")

      val commitContractScript =
        ErgoNamesContracts.ergonames_v1_commit.contractScript
      val commitContract =
        compiler.compileCommitContract(
          commitContractScript,
          singletonSignedTransaction.getOutputsToSpend.get(0).getTokens.get(0)
        )

      val revealContractScript =
        ErgoNamesContracts.ergonames_v1_reveal.contractScript
      val revealContract =
        compiler.compileRevealContract(
          revealContractScript,
          commitContract
        )

//      println(s"commit contract: ${commitContract.toAddress}")
//      println(s"reveal contract: ${revealContract.toAddress}")
//      println(s"registry contract: ${regContract.toAddress}")
//      println(s"issuer contract: ${issuerContract.toAddress}")
//      println(s"subnames contract: ${subnamesContract.toAddress}")
//      println(s"config contract: ${configContract.toAddress}")

      new ContractsConfigManager(
        regContract.toAddress.toString,
        singletonSignedTransaction.getOutputsToSpend
          .get(0)
          .getTokens
          .get(0)
          .getId
          .toString(),
        collectionIssuanceTx.getOutputsToSpend
          .get(0)
          .getTokens
          .get(0)
          .getId
          .toString(),
        configContract.toAddress.toString,
        configSignedTransaction.getOutputsToSpend
          .get(0)
          .getTokens
          .get(0)
          .getId
          .toString(),
        registryCreationConfig.sigUsdOracleSingletonTokenId,
        registryCreationConfig.minCommitBoxAge,
        registryCreationConfig.maxCommitBoxAge,
        issuerContract.toAddress.toString,
        subnamesContract.toAddress.toString,
        commitContract.toAddress.toString,
        revealContract.toAddress.toString
      ).write(contractSaveFilePath.get)

      println(s"Saved to ${contractSaveFilePath.get}")

    case Some("issuer") =>
      val collectionToken = options.get('collectionTokenId) match {
        case Some(value) => Some(new ErgoToken(value, 1))
        case None =>
          println("collectionTokenId required!")
          sys.exit(1)
      }

      val issuerContractScript =
        ErgoNamesContracts.ergonames_v1_issuer.contractScript
      val issuerContract =
        compiler.compileIssuerContract(
          issuerContractScript,
          collectionToken.get
        )

      println(s"Issuer Contract: ${issuerContract.toAddress}")

    case Some("config") =>
      val configContractScript =
        ErgoNamesContracts.ergonames_v1_config.contractScript
      val configContract = compiler.compileConfigContract(
        configContractScript,
        // params below are dummy, appkit cannot compile these into ergotree on the fly yet
        2,
        Array(
          Address.create("9hQT2zaqPpqqkpBR8mCtm9MSWiKEeBP1rX2hdSSPbXLAyWodBVF"),
          Address.create("9gZPevPc1jzKhtcDpGTMFUnsQFziBhtpdR53TKAVQVkVMcm8fwF")
        )
      )

      println(s"Config Contract: ${configContract.toAddress}")

    case Some("subnames") =>
      val subnamesContractScript =
        ErgoNamesContracts.ergonames_v1_subname_registry.contractScript
      val subnamesContract =
        compiler.compileSubnamesContract(subnamesContractScript)

      println(s"Subnames Contract: ${subnamesContract.toAddress}")

    case Some("commit") =>
      val registrySingletonToken =
        options.get('registrySingletonTokenId) match {
          case Some(value) => Some(new ErgoToken(value, 1))
          case None =>
            println("registrySingletonTokenId required!")
            sys.exit(1)
        }

      val commitContractScript =
        ErgoNamesContracts.ergonames_v1_commit.contractScript
      val commitContract =
        compiler.compileCommitContract(
          commitContractScript,
          registrySingletonToken.get
        )

      println(s"Commit Contract: ${commitContract.toAddress}")

    case Some("reveal") =>
      val commitContractAddress =
        options.get('commitContractAddress) match {
          case Some(value) => Some(Address.create(value))
          case None =>
            println("commitContractAddress required!")
            sys.exit(1)
        }

      val revealContractScript =
        ErgoNamesContracts.ergonames_v1_reveal.contractScript
      val revealContract =
        compiler.compileRevealContract(
          revealContractScript,
          commitContractAddress.get.toErgoContract
        )

      println(s"Reveal Contract: ${revealContract.toAddress}")

    case Some("registry") =>
      val registryCompileMetadataFilePath = options.get('metadataPath) match {
        case Some(value) => Some(value)
        case None =>
          println("metadataPath required!")
          sys.exit(1)
      }

      val registryCompileMetadataConfig =
        RegistryCompileMetadata.read(registryCompileMetadataFilePath.get)

      val regContractScript =
        ErgoNamesContracts.ergonames_v1_registry.contractScript
      val regContract = compiler.compileRegistryContract(
        regContractScript,
        new ErgoToken(registryCompileMetadataConfig.configSingletonTokenId, 1),
        new ErgoToken(
          registryCompileMetadataConfig.sigUsdOracleSingletonTokenId,
          1
        ),
        Address
          .create(registryCompileMetadataConfig.subnameRegistryContractAddress)
          .toErgoContract,
        Address
          .create(registryCompileMetadataConfig.issuerContractAddress)
          .toErgoContract,
        Address
          .create(registryCompileMetadataConfig.feeContractAddress)
          .toErgoContract,
        Address
          .create(registryCompileMetadataConfig.configContractAddress)
          .toErgoContract
      )

      println(s"Registry Contract: ${regContract.toAddress}")

    case _ =>
      println("Invalid action")
      sys.exit(1)
  }

  private def submitTransaction(
      signedTx: SignedTransaction,
      dryRun: Boolean
  ): String = {
    if (dryRun) signedTx.getId
    else
      try {
        txHelper.sendTx(signedTx).replace("\"", "")
      } catch {
        case e: Exception =>
          println(
            s"Error: issue submitting transaction. Details: ${e.getMessage}"
          )
          e.printStackTrace()
          sys.exit(1)
      }
  }

  sys.exit(0)
}
