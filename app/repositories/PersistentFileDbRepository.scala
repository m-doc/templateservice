package repositories

import java.sql.Blob

import models.PersistentFile
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
      case path~content => PersistentFile(path, content)
    }
  }

  def create(file: PersistentFile): Unit =  {
    DB.withConnection { implicit connection =>
      SQL("insert into file (path, content) values (${file.path}, ${file.content})")
        .executeInsert()
    }
  }

  def findByPath(path: String): Option[PersistentFile] =  {
    DB.withConnection { implicit connection =>
      SQL("select * from file where path = ${path}")
        .as(mapper.singleOpt)
    }
  }
}
