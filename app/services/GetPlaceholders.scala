package services

import java.nio.charset.CharacterCodingException
import java.nio.file.Path
import org.fusesource.scalate.mustache.{ MustacheParser, Statement, Variable }
import org.mdoc.fshell._
import scalaz._

trait GetPlaceholders {
  private[this] val checkIfTemplateExists: Path => Shell[Boolean] = Shell.fileExists

  private[this] val readContent: Path => Shell[Either[CharacterCodingException, String]] =
    Shell.readAllBytes(_).map(_.decodeUtf8)

  private[this] val parseStatements: (String => List[Statement]) = new MustacheParser().parse

  private[this] val filterVariables: List[Statement] => List[String] =
    _.flatMap(_ match {
      case v: Variable => Some(v.name.value)
      case _ => None
    })

  private[this] val parseVariables: String => List[String] = parseStatements.andThen(filterVariables)

  private[this] val readContentIfTemplateExists: Path => Shell[Option[Either[CharacterCodingException, String]]] =
    path => checkIfTemplateExists(path)
      .flatMap(exists =>
        if (exists) {
          readContent(path).map(Some(_))
        } else {
          Free.point(None)
        })
  private[this] val readContentAndParseVariables: Path => Shell[Option[Either[CharacterCodingException, List[String]]]] =
    readContentIfTemplateExists
      .andThen(shell => shell.map(option => option.map(either => either.right.map(parseVariables))))

  type GetPlaceholders = Reader[Path, Shell[Option[Either[CharacterCodingException, List[String]]]]]
  val getPlaceholders: GetPlaceholders = Reader {
    readContentAndParseVariables
  }
}
