package services

import java.nio.file.Paths
import services.TemplateServiceOp.{TemplateFormat, TemplateId, Placeholder}
import scalaz.concurrent.Task
import scalaz.{~>, Coyoneda}
import org.mdoc.fshell.Shell.ShellSyntax

object TemplateService {

}

sealed trait GetPlaceholdersResult

case object TemplateNotFound extends GetPlaceholdersResult

case object InvalidTemplateEncoding extends GetPlaceholdersResult

case class Placeholders(placeholders: Seq[Placeholder]) extends GetPlaceholdersResult

sealed trait TemplateServiceOp[A]

case class GetPlaceholders(templateId: TemplateId) extends TemplateServiceOp[GetPlaceholdersResult]

case class GetTemplates(templateId: TemplateId, templateFormats: Seq[TemplateFormat]) extends TemplateServiceOp[Seq[TemplateView]]

object TemplateServiceOp {
  type Placeholder = String
  type TemplateId = String
  type TemplateFormat = String
  type FreeTemplateServiceOp[A] = Coyoneda[TemplateServiceOp, A]
}

object TemplateServiceFileSystemInterpreter
  extends (TemplateServiceOp ~> Task)
  with GetPlaceholdersShellImpl
  with GetTemplatesShellImpl {

  override def apply[A](fa: TemplateServiceOp[A]): Task[A] = fa match {
    case GetPlaceholders(templateId) => {
      getPlaceholders(Paths.get(templateId)).runTask
    }
    case GetTemplates(templateId, formats) => {
      getTemplates(Paths.get(templateId), formats)
    }
  }
}
