package utils

import org.ergoplatform.appkit.Address
import scorex.crypto.hash

import java.nio.charset.StandardCharsets

object ErgoNamesUtils {

  def generateSecretHash(
      secretString: String,
      recipientAddress: Address,
      nameToRegister: String
  ): Array[Byte] = {

    val secretStringHash: Array[Byte] =
      hash.Blake2b256(secretString.getBytes(StandardCharsets.UTF_8))

    val nameToRegisterBytes: Array[Byte] =
      nameToRegister.getBytes(StandardCharsets.UTF_8)

    val recipientAddressPropBytes = recipientAddress.asP2PK().script.bytes

    hash.Blake2b256(
      secretStringHash ++ recipientAddressPropBytes ++ nameToRegisterBytes
    )
  }

}
