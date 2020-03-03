package com.example.qa.service.answer;

import com.example.qa.controller.answer.HttpPutAnswerRequest;
import com.example.qa.dao.DaoException;
import com.example.qa.dao.ScalarDbManager;
import com.example.qa.dao.answer.AnswerDao;
import com.example.qa.dao.answer.AnswerRecord;
import com.example.qa.dao.question.QuestionDao;
import com.example.qa.dao.question.QuestionRecord;
import com.example.qa.service.ServiceException;
import com.example.qa.util.DateUtils;
import com.scalar.database.api.DistributedTransaction;
import com.scalar.database.api.DistributedTransactionManager;
import com.scalar.database.exception.transaction.CommitException;
import com.scalar.database.exception.transaction.UnknownTransactionStatusException;
import java.util.Date;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Profile("transaction")
@Service("com.example.qa.service.answer.AnswerServiceForTransaction")
@Lazy
public class AnswerServiceForTransaction implements AnswerService {

  private final Logger log = LoggerFactory.getLogger(this.getClass());

  private final DistributedTransactionManager transactionManager;
  private final AnswerDao answerDao;
  private final QuestionDao questionDao;

  @Autowired
  AnswerServiceForTransaction(
      ScalarDbManager scalarDbManager,
      @Qualifier("com.example.qa.dao.answer.AnswerDao") AnswerDao answerDao,
      @Qualifier("com.example.qa.dao.question.QuestionDao") QuestionDao questionDao) {
    this.answerDao = answerDao;
    this.questionDao = questionDao;
    transactionManager = scalarDbManager.getDistributedTransactionManager();
  }

  /**
   * {@inheritDoc}
   *
   * @throws ServiceException
   */
  @Override
  public void put(HttpPutAnswerRequest request) throws ServiceException {
    DistributedTransaction transaction = transactionManager.start();
    QuestionRecord question = null;
    String date =
        DateUtils.millisToDateStr(request.getQuestionCreatedAt(), DateUtils.FORMAT_YYYYMMDD);
    long questionCreatedAt = request.getQuestionCreatedAt();
    try {
      question = questionDao.get(date, questionCreatedAt, transaction);

      if (question == null) {
        throw new ServiceException(
            "The question (date :"
                + date
                + ", questionCreatedAt : "
                + questionCreatedAt
                + ") does not exist");
      }
      AnswerRecord answerRecord =
          new AnswerRecord(
              request.getQuestionCreatedAt(),
              new Date().getTime(),
              request.getContext(),
              request.getUser());
      answerDao.put(answerRecord, transaction);

      questionDao.update(
          question.getDate(),
          question.getCreatedAt(),
          new Date().getTime(),
          question.getNumberOfAnswers() + 1,
          transaction);
      transaction.commit();
    } catch (DaoException e) {
      transaction.abort();
      throw new ServiceException(e.getMessage(), e);
    } catch (CommitException e) {
      transaction.abort();
      throw new ServiceException(
          "Error adding an answer to the question " + question.getCreatedAt(), e);
    } catch (UnknownTransactionStatusException e) {
      throw new com.example.qa.service.UnknownTransactionStatusException(
          "Error : the transaction to add an answer to the question is an unknown state", e);
    }
  }
}
