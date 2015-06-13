package models

case class PersistentFilePath(path: String) extends AnyVal

case class PersistentFile(path: PersistentFilePath, content: Array[Byte])
