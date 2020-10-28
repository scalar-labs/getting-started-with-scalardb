# Set up the environment for your first application using Scalar DB on Cassandra
- [Set up the environment for your first application using Scalar DB on Cassandra](#set-up-the-environment-for-your-first-application-using-scalar-db-on-cassandra)
    - [Set up the environment](#set-up-the-environment)
        - [Configure the Cassandra connection](#configure-the-cassandra-connection)
    - [Getting Started with Scalar DB](#please-follow-getting-started-with-scalar-dbgetting-started-with-scalardbmd)
        
## Set up the environment

Please install [Scalar DB prerequisites](https://github.com/scalar-labs/scalardb/blob/master/docs/getting-started-with-cassandra.md#install-prerequisites) and [Node.js](https://nodejs.org/en/download/) before running this demonstration.

### Configure the Cassandra connection

The **scalardb.properties** (backend/QA/src/main/resources/scalardb.properties) file holds the configuration for Scalar DB. Basically, it describes the Cassandra installation that will be used.

```
# Comma separated contact points
scalar.db.contact_points=localhost

# Port number for all the contact points. Default port number for each database is used if empty.
#scalar.db.contact_port=

# Credential information to access the database
scalar.db.username=cassandra
scalar.db.password=cassandra

# Storage implementation. Either cassandra or cosmos can be set. Default storage is cassandra.
#scalar.db.storage=cassandra
```

## Please follow [Getting Started with Scalar DB](getting-started-with-scalardb.md)
