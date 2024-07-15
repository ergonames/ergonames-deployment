{

    // ===== Contract Information ===== //
    // Name: ErgoNames Config Contract
    // Description: Contract for the config box of the ergonames protocol, used for determining payment options.
    // Version: 1.0.0
    // Author: Luca D'Angelo

    // ===== Box Contents ===== //
    // Tokens
    // 1. (ConfigSingleton, 1)
    // Registers
    // R4: AvlTree  ConfigAvlTree

    // ===== Relevant Transactions ===== //
    // 1. Mint ErgoName
    // Inputs: Registry, Reveal, Commit
    // Data Inputs: ErgoDexErg2SigUsd, ?ErgoDexErg2Token, ?Config
    // Outputs: Registry, SubNameRegistry, ErgoNameIssuer, ErgoNameFee, MinerFee, TxOperatorFee
    // Context Variables: None

    // ===== Compile Time Constants ($) ===== //
    // $minRequiredSignatures: Int
    // $addresses: Coll[SigmaProp]

    // ===== Context Variables (_) ===== //
    // None

    // ===== Relevant Variables ===== //
    // None

    val address1 = PK("3WvubspBMttcKU97e6oAKdjgaXmoVUDDi6aKdt3in9zTvzSUTxto")
    val address2 = PK("3WxJrwDLXgGE53KpdJ2nSjSMRdXaDWh7Fdz9MY2Zh37UAwfLXzBU")

    atLeast(1, Coll(address1, address2))
}