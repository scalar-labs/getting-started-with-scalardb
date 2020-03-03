package com.example.qa.dao.account;

import com.example.qa.dao.DaoException;
import com.scalar.database.api.*;
import com.scalar.database.exception.storage.ExecutionException;
import com.scalar.database.exception.transaction.CrudException;
import com.scalar.database.io.Key;
import com.scalar.database.io.TextValue;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

@Repository("com.example.qa.dao.account.AccountDao")
public class AccountDao {
  private static final String TABLE_NAME = "account";
  private static final String NAMESPACE = "qa";
  private static final String PRIMARY_KEY_EMAIL = "email";
  private static final String COL_NAME_PASSWORD = "password";
  private final Logger log = LoggerFactory.getLogger(this.getClass());

  /** Insert/update an account in the storage */
  public void put(String email, String password, DistributedStorage storage) throws DaoException {
    Put put = createPutWith(email, password);
    try {
      storage.put(put);
    } catch (ExecutionException e) {
      String errorMsg = "error PUT : email" + email;
      throw new DaoException(errorMsg, e);
    }
    log.info("PUT completed for email " + email);
  }

  /** Retrieve an account* */
  public AccountRecord get(String email, DistributedStorage storage) throws DaoException {
    AccountRecord record = null;
    Get get = createGetWith(email);
    try {
      Optional<Result> optResult = storage.get(get);

      if (optResult.isPresent()) {
        Result result = optResult.get();

        String password = ((TextValue) result.getValue(COL_NAME_PASSWORD).get()).getString().get();
        record = new AccountRecord(email, password);
      }
    } catch (ExecutionException e) {
      String errorMsg = "error GET " + email;
      throw new DaoException(errorMsg, e);
    }
    log.info("GET completed for" + record);
    return record;
  }

  /** Insert/update an account in the storage */
  public void put(String email, String password, DistributedTransaction transaction) {
    Put put = createPutWith(email, password);
    put.withConsistency(Consistency.LINEARIZABLE);
    transaction.put(put);
    log.info("PUT completed for email " + email);
  }

  /** Retrieve an account* */
  public AccountRecord get(String email, DistributedTransaction transaction) throws DaoException {
    AccountRecord record = null;
    Get get = createGetWith(email);
    get.withConsistency(Consistency.LINEARIZABLE);
    try {
      Optional<Result> optResult = transaction.get(get);

      if (optResult.isPresent()) {
        Result result = optResult.get();

        String password = ((TextValue) result.getValue(COL_NAME_PASSWORD).get()).getString().get();
        record = new AccountRecord(email, password);
      }
    } catch (CrudException e) {
      String errorMsg = "error GET " + email;
      throw new DaoException(errorMsg, e);
    }
    log.info("GET completed for" + record);
    return record;
  }

  private Put createPutWith(String email, String password) {
    Put put =
        new Put(new Key(new TextValue(PRIMARY_KEY_EMAIL, email)))
            .forNamespace(NAMESPACE)
            .forTable(TABLE_NAME)
            .withValue(new TextValue(COL_NAME_PASSWORD, password));
    return put;
  }

  private Get createGetWith(String email) {
    Get get =
        new Get(new Key(new TextValue(PRIMARY_KEY_EMAIL, email)))
            .forNamespace(NAMESPACE)
            .forTable(TABLE_NAME);
    return get;
  }
}
