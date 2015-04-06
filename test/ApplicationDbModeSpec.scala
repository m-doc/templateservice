import models.{PersistentFile, PersistentFilePath}
import org.junit.runner.RunWith
import org.specs2.runner.JUnitRunner
import play.api.Application
import play.api.db.DB._
import repositories.RepositoryFactory

@RunWith(classOf[JUnitRunner])
class ApplicationDbModeSpec extends ApplicationSpec {

  def createTestFile(implicit app: Application) = withTransaction { implicit connection =>
    RepositoryFactory.persistentFileDbRepository.create(PersistentFile(PersistentFilePath(fileName), fileContent))
  }

  override def cleanUp(fileId: String) {
    //Nothing to do here since a in-memory db is used
  }

  override def useDb = "true"
}
