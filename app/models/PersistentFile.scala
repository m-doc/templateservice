package models

case class PersistenFilePath(path: String) extends AnyVal

case class PersistentFile(path: PersistenFilePath, content: Array[Byte])
