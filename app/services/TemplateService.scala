package services

import ammonite.ops.{read, exists, Path}
import org.fusesource.scalate.mustache.{Variable, Statement, MustacheParser}

import scalaz._
import scalaz.concurrent.Task

sealed trait TemplateOps[+A]

final case class CheckIfTemplateExists(path: Path) extends TemplateOps[Option[Path]]

final case class ReadTemplateContent(path: Option[Path]) extends TemplateOps[Option[String]]

object TemplateService {

  type TemplateOpsCoyoneda[A] = Coyoneda[TemplateOps, A]

  private[this] val checkIfTemplateExists = (path: Path) => Free.liftFC(CheckIfTemplateExists(path))

  private[this] val readContent = (path: Option[Path]) => Free.liftFC(ReadTemplateContent(path))

  private[this] val parseStatements = (content: Option[String]) =>
    content.map(new MustacheParser().parse(_))

  private[this] val filterVariables = (stmnts: Option[List[Statement]]) => stmnts.map(_.flatMap(_ match {
    case v: Variable => Some(v.name.value)
    case _ => None
  }))

  type GetPlaceholders = Reader[Path, Free[TemplateOpsCoyoneda, Option[List[String]]]]

  val getPlaceholders: GetPlaceholders = Reader(checkIfTemplateExists).map(
    _
      .flatMap(readContent)
      .map(parseStatements)
      .map(filterVariables)
  )
}

object TemplateOpsDefaultInterpreters {
  val taskInterpreter: TemplateOps ~> Task = new (TemplateOps ~> Task) {
    override def apply[A](op: TemplateOps[A]): Task[A] = op match {
      case CheckIfTemplateExists(path) => checkIfTemplateExistsTask(path)
      case ReadTemplateContent(maybePath) => readContentTask(maybePath)
    }

    def checkIfTemplateExistsTask(path: Path) = Task {
      if (exists ! path) Some(path) else None
    }

    def readContentTask(maybePath: Option[Path]) = Task {
      maybePath.map(read ! _)
    }
  }
}