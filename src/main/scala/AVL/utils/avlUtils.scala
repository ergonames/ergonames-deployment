package AVL.utils

import configs.{AVLJsonHelper, AvlJson, ErgoNamesInsert}
import work.lithos.plasma.collections.PlasmaMap

import scala.collection.mutable

object avlUtils {

  def exportAVL[K, V](
      map: PlasmaMap[K, V],
      tokenId: String,
      index: Long,
      name: String
  ): AVLJsonHelper = {

    val manifest: work.lithos.plasma.collections.Manifest =
      map.getManifest(255)

    val manifestHex: String = manifest.toHexStrings._1

    val manifestDigestHex: String =
      manifest.digest.map("%02x".format(_)).mkString
    val manifestSubTreeHex: Seq[String] =
      manifest.toHexStrings._2

    new AVLJsonHelper(
      tokenId,
      index,
      name,
      manifestHex,
      manifestDigestHex,
      manifestSubTreeHex.toArray
    )
  }

  def AVLFromExport[K, V](
      jsonData: AvlJson,
      map: PlasmaMap[K, V]
  ): Unit = {

    val manifest: work.lithos.plasma.collections.Manifest =
      work.lithos.plasma.collections.Manifest.fromHexStrings(
        jsonData.digestHex,
        jsonData.manifestHex,
        jsonData.subTreeHex.toSeq
      )

    map.loadManifest(manifest)

  }

}
