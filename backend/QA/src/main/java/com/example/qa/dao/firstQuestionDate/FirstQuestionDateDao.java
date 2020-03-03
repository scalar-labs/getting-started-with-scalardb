package com.example.qa.dao.firstQuestionDate;

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

@Repository("com.example.qa.dao.firstQuestionDate.FirstQuestionDateDao")
public class FirstQuestionDateDao {
  private static final String PARTITION_KEY_VALUE_ID = "1";
  private static final String NAMESPACE = "qa";
  private static final String TABLE_NAME = "firstQuestionDate";

  private static final String PARTITION_KEY_NAME_ID = "id";
  private static final String COL_NAME_FIRST_QUESTION_DATE = "first_question_date";
  private final Logger log = LoggerFactory.getLogger(this.getClass());

  /** Insert the date of the oldest question stored in the storage */
  public void put(String date, DistributedStorage storage) throws DaoException {
    Put put = createPutWith(date);

    try {
      storage.put(put);
    } catch (ExecutionException e) {
      throw new DaoException("Error inserting the date of the first question " + date, e);
    }
    log.info("PUT completed for " + date);
  }

  /** Retrieve date of the oldest question stored in the storage* */
  public Optional<String> get(DistributedStorage storage) throws DaoException {
    Optional<String> firstQuestionDate = Optional.empty();
    Get get = createGetWith();
    try {
      Optional<Result> optResult = storage.get(get);

      if (optResult.isPresent()) {
        Result result = optResult.get();
        firstQuestionDate =
            ((TextValue) result.getValue(COL_NAME_FIRST_QUESTION_DATE).get()).getString();
        return firstQuestionDate;
      }

    } catch (ExecutionException e) {
      throw new DaoException("Error retrieving the date of the first question", e);
    }
    log.info("GET completed");
    return firstQuestionDate;
  }

  /** Retrieve date of the oldest question stored in the storage* */
  public Optional<String> get(DistributedTransaction transaction) throws DaoException {
    Optional<String> firstQuestionDate = Optional.empty();
    Get get = createGetWith();
    get.withConsistency(Consistency.LINEARIZABLE);
    try {
      Optional<Result> optResult = transaction.get(get);

      if (optResult.isPresent()) {
        Result result = optResult.get();
        firstQuestionDate =
            ((TextValue) result.getValue(COL_NAME_FIRST_QUESTION_DATE).get()).getString();
        return firstQuestionDate;
      }

    } catch (CrudException e) {
      throw new DaoException("Error retrieving the date of the first question", e);
    }
    log.info("GET completed");
    return firstQuestionDate;
  }

  /** Insert the date of the oldest question stored in the storage with transaction */
  public void put(String date, DistributedTransaction transaction) {
    Put put = createPutWith(date);
    put.withConsistency(Consistency.LINEARIZABLE);

    transaction.put(put);
    log.info("PUT completed for " + date);
  }

  private Get createGetWith() {
    Get get =
        new Get(new Key(new TextValue(PARTITION_KEY_NAME_ID, PARTITION_KEY_VALUE_ID)))
            .forNamespace(NAMESPACE)
            .forTable(TABLE_NAME);
    return get;
  }

  private Put createPutWith(String date) {
    Put put =
        new Put(new Key(new TextValue(PARTITION_KEY_NAME_ID, PARTITION_KEY_VALUE_ID)))
            .forNamespace(NAMESPACE)
            .forTable(TABLE_NAME)
            .withValue(new TextValue(COL_NAME_FIRST_QUESTION_DATE, date));
    return put;
  }
}
