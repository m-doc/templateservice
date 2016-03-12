package services

import java.io.{StringWriter, StringReader}

import com.github.mustachejava.{MustacheException, DefaultMustacheFactory}
import services.TemplateService._
import scalaz.concurrent.Task

trait ProcessTemplateImpl {

  def template(content: String, vars: TemplateVars): Task[ProcessTemplateResult] = Task {
    try {
      ProcessedTemplate(processTemplateContent(content, vars))
    } catch {
      case e: MustacheException => InvalidTemplate(s"invalid template: ${e.getMessage}", e)
    }
  }

  private[this] def processTemplateContent(content: Content, vars: TemplateVars) = {
    val mf = new DefaultMustacheFactory()
    val mustache = mf.compile(new StringReader(content), "")
    import scala.collection.JavaConversions._
    val result = mustache.execute(new StringWriter(), mapAsJavaMap(vars))
    result.toString
  }
}
