import models.{PersistentFile, PersistentFilePath}
import org.junit.runner.RunWith
import org.specs2.runner.JUnitRunner
import play.api.Application
import play.api.db.DB._
import repositories.RepositoryFactory

@RunWith(classOf[JUnitRunner])
class ApplicationDbModeSpec extends ApplicationSpec {

  def createTestFile(implicit app: Application) = withTransaction { implicit connection =>
    val file = PersistentFile(PersistentFilePath(fileName), fileContent)
    RepositoryFactory.persistentFileDbRepository.create(file)
    file.path.path
  }

  override def cleanUp(fileId: String) {
    //Nothing to do here since a in-memory db is used
  }

  override def useDb = "true"
}
