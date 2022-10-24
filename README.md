# scaffold

[![scaffold](https://circleci.com/gh/imborge/scaffold.svg?style=svg)](https://circleci.com/gh/imborge/scaffold)

A Clojure CRUD scaffolding library.

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
- [ ] Generate CRUD request handlers
        needs to map query function for it to be complete
- [x] Generate reitit routes
- [ ] Generate re-frame events and subscriptions

## License

Copyright © 2020 Børge André Jensen

Distributed under the [MIT License](http://opensource.org/licenses/MIT)
