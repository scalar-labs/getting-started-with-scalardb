package com.example.qa.dao;

import com.scalar.db.api.DistributedStorage;
import com.scalar.db.api.DistributedTransactionManager;

public interface ScalarDbFactory {
  DistributedStorage createDistributedStorage();

  DistributedTransactionManager createDistributedTransactionManager(DistributedStorage storage);
}
