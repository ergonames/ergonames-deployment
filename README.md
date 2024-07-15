# ErgoNames Contracts Deployment CLI Tool

This tool facilitates the deployment of ErgoNames contracts with customizable values. It also allows you to compile contract addresses, enabling verification of any contract values or constants.

An additional feature, particularly useful for contract testing, is the clone ability. This allows you to clone any box from the mainnet to the testnet with a custom ERG value. This is especially helpful for testing contracts that require boxes only available on mainnet, such as the USD oracle.

## Setup

1. Copy `config.json.example` to `config.json` and fill in the values.
2. Most defaults are set up; the primary requirement is the mnemonic.
3. The mnemonic is not necessary for contract compiling.
4. The mnemonic password can be left blank if the mnemonic is not encrypted (which is usually the case).
5. The `addressIndex` can be left as `0`. If you have multiple addresses in your wallet, you can change this to the index of the address you want to use.

## Running the Tool

### Using Scala

1. Install [Scala](https://www.scala-lang.org/download/)
2. Install [sbt](https://www.scala-sbt.org/download.html)
3. Run the following command in the project's root directory:
   ```
   sbt "runMain app.Main"
   ```

### Using Jar

1. Install Java
2. Download the [latest release](https://github.com/mgpai22/greasy-phoenix/releases/latest)
3. Run the jar file using the following command structure:
   ```
   java -jar ergonames-deployment-*.jar --conf <path to conf> <additional flags>
   ```

#### Examples:

- Cloning a box:
  ```
  java -jar ergonames-deployment-*.jar --conf <path to conf> --clone --boxID <boxId> --nanoErgs <optional, else box value will be exactly copied>
  ```

- Initializing Contracts:
  ```
  java -jar ergonames-deployment-*.jar --conf <path to conf> --initialize --metadataPath <metadataPath> --poolsPath <poolsPath> --contractSaveFilePath <optional, where contract addresses are saved>
  ```
  
- Compiling Registry Contract:
  ```
  java -jar ergonames-deployment-*.jar --conf <path to conf> --registry --metadataPath <metadataPath>
  ```

## Building the Jar

1. Ensure Scala and sbt are installed
2. Run the following command in the project's root directory:
   ```
   sbt clean assembly
   ```
3. The generated jar file will be located in the project's root directory

## Available Commands

- `--initialize`: Initialize all Ergonames contracts
- `--registry`: Compile the registry contract
- `--clone`: Clone a box from mainnet to testnet
- `--issuer`: Compile the issuer contract
- `--config`: Compile the config contract
- `--subnames`: Compile the subnames contract

## Additional Flags

- `--conf`: Path to the configuration file (required)
- `--dryrun`: Test the transaction without submitting it to the network
- `--metadataPath`: Path to metadata file (required for some operations)
- `--poolsPath`: Path to pools configuration file (required for some operations)
- `--contractSaveFilePath`: Path to save compiled contracts (optional)
- `--collectionTokenId`: ID of the collection token (required for some operations)
- `--registrySingletonTokenId`: ID of the registry singleton token (required for some operations)
- `--commitContractAddress`: Address of the commit contract (required for some operations)
- `--boxId`: ID of the box to clone (required for clone operation)
- `--nanoErgs`: Custom ERG value for cloned box (optional for clone operation)
