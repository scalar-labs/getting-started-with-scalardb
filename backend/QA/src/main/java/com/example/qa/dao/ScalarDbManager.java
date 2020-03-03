package com.example.qa.dao;

import com.scalar.database.api.DistributedStorage;
import com.scalar.database.api.DistributedTransactionManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
public class ScalarDbManager {
  private final DistributedStorage storage;
  private final DistributedTransactionManager transactionManager;

  @Autowired
  public ScalarDbManager(
      @Qualifier("com.example.qa.dao.LocalEnvironmentScalarDbFactory")
          ScalarDbFactory scalarDbFactory) {
    storage = scalarDbFactory.createDistributedStorage();
    transactionManager = scalarDbFactory.createDistributedTransactionManager(storage);
  }

  public DistributedStorage getDistributedStorage() {
    return storage;
  }

  public DistributedTransactionManager getDistributedTransactionManager() {
    return transactionManager;
  }
}
