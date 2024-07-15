package contracts

import enumeratum._

import scala.collection.immutable
import scala.io.Source

sealed trait ErgoNamesContracts extends EnumEntry {
  // Top Folder
  val domain: String = ""
  // Sub Folder
  val domainType: String = ""
  val contractType: ContractType = ContractTypes.None
  val fileExtension: String = ".es"
  val dirName: String = "contracts"
  val version: Long = 0

  lazy val fileName: String = if (version <= 1) {
    this.toString + fileExtension
  } else {
    this.toString + s"_v$version" + fileExtension
  }
  lazy val contractScript: String = get()

  def getPath: String =
    List(dirName, domain, domainType, contractType.plural, fileName)
      .filter(_.nonEmpty)
      .mkString("/")

  def get(): String = {
    val getViaPath: () => String = () => {
      val fullPath: String = getPath
      try {
        val contractSource =
          Source.fromResource(
            fullPath
          )

        val contractString = contractSource.mkString
        contractSource.close()

        contractString
      } catch {
        case _: NullPointerException =>
          throw new NullPointerException(s"$fullPath not found")
      }
    }

    val contractString: String = getViaPath()

    contractString
  }
}

object ErgoNamesContracts extends Enum[ErgoNamesContracts] {
  val values: immutable.IndexedSeq[ErgoNamesContracts] = findValues
  case object ergonames_v1_registry extends TokenBoxGuardScriptContract
  case object ergonames_v1_subname_registry extends TokenBoxGuardScriptContract
  case object ergonames_v1_issuer extends TokenBoxGuardScriptContract
  case object ergonames_v1_config extends TokenBoxGuardScriptContract
  case object ergonames_v1_reveal extends ProxyContractBoxGuardScriptContract
  case object ergonames_v1_commit extends CommitmentContractBoxGuardScriptContract
}

sealed trait TokenContract extends ErgoNamesContracts {
  override val domain: String = "ErgoNames"
}

sealed trait ProxyContract extends ErgoNamesContracts {
  override val domain: String = "ErgoNames"
}

sealed trait CommitmentContract extends ErgoNamesContracts {
  override val domain: String = "ErgoNames"
}

//</editor-fold>

//<editor-fold desc="Detailed Contract Types">
/** // ===== Detailed Level Contracts =====
 */
sealed trait TokenBoxGuardScriptContract extends TokenContract {
  override val contractType: ContractType = ContractTypes.BoxGuardScript
}

sealed trait ProxyContractBoxGuardScriptContract extends ProxyContract {
  override val contractType: ContractType = ContractTypes.ProxyContract
}

sealed trait CommitmentContractBoxGuardScriptContract extends ProxyContract {
  override val contractType: ContractType = ContractTypes.CommitmentContract
}
//</editor-fold>

//<editor-fold desc="Contract Type Enum">
/** Describes the different contract types as Enums
 */
sealed trait ContractType extends EnumEntry { val plural: String }

object ContractTypes extends Enum[ContractType] {
  val values: immutable.IndexedSeq[ContractType] = findValues

  case object ProxyContract extends ContractType {
    override val plural = "ProxyContracts"
  }

  case object CommitmentContract extends ContractType {
    override val plural = "ProxyContracts"
  }

  case object BoxGuardScript extends ContractType {
    override val plural = "BoxGuardScripts"
  }

  case object None extends ContractType { override val plural = "" }
}

//</editor-fold>
