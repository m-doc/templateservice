# --- !Ups

create table file (
  id VARCHAR(32) not null,
  path VARCHAR(255) not null,
  content BLOB not null,
  PRIMARY KEY (id),
);

create unique index uidx_file_path on file (path);


# --- !Downs

drop TABLE file;