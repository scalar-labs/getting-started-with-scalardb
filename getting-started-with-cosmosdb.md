# Set up the environment for your first application using Scalar DB on Cosmos DB
- [Set up the environment for your first application using Scalar DB](#set-up-the-environment-for-your-first-application-using-scalar-db-on-cosmos-db)
    - [Set up the environment](#set-up-the-environment)
        - [Configure the Cassandra connection](#configure-the-cassandra-connection)
    - [Getting Started with Scalar DB](#please-follow-getting-started-with-scalar-dbgetting-started-with-scalardbmd)
        
## Set up the environment

Please install [Scalar DB prerequisites](https://github.com/scalar-labs/scalardb/blob/master/docs/getting-started-with-cosmosdb.md#install-prerequisites) and [Node.js](https://nodejs.org/en/download/) before running this demonstration.
           
### Configure the Cosmos DB connection
    
The **scalardb.properties** (backend/QA/src/main/resources/scalardb.properties) file holds the configuration for Scalar DB. Basically, Cosmos DB account_uri and primary_key will get from Azure Cosmos DB account.
    
```
# Comma separated contact points
scalar.db.contact_points=<YOUR_ACCOUNT_URI>

# Port number for all the contact points. Default port number for each database is used if empty.
#scalar.db.contact_port=

# Credential information to access the database
scalar.db.username=
scalar.db.password=<YOUR_ACCOUNT_PASSWORD>

# Storage implementation. Either cassandra or cosmos can be set. Default storage is cassandra.
scalar.db.storage=cosmos
```
    
## Please follow [Getting Started with Scalar DB](getting-started-with-scalardb.md)
