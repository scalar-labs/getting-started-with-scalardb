package com.example.qa.dao;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.scalar.database.api.DistributedStorage;
import com.scalar.database.api.DistributedTransactionManager;
import com.scalar.database.config.DatabaseConfig;
import com.scalar.database.service.StorageModule;
import com.scalar.database.service.StorageService;
import com.scalar.database.service.TransactionModule;
import com.scalar.database.service.TransactionService;
import java.io.File;
import java.io.IOException;
import org.springframework.stereotype.Component;

@Component("com.example.qa.dao.LocalEnvironmentScalarDbFactory")
public class LocallyConfiguredCassandraFactory implements ScalarDbFactory {
  /** QA\src\main\resources\scalardb.properties) */
  private static final String SCALARDB_PROPERTIES = "scalardb.properties";

  private DatabaseConfig dbConfiguration = null;

  public LocallyConfiguredCassandraFactory() throws IOException {
    dbConfiguration =
        new DatabaseConfig(
            new File(getClass().getClassLoader().getResource(SCALARDB_PROPERTIES).getFile()));
  }

  @Override
  public DistributedStorage createDistributedStorage() {
    Injector injector = Guice.createInjector(new StorageModule(dbConfiguration));
    return injector.getInstance(StorageService.class);
  }

  @Override
  public DistributedTransactionManager createDistributedTransactionManager(
      DistributedStorage storage) {
    Injector injector = Guice.createInjector(new TransactionModule(dbConfiguration));
    return injector.getInstance(TransactionService.class);
  }
}
