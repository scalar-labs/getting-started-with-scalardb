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
import com.scalar.database.api.DistributedStorage;
import java.util.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Profile("storage")
@Service("com.example.qa.service.question.QuestionServiceForStorage")
@Lazy
public class QuestionServiceForStorage implements QuestionService {

  private final DistributedStorage storage;
  private final QuestionDao questionDao;
  private final AnswerDao answerDao;
  private final FirstQuestionDateDao firstQuestionDateDao;

  @Autowired
  public QuestionServiceForStorage(
      ScalarDbManager scalarDbManager,
      @Qualifier("com.example.qa.dao.question.QuestionDao") QuestionDao questionDao,
      @Qualifier("com.example.qa.dao.answer.AnswerDao") AnswerDao answerDao,
      @Qualifier("com.example.qa.dao.firstQuestionDate.FirstQuestionDateDao")
          FirstQuestionDateDao firstQuestionDateDao) {
    this.questionDao = questionDao;
    this.answerDao = answerDao;
    this.firstQuestionDateDao = firstQuestionDateDao;
    storage = scalarDbManager.getDistributedStorage();
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
    try {
      questionDao.put(record, storage);
      // If the question is the first inserted in the storage, save its creation day on a separate
      // day
      // that will be used
      // as a lower boundary when looking up questions
      Optional<String> firstQuestionDate = firstQuestionDateDao.get(storage);

      if (!firstQuestionDate.isPresent()) {
        firstQuestionDateDao.put(
            DateUtils.millisToDateStr(millis, DateUtils.FORMAT_YYYYMMDD), storage);
      }
    } catch (DaoException e) {
      throw new ServiceException("Error inserting question with title" + reqQuestion.getTitle(), e);
    }

    return new HttpPutQuestionResponse(record.getCreatedAt());
  }

  /** Retrieve the question by its id* */
  public HttpGetQuestionResponse get(long id) throws ServiceException {
    QuestionRecord question = null;
    List<AnswerRecord> answers = new ArrayList<>();
    try {
      String date = DateUtils.millisToDateStr(id, DateUtils.FORMAT_YYYYMMDD);
      question = questionDao.get(date, id, storage);
      // Get the answers of the question
      if (question != null) {
        answers = answerDao.scan(question.getCreatedAt(), storage);
      } else {
        throw new QuestionNotFoundException(date, id);
      }
    } catch (DaoException e) {
      throw new ServiceException("Error retrieving question " + id, e);
    }
    return new HttpGetQuestionResponse(question, answers);
  }

  /**
   * Retrieve the questions by targeting a starting day. If the number of questions for that day is
   * not enough to to satisfy the minimal numbers of results desired. It will also retrieve the
   * questions of the previous day until the minimal number of results is satisfied.
   *
   * @param start a day
   * @param minimal a minimal number of questions desired
   * @return a list of questions sorted in ascending order by its creation date
   * @throws ServiceException
   */
  public List<HttpGetQuestionResponse> get(String start, int minimal) throws ServiceException {

    List<HttpGetQuestionResponse> questionList = new ArrayList<>();

    Optional<String> firstQuestionDate = null;
    try {
      firstQuestionDate = firstQuestionDateDao.get(storage);
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

        List<QuestionRecord> resultList = null;
        resultList =
            questionDao.scan(DateUtils.millisToDateStr(millis, DateUtils.FORMAT_YYYYMMDD), storage);

        for (QuestionRecord questionRecord : resultList) {
          questionList.add(new HttpGetQuestionResponse(questionRecord, null));
        }
        // Stop searching if the number of questions exceeds the minimal.
        if (questionList.size() >= minimal) {
          break;
        }
      }
    } catch (DaoException e) {
      throw new ServiceException(e.getMessage(), e);
    }
    return questionList;
  }
}
