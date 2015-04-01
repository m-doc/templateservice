package repositories

import java.sql.Blob

import models.{PersistentFilePath, PersistentFile}
import play.api.Logger
import play.api.db._
import play.api.Play.current

import anorm._
import anorm.SqlParser._


object PersistentFileDbRepository extends PersistentFileRepository {
  implicit def rowToByteArray: Column[Array[Byte]] = {
    Column.nonNull[Array[Byte]] { (value, meta) =>
      val MetaDataItem(qualified, nullable, clazz) = meta
      value match {
        case bytes: Array[Byte] => Right(bytes)
        case blob: Blob => Right(blob.getBytes(1, blob.length.asInstanceOf[Int]))
        case _ => Left(TypeDoesNotMatch("..."))
      }
    }
  }

  val mapper = {
    get[String]("path") ~
    get[Array[Byte]]("content") map {
      case path~content => PersistentFile(PersistentFilePath(path), content)
    }
  }

  def create(file: PersistentFile): Unit =  {
    DB.withConnection { implicit connection =>
      SQL(s"insert into file (path, content) values ('${file.path}', ${file.content})")
        .executeInsert()
      Logger.info(s"created file with path=${file.path.path} in db")

    }
  }

  def findByPath(path: String): Option[PersistentFile] =  {
    DB.withConnection { implicit connection =>
      SQL(s"select * from file where path = '${path}'")
        .as(mapper.singleOpt)
    }
  }
}
