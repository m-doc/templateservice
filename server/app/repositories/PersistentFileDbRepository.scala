package repositories

import java.sql.{Blob, PreparedStatement}

import anorm.SqlParser._
import anorm.{~, _}
import models.{PersistentFile, PersistentFilePath}
import play.api.Logger
import play.api.Play.current
import play.api.db._

import scalaz.effect.IO


object PersistentFileDbRepository extends PersistentFileRepository {
  val mapper = {
    get[String]("path") ~
      get[Array[Byte]]("content") map {
      case path ~ content => PersistentFile(PersistentFilePath(path), content)
    }
  }

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

  implicit object byteArrayToStatement extends ToStatement[Array[Byte]] {
    def set(s: PreparedStatement, i: Int, array: Array[Byte]): Unit = {
      s.setBlob(i, new javax.sql.rowset.serial.SerialBlob(array))
    }
  }

  def create(file: PersistentFile): IO[Unit] = IO {
    DB.withConnection { implicit connection =>
      val result = SQL("insert into file (path, content) values ({path}, {data})")
        .on(
          'path -> file.path.path,
          'data -> file.content
        ).execute()
      Logger.info(s"creating file with path=${file.path.path}")
    }
  }

  def findByPath(path: String): IO[Option[PersistentFile]] = IO {
    DB.withConnection { implicit connection =>
      SQL("select * from file where path = {path}")
        .on('path -> path)
        .as(mapper.singleOpt)
    }
  }

}