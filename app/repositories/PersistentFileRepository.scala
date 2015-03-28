package repositories

import models.PersistentFile

trait PersistentFileRepository {

  def create(file: PersistentFile): Unit

  def findByPath(path: String): Option[PersistentFile]

}
