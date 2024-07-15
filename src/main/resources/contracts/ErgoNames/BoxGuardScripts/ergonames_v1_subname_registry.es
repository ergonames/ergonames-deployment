{
    // ===== Contract Description ===== //
    // Name: ErgoNames Subname Registry Contract
    // Description: Contract managing the minting of the ErgoName subname.
    // Version: 1.0.0
    // Author: Luca D'Angelo (ldgaetano@protonmail.com)

    // ===== Box Contents ===== //
    // Tokens
    // 1. (SubNameRegistrySingletonTokenId, 1L)
    // Registers
    // R4: AvlTree              RegistryAvlTree
    // R5: (Coll[Byte], Long)   PreviousState
    // R6: Coll[Byte]           ParentErgoNameTokenId

    // ===== Relevant Transactions ===== //
    // 1. Mint SubName
    // Inputs: ParentSubNameRegistry, ChildSubnameRegistryNFT, ErgoNameNFT
    // Data Inputs: None
    // Outputs: ParentSubNameRegistry, ChildSubNameRegistry, SubNameNFT, ErgoNameNFT, MinerFee, TxOperatorFee
    // Context Variables: SubNameHash, InerstionProof, LookUpProof

    // ===== Compile Time Constants ($) ===== //
    // None

    // ===== Context Variables (_) ===== //
    // _subNameHash: Coll[Byte]     - Hash of the ErgoName SubName to register.
    // _insertionProof: Coll[Byte]  - Proof that the SubNameHash and SubNameTokenId were inserted into the registry avl tree.
    // _lookupProof: Coll[Byte]     - Proof for getting a value from the config avl tree.

    // ===== User-Defined Functions ===== //
    // def isValidAscii: (Coll[Byte] => Boolean)

    // ===== Relevant Variables ===== //
    val previousRegistry: AvlTree           = SELF.R4[AvlTree].get
    val previousState: (Coll[Byte], Long)   = SELF.R5[(Coll[Byte], Long)].get
    val parentErgoNameTokenId: Coll[Byte]   = SELF.R6[Coll[Byte]].get

    val _subNameHash: Coll[Byte]    = getVar[Coll[Byte]](0).get
    val _insertionProof: Coll[Byte] = getVar[Coll[Byte]](1).get

    // ===== User-Defined Functions ===== //
    def isValidAscii(chars: Coll[Byte]): Boolean = {
        // Allowed ASCII characters (based on x.com handle format)
        val zero: Byte          = 48         // Numbers lower-bound
        val nine: Byte          = 57         // Numbers upper-bound
        val A: Byte             = 65         // Upper-case letters lower-bound
        val Z: Byte             = 90         // Upper-case letters upper-bound
        val a: Byte             = 97         // Lower-case letters lower-bound
        val z: Byte             = 122        // Lower-case letters upper-bound
        val underscore: Byte    = 95         // The only non-alphanumeric character allowed

        // All characters must be a digit, an uppercase letter, a lowercase letter, an underscore, or any combination thereof.
        chars.forall { (char: Byte) =>
            val isDigit: Boolean            = char >= zero && char <= nine
            val isUpperCaseLetter: Boolean  = char >= A && char <= Z
            val isLowerCaseLetter: Boolean  = char >= a && char <= z
            val isUnderscore: Boolean       = char == underscore

            isDigit || isUpperCaseLetter || isLowerCaseLetter || isUnderscore
        }
    }

    // ===== Mint SubName Tx ===== //
    val validMintSubNameTx: Boolean = {

        // Inputs
        val childSubNameRegistryBoxIn: Box  = INPUTS(1)
        val ergoNameNftBoxIn: Box           = INPUTS(2)

        // Outputs
        val parentSubNameRegistryBoxOut: Box            = OUTPUTS(0)
        val childSubNameRegistryBoxOut: Box             = OUTPUTS(1)
        val subNameNftBoxOut: Box                       = OUTPUTS(2)
        val ergoNameNftBoxOut: Box                      = OUTPUTS(3)
        val minerFeeBoxOut: Box                         = OUTPUTS(4)
        val txOperatorFeeBoxOut: Box                    = OUTPUTS(5)

        // Relevant Variables
        val subNameTokenId: Coll[Byte]              = SELF.id // Thus all ErgoName token ids will be unique.

        val subNameBytes: Coll[Byte]                = childSubNameRegistryBoxOut.R4[Coll[Byte]].get

        val receiverPKGroupElement: GroupElement    = childSubNameRegistryBoxOut.R5[GroupElement].get
        val receiverPKSigmaProp: SigmaProp          = proveDlog(receiverPKGroupElement)

        val validParentErgoName: Boolean = (ergoNameNftBoxIn.tokens(0)._1 == parentErgoNameTokenId)

        val validSubNameFormat: Boolean = {

            allOf(Coll(
                (subNameBytes.size > 0),
                isValidAscii(subNameBytes)
            ))

        }

        val validSubNameRegistryUpdate: Boolean = {

            val validStateUpdate: Boolean = {

                val newState: (Coll[Byte], Long)        = parentSubNameRegistryBoxOut.R5[(Coll[Byte], Long)].get

                val validSubNameTokenIdUpdate: Boolean  = (newState._1 == subNameTokenId)
                val validIndexIncrement: Boolean        = (newState._2 == previousState._2 + 1L)

                allOf(Coll(
                    validSubNameTokenIdUpdate,
                    validIndexIncrement
                ))

            }

            val validSubNameInsertion: Boolean = {

                val newRegistry: AvlTree = previousRegistry.insert(Coll((_subNameHash, subNameTokenId)), _insertionProof).get

                (parentSubNameRegistryBoxOut.R4[AvlTree].get.digest == newRegistry.digest)

            }

            val validSelfRecreation: Boolean = {

                allOf(Coll(
                    (parentSubNameRegistryBoxOut.value == SELF.value),
                    (parentSubNameRegistryBoxOut.propositionBytes == SELF.propositionBytes),
                    (parentSubNameRegistryBoxOut.tokens(0) == SELF.tokens(0)),
                    (parentSubNameRegistryBoxOut.R6[Coll[Byte]].get == SELF.R6[Coll[Byte]].get)
                ))

            }

            allOf(Coll(
                validStateUpdate,
                validSubNameInsertion,
                validSelfRecreation
            ))

        }

        val validChildSubNameRegistryBoxOut: Boolean = {

            val emptyDigest: Coll[Byte] = fromBase16("4ec61f485b98eb87153f7c57db4f5ecd75556fddbc403b41acf8441fde8e160900")

            val validR5: Boolean = {
                val r5 = childSubNameRegistryBoxOut.R5[(Coll[Byte], Long)].get
                r5._1.size == 0 && r5._2 == 0L
            }

            allOf(Coll(
                (childSubNameRegistryBoxOut.propositionBytes == SELF.propositionBytes),
                (childSubNameRegistryBoxOut.tokens(0) == (childSubNameRegistryBoxIn.tokens(0)._1, 1L)),
                (childSubNameRegistryBoxOut.R4[AvlTree].get.digest == emptyDigest),
                validR5,
                (childSubNameRegistryBoxOut.R6[Coll[Byte]].get == subNameTokenId)
            ))

        }

        val validSubNameNftBoxOut: Boolean = {

            allOf(Coll(
                (subNameNftBoxOut.propositionBytes == receiverPKSigmaProp.propBytes),
                (subNameNftBoxOut.tokens(0) == (subNameTokenId, 1L))
            ))

        }

        val validErgoNameNftBoxOut: Boolean = {

            allOf(Coll(
                (ergoNameNftBoxOut.propositionBytes == receiverPKSigmaProp.propBytes),
                (ergoNameNftBoxOut.tokens(0) == ergoNameNftBoxIn.tokens(0))
            ))

        }

        allOf(Coll(
            validParentErgoName,
            validSubNameFormat,
            validSubNameRegistryUpdate,
            validChildSubNameRegistryBoxOut,
            validSubNameNftBoxOut,
            validErgoNameNftBoxOut
        ))

    }

    val address1 = PK("3WvubspBMttcKU97e6oAKdjgaXmoVUDDi6aKdt3in9zTvzSUTxto")
    val address2 = PK("3WxJrwDLXgGE53KpdJ2nSjSMRdXaDWh7Fdz9MY2Zh37UAwfLXzBU")

    val $ergonameMultiSigSigmaProp = atLeast(1, Coll(address1, address2))

    sigmaProp(validMintSubNameTx) || $ergonameMultiSigSigmaProp
}