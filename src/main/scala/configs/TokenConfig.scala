package configs

import com.google.gson.GsonBuilder
import java.io.{File, FileWriter}
import scala.io.Source

case class Token(
    id: String,
    name: String,
    decimal: Int
)

object TokenConf {
  private val gson = new GsonBuilder().setPrettyPrinting().create()

  def read(filePath: String): Array[Token] = {
    val jsonString: String = Source.fromFile(filePath).mkString
    gson.fromJson(jsonString, classOf[Array[Token]])
  }

  private def write(filePath: String, tokens: Array[Token]): Unit = {
    val jsonString: String = gson.toJson(tokens)
    val fileWriter = new FileWriter(filePath)
    fileWriter.write(jsonString)
    fileWriter.close()
  }

  def updateOrAddToken(filePath: String, token: Token): Unit = {
    val tokens: Array[Token] = read(filePath)
    val existingTokenIndex: Int = tokens.indexWhere(_.id == token.id)

    val updatedTokens: Array[Token] = if (existingTokenIndex != -1) {
      tokens.updated(existingTokenIndex, token)
    } else {
      tokens :+ token
    }

    write(filePath, updatedTokens)
  }
}
