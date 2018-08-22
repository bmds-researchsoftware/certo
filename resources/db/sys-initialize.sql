-- :name create-schema-sys
-- :command :execute
-- :result :raw
-- :doc Create schema sys
drop schema if exists sys cascade;
create schema sys;


-- :name create-extensions
-- :command :execute
-- :result :raw
-- :doc Create extensions
create extension if not exists "pgcrypto";
create extension if not exists btree_gist;

