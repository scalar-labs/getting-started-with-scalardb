package com.example.qa.dao;

import com.scalar.database.api.DistributedStorage;
import com.scalar.database.api.DistributedTransactionManager;

public interface ScalarDbFactory {
  DistributedStorage createDistributedStorage();

  DistributedTransactionManager createDistributedTransactionManager(DistributedStorage storage);
}
