package repositories

object RepositoryFactory {

  val persistentFileDbRepository = PersistentFileDbRepository
  def persistentFileFsRespository(basePath: String) = new PersistentFileFsRepository(basePath)

}
