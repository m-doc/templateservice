package models

import java.util.UUID

case class PersistentFileId(value: String = UUID.randomUUID().toString.replaceAll("-", "")) extends AnyVal

case class PersistentFile(id: PersistentFileId = PersistentFileId(), path: String, content: Array[Byte])
