# --- !Ups

create table file (
  path VARCHAR(255) not null,
  content BLOB not null,
  PRIMARY KEY (path),
);


# --- !Downs

drop TABLE file;