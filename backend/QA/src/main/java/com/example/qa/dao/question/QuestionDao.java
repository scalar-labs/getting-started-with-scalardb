package com.example.qa.dao.question;

import com.example.qa.dao.DaoException;
import com.scalar.database.api.*;
import com.scalar.database.exception.storage.ExecutionException;
import com.scalar.database.exception.transaction.CrudException;
import com.scalar.database.io.BigIntValue;
import com.scalar.database.io.IntValue;
import com.scalar.database.io.Key;
import com.scalar.database.io.TextValue;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

@Repository("com.example.qa.dao.question.QuestionDao")
public class QuestionDao {
  private static final String NAMESPACE = "qa";
  private static final String TABLE_NAME = "question";
  private static final String PARTITION_KEY_NAME_DATE = "date";
  private static final String CLUSTERING_KEY_NAME_CREATED_AT = "created_at";
  private static final String COL_NAME_TITLE = "title";
  private static final String COL_NAME_USER = "user";
  private static final String COL_NAME_CONTEXT = "context";
  private static final String COL_NAME_UPDATED_AT = "updated_at";
  private static final String COL_NAME_NUMBER_OF_ANSWERS = "number_of_answers";
  private final Logger log = LoggerFactory.getLogger(this.getClass());

  /** Insert a question with storage */
  public void put(QuestionRecord record, DistributedStorage storage) throws DaoException {
    Put put = createPutWith(record);

    try {
      storage.put(put);
    } catch (ExecutionException e) {
      throw new DaoException("Error inserting the question " + record, e);
    }
    log.info("PUT completed for " + record);
  }

  /** Retrieve the questions for the specified date * */
  public List<QuestionRecord> scan(String date, DistributedStorage storage) throws DaoException {
    List<QuestionRecord> questions = new ArrayList<>();
    Scan scan = createScanWith(date);
    try {
      Scanner scanner = storage.scan(scan);
      for (Result result : scanner) {
        QuestionRecord question = mapResult(result);
        questions.add(question);
      }

    } catch (ExecutionException e) {
      throw new DaoException("Error performing scan with the key " + date, e);
    }
    log.info("SCAN completed for " + date);
    return questions;
  }

  private QuestionRecord mapResult(Result result) {
    String date = ((TextValue) result.getPartitionKey().get().get().get(0)).getString().get();
    long createdAt = ((BigIntValue) result.getClusteringKey().get().get().get(0)).get();
    String title = ((TextValue) result.getValue(COL_NAME_TITLE).get()).getString().get();
    String user = ((TextValue) result.getValue(COL_NAME_USER).get()).getString().get();
    String context = ((TextValue) result.getValue(COL_NAME_CONTEXT).get()).getString().get();
    long updatedAt = ((BigIntValue) result.getValue(COL_NAME_UPDATED_AT).get()).get();
    int numberOfAnswers = ((IntValue) result.getValue(COL_NAME_NUMBER_OF_ANSWERS).get()).get();
    return new QuestionRecord(date, createdAt, title, user, context, updatedAt, numberOfAnswers);
  }

  /** Get the question by specifying its keys */
  public QuestionRecord get(String date, long createdAt, DistributedStorage storage)
      throws DaoException {
    QuestionRecord record = null;
    Get get = createGetWith(date, createdAt);
    try {
      Optional<Result> optResult = storage.get(get);

      if (optResult.isPresent()) {
        Result result = optResult.get();
        record = mapResult(result);
      }
    } catch (ExecutionException e) {
      throw new DaoException(
          "Error retrieving the question with the partition key "
              + date
              + " and clustering key "
              + createdAt,
          e);
    }
    log.info("GET completed." + date + "," + createdAt);
    return record;
  }

  /** Insert a question with transaction */
  public void put(QuestionRecord record, DistributedTransaction transaction) {
    Put put = createPutWith(record);
    put.withConsistency(Consistency.LINEARIZABLE);

    transaction.put(put);
    log.info("PUT completed for " + record);
  }

  /** Get the question by specifying its keys with transaction */
  public QuestionRecord get(String date, long createdAt, DistributedTransaction transaction)
      throws DaoException {
    QuestionRecord record = null;
    Get get = createGetWith(date, createdAt);
    get.withConsistency(Consistency.LINEARIZABLE);
    try {
      Optional<Result> optResult = transaction.get(get);

      if (optResult.isPresent()) {
        Result result = optResult.get();
        record = mapResult(result);
      }
    } catch (CrudException e) {
      throw new DaoException(
          "Error retrieving the question with the partition key "
              + date
              + " and clustering key "
              + createdAt,
          e);
    }
    log.info("GET completed." + date + "," + createdAt);
    return record;
  }

