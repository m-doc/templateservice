package services

import org.fusesource.scalate._
import services.TemplateService._

import scalaz.concurrent.Task

trait ProcessTemplateImpl {

  private[this] val templateEngine = new TemplateEngine

  def template(path: String, vars: TemplateVars): Task[ProcessTemplateResult] = Task {
    try {
      ProcessedTemplate(templateEngine.layout(path, vars))
    } catch {
      case _: ResourceNotFoundException => TemplateNotFound
      case e: TemplateException => InvalidTemplate(s"invalid template: ${e.getMessage}", e)
    }
  }
}
