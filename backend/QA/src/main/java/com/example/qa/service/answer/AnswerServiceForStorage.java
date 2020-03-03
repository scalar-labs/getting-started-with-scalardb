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
import com.scalar.database.api.DistributedStorage;
import java.util.Date;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Profile("storage")
@Service("com.example.qa.service.answer.AnswerServiceForStorage")
@Lazy
public class AnswerServiceForStorage implements AnswerService {

  private final Logger log = LoggerFactory.getLogger(this.getClass());

  private final DistributedStorage storage;
  private final AnswerDao answerDao;
  private final QuestionDao questionDao;

  @Autowired
  AnswerServiceForStorage(
      ScalarDbManager scalarDbManager,
      @Qualifier("com.example.qa.dao.answer.AnswerDao") AnswerDao answerDao,
      @Qualifier("com.example.qa.dao.question.QuestionDao") QuestionDao questionDao) {
    this.answerDao = answerDao;
    this.questionDao = questionDao;
    storage = scalarDbManager.getDistributedStorage();
  }

  /** {@inheritDoc} */
  @Override
  public void put(HttpPutAnswerRequest request) throws ServiceException {
    QuestionRecord question = null;
    String partitionKey =
        DateUtils.millisToDateStr(request.getQuestionCreatedAt(), DateUtils.FORMAT_YYYYMMDD);
    long clusteringKey = request.getQuestionCreatedAt();
    try {
      question = questionDao.get(partitionKey, request.getQuestionCreatedAt(), storage);

      if (question == null) {
        return;
      }
      AnswerRecord answerRecord =
          new AnswerRecord(
              request.getQuestionCreatedAt(),
              new Date().getTime(),
              request.getContext(),
              request.getUser());

      answerDao.put(answerRecord, storage);

      questionDao.update(
          question.getDate(),
          question.getCreatedAt(),
          new Date().getTime(),
          question.getNumberOfAnswers() + 1,
          storage);

    } catch (DaoException e) {
      throw new ServiceException(e.getMessage(), e);
    }
  }
}
