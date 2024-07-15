package main

import contracts.ErgoNamesContracts
import execute.Client
import org.ergoplatform.appkit.{Address, ErgoContract}
import org.ergoplatform.sdk.ErgoToken
import utils.ContractCompile
import org.bouncycastle.util.encoders.Hex
import scorex.util.encode.Base64

object test extends App {
  println("Hello World!")
}

object contractPrint extends App {
  private val client: Client = new Client()
  client.setClient
  private val ctx = client.getContext
  val compiler = new ContractCompile(ctx)
  val registrySingletonToken = new ErgoToken(
    "e0309d2d2948098814b8635a2ba4c8d98918a60bb81b10a81c13e97b9d323ec2",
    1L
  )
  val commitContractScript =
    ErgoNamesContracts.ergonames_v1_commit.contractScript
  val commitContract =
    compiler.compileCommitContract(commitContractScript, registrySingletonToken)

  val revealContractScript =
    ErgoNamesContracts.ergonames_v1_reveal.contractScript
  val revealContract =
    compiler.compileRevealContract(revealContractScript, commitContract)

  val issuerContractScript =
    ErgoNamesContracts.ergonames_v1_issuer.contractScript
  val issuerContract =
    compiler.compileIssuerContract(issuerContractScript, registrySingletonToken)

  val subnamesContractScript =
    ErgoNamesContracts.ergonames_v1_subname_registry.contractScript
  val subnamesContract =
    compiler.compileSubnamesContract(subnamesContractScript)

  val configContractScript =
    ErgoNamesContracts.ergonames_v1_config.contractScript
  val configContract = compiler.compileConfigContract(
    configContractScript,
    2,
    Array(
      Address.create("9hQT2zaqPpqqkpBR8mCtm9MSWiKEeBP1rX2hdSSPbXLAyWodBVF"),
      Address.create("9gZPevPc1jzKhtcDpGTMFUnsQFziBhtpdR53TKAVQVkVMcm8fwF")
    )
  )

  val regContractScript =
    ErgoNamesContracts.ergonames_v1_registry.contractScript
  val regContract = compiler.compileRegistryContract(
    regContractScript,
    registrySingletonToken,
    registrySingletonToken,
    subnamesContract,
    issuerContract,
    issuerContract,
    configContract
  )

  println(s"commit contract: ${commitContract.toAddress}")
  println(s"reveal contract: ${revealContract.toAddress}")
  println(s"registry contract: ${regContract.toAddress}")
  println(s"issuer contract: ${issuerContract.toAddress}")
  println(s"subnames contract: ${subnamesContract.toAddress}")
  println(s"config contract: ${configContract.toAddress}")
}

object testSP extends App {
  val msigAddress =
    "w7hVGKaq8uiK8qea21ck81TY13TFzGKfQXeQFyJdWEZt3W1JMoV65StuEszxEW8hrnoY6fJgKwTEjgWmPgSztSjkySayMRNWnHCAYF3sN93c1PCwAXSJLQCBKsseijFPwddcWLrRn4W91CcpvhPQHETKebmmGB1oJKfuSB68U3f"
  val msigContract = Address.create(msigAddress).toErgoContract
  val sp = msigContract.toAddress.getPublicKey
  println(sp)
}

object decode extends App {
  val hex = "AAANak5OMP8keWSaTD+PBUUESBAsxWS6FO95SOJZjVs="
  val str = Base64.decode(hex)
}
