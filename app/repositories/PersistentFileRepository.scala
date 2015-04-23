package repositories

import models.PersistentFile

import scalaz.effect.IO

trait PersistentFileRepository {

  def create(file: PersistentFile): IO[Unit]

  def findByPath(path: String): IO[Option[PersistentFile]]

}
