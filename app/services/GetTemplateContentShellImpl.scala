package services

import java.nio.charset.Charset
import java.nio.file.Path

import scalaz.Free
import scalaz.Scalaz._

trait GetTemplateContentShellImpl {

  import org.mdoc.fshell._

  def fileContent(path: Path): Shell[GetContentResult] = Shell.fileExists(path).flatMap(exists =>
    if (exists) {
      Shell.readAllBytes(path)
        .map {
          _
            .decodeString(Charset.forName("UTF-8"))
            .bimap(
              _ => InvalidTemplateEncoding(path.toString, "UTF-8"),
              TemplateContent(_)
            )
            .merge
        }
    } else {
      Free.point(TemplateNotFound(path.toString))
    })
}
