package com.example.qa.dao.answer;

import com.example.qa.dao.DaoException;
import com.scalar.database.api.*;
import com.scalar.database.exception.storage.ExecutionException;
import com.scalar.database.exception.transaction.CrudException;
import com.scalar.database.io.BigIntValue;
import com.scalar.database.io.Key;
import com.scalar.database.io.TextValue;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

@Repository("com.example.qa.dao.answer.AnswerDao")
public class AnswerDao {

  private static final String NAMESPACE = "qa";
  private static final String TABLE_NAME = "answer";
  private static final String PARTITION_KEY_NAME_QUESTION_CREATED_AT = "question_created_at";
  private static final String CLUSTERING_KEY_NAME_CREATED_AT = "created_at";
  private static final String COL_NAME_CONTEXT = "context";
  private static final String COL_NAME_USER = "user";
  private final Logger log = LoggerFactory.getLogger(this.getClass());

  // Put/update an answer
  public void put(AnswerRecord record, DistributedStorage storage) throws DaoException {
    Put put = createPutWith(record);

    try {
      storage.put(put);
    } catch (ExecutionException e) {
      throw new DaoException("error PUT " + record, e);
    }
    log.info("PUT completed for " + record);
  }

  /** Retrieve the answer for the specified question */
  public List<AnswerRecord> scan(long questionCreatedAt, DistributedStorage storage)
      throws DaoException {
    List<AnswerRecord> answers = new ArrayList<>();
    Scan scan = createScanWith(questionCreatedAt);
    try {
      Scanner scanner = storage.scan(scan);
      for (Result result : scanner) {
        answers.add(mapResult(result, questionCreatedAt));
      }
    } catch (ExecutionException e) {
      throw new DaoException("error SCAN " + questionCreatedAt, e);
    }
    log.info("SCAN completed for " + questionCreatedAt);
    return answers;
  }

  /** Put/update an answer with transaction */
  public void put(AnswerRecord record, DistributedTransaction transaction) {
    Put put = createPutWith(record);
    put.withConsistency(Consistency.LINEARIZABLE);

    transaction.put(put);
    log.info("PUT completed for " + record);
  }

  private Put createPutWith(AnswerRecord record) {
    Put put =
        new Put(
                new Key(
                    new BigIntValue(
                        PARTITION_KEY_NAME_QUESTION_CREATED_AT, record.getQuestionCreatedAt())),
                new Key(new BigIntValue(CLUSTERING_KEY_NAME_CREATED_AT, record.getCreatedAt())))
            .forNamespace(NAMESPACE)
            .forTable(TABLE_NAME)
            .withValue(new TextValue(COL_NAME_CONTEXT, record.getContext()))
            .withValue(new TextValue(COL_NAME_USER, record.getUser()));
    return put;
  }

  /** Retrieve the answer for the specified question */
  public List<AnswerRecord> scan(long questionCreatedAt, DistributedTransaction transaction)
      throws DaoException {
    List<AnswerRecord> answers = new ArrayList<>();
    Scan scan = createScanWith(questionCreatedAt);
    scan.withConsistency(Consistency.LINEARIZABLE);
    try {
      List<Result> scanner = transaction.scan(scan);
      for (Result result : scanner) {
        answers.add(mapResult(result, questionCreatedAt));
      }
    } catch (CrudException e) {
      throw new DaoException("error SCAN " + questionCreatedAt, e);
    }
    log.info("SCAN completed for " + questionCreatedAt);
    return answers;
  }

  private Scan createScanWith(long questionCreatedAt) {
    Scan scan =
        new Scan(
                new Key(new BigIntValue(PARTITION_KEY_NAME_QUESTION_CREATED_AT, questionCreatedAt)))
            .forNamespace(NAMESPACE)
            .forTable(TABLE_NAME)
            .withOrdering(
                new Scan.Ordering(CLUSTERING_KEY_NAME_CREATED_AT, Scan.Ordering.Order.ASC));
    return scan;
  }

  private AnswerRecord mapResult(Result result, long questionCreatedAt) {
    long createdAt = ((BigIntValue) result.getClusteringKey().get().get().get(0)).get();
    String context = ((TextValue) result.getValue(COL_NAME_CONTEXT).get()).getString().get();
    String user = ((TextValue) result.getValue(COL_NAME_USER).get()).getString().get();
    return new AnswerRecord(questionCreatedAt, createdAt, context, user);
  }
}
