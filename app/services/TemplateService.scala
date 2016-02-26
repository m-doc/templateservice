package services

import java.nio.charset.CharacterCodingException
import java.nio.file.Path

import controllers.Template._
import org.mdoc.fshell.ProcessResult
import org.mdoc.fshell.Shell
import org.mdoc.fshell.Shell.ShellSyntax
import org.fusesource.scalate.mustache.{Variable, Statement, MustacheParser}
import play.api.libs.json.Json

import scalaz._
import scalaz.concurrent.Task

import java.nio.file.{DirectoryStream, Paths}

object TemplateService {

  private[this] val checkIfTemplateExists: Path => Shell[Boolean] = Shell.fileExists(_)

  private[this] val readContent: Path => Shell[Either[CharacterCodingException, String]] =
    Shell.readAllBytes(_).map(_.decodeUtf8)

  private[this] val parseStatements: (String => List[Statement]) = new MustacheParser().parse(_)

  private[this] val filterVariables: List[Statement] => List[String] =
    _.flatMap(_ match {
      case v: Variable => Some(v.name.value)
      case _ => None
    })

  private[this] val parseVariables: String => List[String] = parseStatements.andThen(filterVariables)

  private[this] val readContentIfTemplateExists: Path => Shell[Option[Either[CharacterCodingException, String]]] =
    path => checkIfTemplateExists(path)
      .flatMap(exists =>
        if (exists) readContent(path).map(Some(_))
        else Free.point(None))

  private[this] val readContentAndParseVariables: Path => Shell[Option[Either[CharacterCodingException, List[String]]]] =
    readContentIfTemplateExists
      .andThen(shell => shell.map(option => option.map(either => either.right.map(parseVariables))))

  type GetPlaceholders = Reader[Path, Shell[Option[Either[CharacterCodingException, List[String]]]]]
  val getPlaceholders: GetPlaceholders = Reader {
    readContentAndParseVariables
  }

  //TODO switch to FSHell as soon as 'FilesInDirectory' is available
  case class TemplateView(name: String, sizeInBytes: Long)

  type GetTemplates = Reader[(Path, Seq[String]), Task[Seq[TemplateView]]]
  val getTemplates: GetTemplates = Reader {
    case (path, fileEndings) =>
      Task {
        val supportedFormatsFilterString = fileEndings.foldLeft("*.{")((z, ext) => z + "," + ext) + "}"
        val files = java.nio.file.Files.newDirectoryStream(path, supportedFormatsFilterString)
        try {
          import scala.collection.JavaConversions._
          files.iterator().toSeq
            .map(path => TemplateView(path.getFileName.toString, path.toFile.length))
            .sortBy(tv => tv.name)
        } finally {
          files.close()
        }
      }
  }
}
