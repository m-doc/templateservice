package services

import java.nio.charset.CharacterCodingException
import java.nio.file.Path
import org.fusesource.scalate.mustache.{ MustacheParser, Statement, Variable }
import org.mdoc.fshell._
import scalaz._
import scalaz.Scalaz._

trait GetPlaceholdersShellImpl {
  private[this] val checkIfTemplateExists: Path => Shell[Boolean] = Shell.fileExists

  type Content = String

  type FailureOrContent = Either[CharacterCodingException, Content]
  private[this] val readContent: Path => Shell[FailureOrContent] =
    Shell.readAllBytes(_).map(_.decodeUtf8)

  type Statements = List[Statement]
  private[this] val parseStatements: (Content => Statements) = new MustacheParser().parse

  type Variables = List[String]
  private[this] val filterVariables: Statements => Variables =
    _.flatMap(_ match {
      case v: Variable => Some(v.name.value)
      case _ => None
    })

  private[this] val parseVariables: Content => Variables = parseStatements.andThen(filterVariables)

  private[this] val readContentIfTemplateExists: Path => Shell[Option[FailureOrContent]] =
    path => checkIfTemplateExists(path)
      .flatMap(exists =>
        if (exists) {
          readContent(path).map(Some(_))
        } else {
          Free.point(None)
        })

  type FailureOrVariables = Either[CharacterCodingException, Variables]
  private[this] val readContentAndParseVariables: Path => Shell[Option[FailureOrVariables]] =
    readContentIfTemplateExists
      .andThen(shell => shell.map(option => option.map(either => either.right.map(parseVariables))))

  def getPlaceholders(path: Path): Shell[GetPlaceholdersResult] = readContentAndParseVariables(path)
    .map { option =>
      option.map { either =>
        either.bimap(_ => InvalidTemplateEncoding, new Placeholders(_)).merge
      }.getOrElse(TemplateNotFound)
    }
}
