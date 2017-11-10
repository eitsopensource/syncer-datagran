# eits-syncer-datagran

A fork of: https://github.com/eitsopensource/eits-syncer used in Datagran mobile application.

# Change Log

##1.0.0-RELEASE
- Remove the auto-sync functionality when inserting, updating and deleting revisions.
- Added a construtor in Watcher class for use Observer to monitor the syncronizing process
- Added methods to QueryRevisionService class for improve the querying system
    - `public IQueryRevisionService where( String field, Object value, String operator )` to use custom operators.
    - `public IQueryRevisionService where( String field )` to use with operators methods (explained in sequence).
    - `public IQueryRevisionService eq( Object value )` operator method that compares if value is equals.
    - `public IQueryRevisionService gt( Object value )` operator method that compares if value is greater than.
    - `public IQueryRevisionService lt( Object value )` operator method that compares if value is lower than.
    - `public IQueryRevisionService goe( Object value )` operator method that compares if value is greater or equals.
    - `public IQueryRevisionService loe( Object value )` operator method that compares if value is lower or equals.
    - `public IQueryRevisionService castAs( Object field, Srting castAs )` used to cast field values. See: [SQLite](https://sqlite.org/lang_expr.html).
    - `private Object processValue ( Object value )` used to process values passed to where methods.
- Added `SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, true` to the ObjectMapper configuration.

##1.0.1-RELEASE
    - Added `DeserializationFeature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE, false` to the ObjectMapper configuration.
