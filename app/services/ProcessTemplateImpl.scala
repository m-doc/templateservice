package services

import java.io.{StringWriter, StringReader}

import com.github.mustachejava.{MustacheException, DefaultMustacheFactory}
import services.TemplateService._

trait ProcessTemplateImpl {

  def template(contentResult: GetContentResult, vars: TemplateVars): ProcessTemplateResult =
    contentResult match {
      case content: TemplateContent => {
        try {
          ProcessedTemplate(processTemplateContent(content, vars))
        } catch {
          case e: MustacheException => InvalidTemplate(s"invalid template: ${e.getMessage}", e)
        }
      }
      case ie: InvalidTemplateEncoding => ie
      case nf: TemplateNotFound => nf
    }

  private[this] def processTemplateContent(content: TemplateContent, vars: TemplateVars) = {
    val mf = new DefaultMustacheFactory()
    val mustache = mf.compile(new StringReader(content.content), "")
    import scala.collection.JavaConversions._
    val result = mustache.execute(new StringWriter(), mapAsJavaMap(vars))
    ProcessedTemplateContent(result.toString)
  }
}
