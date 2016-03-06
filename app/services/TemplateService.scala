package services

import java.nio.file.Paths
import services.TemplateService._
import scalaz.concurrent.Task
import scalaz.{~>, Coyoneda}
import org.mdoc.fshell.Shell.ShellSyntax

object TemplateService {
  type Placeholder = String
  type TemplateId = String
  type TemplateFormat = String
  type FreeTemplateServiceOp[A] = Coyoneda[TemplateServiceOp, A]
  type Content = String
  type TemplateVars = Map[String, String]
}

sealed trait ProcessTemplateResult

case class ProcessedTemplate(content: Content) extends ProcessTemplateResult

case class InvalidTemplate(errorMsg: String, exception: Exception) extends ProcessTemplateResult

sealed trait GetPlaceholdersResult

case object TemplateNotFound extends GetPlaceholdersResult with ProcessTemplateResult

case object InvalidTemplateEncoding extends GetPlaceholdersResult

case class Placeholders(placeholders: Seq[Placeholder]) extends GetPlaceholdersResult

sealed trait TemplateServiceOp[A]

case class GetPlaceholders(templateId: TemplateId) extends TemplateServiceOp[GetPlaceholdersResult]

case class GetTemplates(templateId: TemplateId, templateFormats: Seq[TemplateFormat]) extends TemplateServiceOp[Seq[TemplateView]]

case class ProcessTemplate(templateId: TemplateId, vars: TemplateVars) extends TemplateServiceOp[ProcessTemplateResult]

object TemplateServiceFileSystemInterpreter
  extends (TemplateServiceOp ~> Task)
  with GetPlaceholdersShellImpl
  with GetTemplatesShellImpl
  with ProcessTemplateImpl {

  override def apply[A](fa: TemplateServiceOp[A]): Task[A] = fa match {
    case GetPlaceholders(templateId) =>
      getPlaceholders(Paths.get(templateId)).runTask
    case GetTemplates(templateId, formats) =>
      getTemplates(Paths.get(templateId), formats)
    case ProcessTemplate(templateId, vars) =>
      template(templateId, vars)
  }
}