  private Get createGetWith(String date, long createdAt) {
    Get get =
        new Get(
                new Key(new TextValue(PARTITION_KEY_NAME_DATE, date)),
                new Key(new BigIntValue(CLUSTERING_KEY_NAME_CREATED_AT, createdAt)))
            .forNamespace(NAMESPACE)
            .forTable(TABLE_NAME);
    return get;
  }

  private Put createPutWith(QuestionRecord record) {
    Put put =
        new Put(
                new Key(new TextValue(PARTITION_KEY_NAME_DATE, record.getDate())),
                new Key(new BigIntValue(CLUSTERING_KEY_NAME_CREATED_AT, record.getCreatedAt())))
            .forNamespace(NAMESPACE)
            .forTable(TABLE_NAME)
            .withValue(new TextValue(COL_NAME_TITLE, record.getTitle()))
            .withValue(new TextValue(COL_NAME_USER, record.getUser()))
            .withValue(new TextValue(COL_NAME_CONTEXT, record.getContext()))
            .withValue(new BigIntValue(COL_NAME_UPDATED_AT, record.getUpdatedAt()))
            .withValue(new IntValue(COL_NAME_NUMBER_OF_ANSWERS, record.getNumberOfAnswers()));
    return put;
  }

  /** Retrieve the questions for the specified date * */
  public List<QuestionRecord> scan(String date, DistributedTransaction transaction)
      throws DaoException {
    List<QuestionRecord> questions = new ArrayList<>();
    Scan scan = createScanWith(date);
    scan.withConsistency(Consistency.LINEARIZABLE);
    try {
      List<Result> scanner = transaction.scan(scan);
      for (Result result : scanner) {
        QuestionRecord question = mapResult(result);
        questions.add(question);
      }

    } catch (CrudException e) {
      throw new DaoException("Error performing scan with the key " + date, e);
    }
    log.info("SCAN completed for " + date);
    return questions;
  }

  private Scan createScanWith(String date) {
    Scan scan =
        new Scan(new Key(new TextValue(PARTITION_KEY_NAME_DATE, date)))
            .forNamespace(NAMESPACE)
            .forTable(TABLE_NAME)
            .withOrdering(
                new Scan.Ordering(CLUSTERING_KEY_NAME_CREATED_AT, Scan.Ordering.Order.DESC));
    return scan;
  }

  /** Update the timestamp of the last update and the number of answer */
  public void update(
      String date, long createdAt, long updatedAt, int numAnswer, DistributedStorage storage)
      throws DaoException {
    Put put = createPutWithForUpdate(date, createdAt, updatedAt, numAnswer);

    try {
      storage.put(put);
    } catch (ExecutionException e) {
      throw new DaoException(
          "Error updating the question : date "
              + date
              + ", createdAt "
              + createdAt
              + ", updatedAt "
              + updatedAt
              + ", number_of_answer "
              + numAnswer,
          e);
    }
    log.info(
        "PUT completed for : date "
            + date
            + ", createdAt "
            + createdAt
            + ", updatedAt "
            + updatedAt
            + ", number_of_answer "
            + numAnswer);
  }

  /** Update the timestamp of the last update and the number of answer */
  public void update(
      String date,
      long createdAt,
      long updatedAt,
      int numAnswer,
      DistributedTransaction transaction) {
    Put put = createPutWithForUpdate(date, createdAt, updatedAt, numAnswer);
    put.withConsistency(Consistency.LINEARIZABLE);

    transaction.put(put);
    log.info(
        "PUT completed for : date "
            + date
            + ", createdAt "
            + createdAt
            + ", updatedAt "
            + updatedAt
            + ", number_of_answer "
            + numAnswer);
  }

  private Put createPutWithForUpdate(String date, long createdAt, long updatedAt, int numAnswer) {
    Put put =
        new Put(
                new Key(new TextValue(PARTITION_KEY_NAME_DATE, date)),
                new Key(new BigIntValue(CLUSTERING_KEY_NAME_CREATED_AT, createdAt)))
            .forNamespace(NAMESPACE)
            .forTable(TABLE_NAME)
            .withValue(new BigIntValue(COL_NAME_UPDATED_AT, updatedAt))
            .withValue(new IntValue(COL_NAME_NUMBER_OF_ANSWERS, numAnswer));
    return put;
  }
}
