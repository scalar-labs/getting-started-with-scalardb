package com.example.qa.service.question;

import com.example.qa.controller.question.HttpGetQuestionResponse;
import com.example.qa.controller.question.HttpPutQuestionRequest;
import com.example.qa.controller.question.HttpPutQuestionResponse;
import com.example.qa.dao.DaoException;
import com.example.qa.dao.ScalarDbManager;
import com.example.qa.dao.answer.AnswerDao;
import com.example.qa.dao.answer.AnswerRecord;
import com.example.qa.dao.firstQuestionDate.FirstQuestionDateDao;
import com.example.qa.dao.question.QuestionDao;
import com.example.qa.dao.question.QuestionRecord;
import com.example.qa.service.ServiceException;
import com.example.qa.util.DateUtils;
import com.scalar.database.api.DistributedTransaction;
import com.scalar.database.api.DistributedTransactionManager;
import com.scalar.database.exception.transaction.CommitException;
import com.scalar.database.exception.transaction.UnknownTransactionStatusException;
import java.util.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Profile("transaction")
@Service("com.example.qa.service.question.QuestionServiceForTransaction")
@Lazy
public class QuestionServiceForTransaction implements QuestionService {

  private final DistributedTransactionManager transactionManager;
  private final QuestionDao questionDao;
  private final AnswerDao answerDao;
  private final FirstQuestionDateDao firstQuestionDateDao;

  @Autowired
  public QuestionServiceForTransaction(
      ScalarDbManager scalarDbManager,
      @Qualifier("com.example.qa.dao.question.QuestionDao") QuestionDao questionDao,
      @Qualifier("com.example.qa.dao.answer.AnswerDao") AnswerDao answerDao,
      @Qualifier("com.example.qa.dao.firstQuestionDate.FirstQuestionDateDao")
          FirstQuestionDateDao firstQuestionDateDao) {
    this.questionDao = questionDao;
    this.answerDao = answerDao;
    this.firstQuestionDateDao = firstQuestionDateDao;

    transactionManager = scalarDbManager.getDistributedTransactionManager();
  }

  @Override
  public HttpPutQuestionResponse put(HttpPutQuestionRequest reqQuestion) throws ServiceException {
    long millis = new Date().getTime();

    QuestionRecord record =
        new QuestionRecord(
            DateUtils.millisToDateStr(millis, DateUtils.FORMAT_YYYYMMDD),
            millis,
            reqQuestion.getTitle(),
            reqQuestion.getUser(),
            reqQuestion.getContext(),
            0L,
            0);

    DistributedTransaction transaction = transactionManager.start();
    questionDao.put(record, transaction);

    // If the question is the first inserted in the storage, save its creation day on a separate day
    // that will be used
    // as a lower boundary when looking up questions
    Optional<String> firstQuestionDate = null;
    try {
      firstQuestionDate = firstQuestionDateDao.get(transaction);

      if (!firstQuestionDate.isPresent()) {
        firstQuestionDateDao.put(
            DateUtils.millisToDateStr(millis, DateUtils.FORMAT_YYYYMMDD), transaction);
      }

      transaction.commit();

    } catch (DaoException e) {
      transaction.abort();
      throw new ServiceException(e.getMessage(), e);
    } catch (CommitException e) {
      transaction.abort();
      throw new ServiceException("Error creating first question date or inserting a question", e);
    } catch (UnknownTransactionStatusException e) {
      throw new com.example.qa.service.UnknownTransactionStatusException(
          "Error : the transaction to insert a question is in an unknown state", e);
    }

    return new HttpPutQuestionResponse(record.getCreatedAt());
  }

  /** Retrieve the question by its id* */
  public HttpGetQuestionResponse get(long id) throws ServiceException {

    DistributedTransaction transaction = transactionManager.start();
    QuestionRecord question = null;
    List<AnswerRecord> answers = null;
    try {
      String date = DateUtils.millisToDateStr(id, DateUtils.FORMAT_YYYYMMDD);
      question = questionDao.get(date, id, transaction);
      // Get the answers of the question
      answers = new ArrayList<>();
      if (question != null) {
        answers = answerDao.scan(question.getCreatedAt(), transaction);
      } else {
        transaction.abort();
        throw new QuestionNotFoundException(date, id);
      }
      transaction.commit();

    } catch (DaoException e) {
      transaction.abort();
      throw new ServiceException(e.getMessage(), e);
    } catch (CommitException e) {
      transaction.abort();
      throw new ServiceException(
          "Error retrieving question or retrieving the answer associated with the question " + id,
          e);
    } catch (UnknownTransactionStatusException e) {
      throw new com.example.qa.service.UnknownTransactionStatusException(
          "Error : the transaction to retrieve the question( createdAt : "
              + id
              + " is in an unknown state",
          e);
    }
    return new HttpGetQuestionResponse(question, answers);
  }

  /**
   * Retrieve the questions by targeting a starting day. If the number of questions for that day is
   * not enough to satisfy the minimal numbers of results desired. It will also retrieve the
   * questions of the previous day until the minimal number of results is satisfied.
   *
   * @param start a day
   * @param minimal a minimal number of questions desired
   * @return a list of questions sorted in ascending order by its creation date
   * @throws ServiceException
   */
  public List<HttpGetQuestionResponse> get(String start, int minimal) throws ServiceException {
    List<HttpGetQuestionResponse> questionList = new ArrayList<>();
    DistributedTransaction transaction = transactionManager.start();

    try {
      Optional<String> firstQuestionDate = firstQuestionDateDao.get(transaction);
      if (!firstQuestionDate.isPresent()) {
        return questionList;
      }
      // start Date
      Calendar fromDate = DateUtils.strToCalender(start, DateUtils.FORMAT_YYYYMMDD);
      // end Date
      Calendar toDate = DateUtils.strToCalender(firstQuestionDate.get(), DateUtils.FORMAT_YYYYMMDD);

      // Retrieve the list of questions one day at a time.
      for (Calendar cal = fromDate;
          cal.getTimeInMillis() >= toDate.getTimeInMillis();
          cal.add(Calendar.DATE, -1)) {
        long millis = cal.getTimeInMillis();

        List<QuestionRecord> resultList =
            questionDao.scan(
                DateUtils.millisToDateStr(millis, DateUtils.FORMAT_YYYYMMDD), transaction);

        for (QuestionRecord questionRecord : resultList) {
          questionList.add(new HttpGetQuestionResponse(questionRecord, null));
        }
        // Stop searching if the number of questions exceeds the minimal.
        if (questionList.size() >= minimal) {
          break;
        }
      }
      transaction.commit();
    } catch (CommitException | DaoException e) {
      transaction.abort();
      throw new ServiceException(
          "Error retrieving first question date so as to perform a scan or retrieving multiple questions from day: "
              + start
              + " with minimal = "
              + minimal,
          e);
    } catch (UnknownTransactionStatusException e) {
      throw new com.example.qa.service.UnknownTransactionStatusException(
          "Error : the transaction to retrieve questions is in an unknown state", e);
    }
    return questionList;
  }
}
