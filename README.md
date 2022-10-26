# scaffold

[![scaffold](https://circleci.com/gh/imborge/scaffold.svg?style=svg)](https://circleci.com/gh/imborge/scaffold)

A Clojure CRUD scaffolding library.

**THIS IS A WORK IN PROGRESS**

The vision for the library is to be general enough to be used with e.g. Luminus or kit, 
but the focus is currently on the following stack:

- Ring
  - metosin/ring-http-response
- Reitit
- HugSQL
- (reagent/re-frame)

## TODO

- [x] Generate migrations to create table(s)
  - [x] Column constraints
  - [x] Table constraints
  - [x] Data types
- [x] Generate basic CRUD queries
- [x] Generate CRUD request handlers
  - [x] Write tests
- [x] Generate reitit routes
  - [ ] Write tests
- [ ] Generate re-frame events and subscriptions

## Example usage

Here's an example scaffolding migrations, queries, routes and request handlers for
a `user` table:

```clojure
(ns user
  (:require [scaffold.core :as scaffold]))
  
(def scaffold-conf
  (merge scaffold.core/sample-configuration
         {:migrations/dir          "resources/migrations/"
          :hugsql/queries-filename "resources/sql/queries.sql"
          :reitit/routes-file      "src/clj/routes.clj"
          :hugsql/queries-append?  true
          :hugsql/query-fn         '(:query-fn (utils/route-data request))}))


(def user-table
  {:name "users"
   :columns
   [["id" [:uuid] [[:primary-key] [:default "uuid_generate_v4()"]]]
    ["email" [:citext] [[:unique]]]
    ["password" [:text] []]]})
    
(scaffold/scaffold! scaffold-conf user-table)
```

## License

Copyright © 2020 Børge André Jensen

Distributed under the [MIT License](http://opensource.org/licenses/MIT)
