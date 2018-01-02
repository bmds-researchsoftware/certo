-- $ sudo -u postgres createuser certo -d -s -P

-- Add the following line to pg_hba.conf:
-- local	 all		 certo		 			 md5

-- $ sudo -u postgres service postgresql reload

-- $ createdb -U certo certo

-- Add the following line to .pgpass:
-- localhost:5432:certo:certo:PASSWORD

-- $ cd certo/resources

-- $ psql -U certo -d certo

-- certo=# \i createdb.sql

drop schema if exists study cascade;

create schema study;

create table study.subjects (
  id serial8 primary key,
  first_name text,  
  last_name text,
  birth_date date,
  created_at timestamptz default current_timestamp,
  updated_at timestamptz default current_timestamp
);

insert into study.subjects (id, first_name, last_name, birth_date, created_at, updated_at) values (default, 'Martha', 'Washington', '1731-06-13', default, default);
insert into study.subjects (id, first_name, last_name, birth_date, created_at, updated_at) values (default, 'Abigail', 'Adams', '1744-11-22', default, default);
insert into study.subjects (id, first_name, last_name, birth_date, created_at, updated_at) values (default, 'Martha', 'Jefferson', '1748-10-30', default, default);

