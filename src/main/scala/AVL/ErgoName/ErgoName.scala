package AVL.ErgoName

import scorex.crypto.hash.Blake2b256

case class ErgoName(name: String) {

  def toErgoNameHash: ErgoNameHash = ErgoNameHash(
    Blake2b256.hash(name.getBytes("UTF-8"))
  )
}
