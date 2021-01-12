package com.example.qa.service.account;

import com.example.qa.dao.DaoException;
import com.example.qa.dao.ScalarDbManager;
import com.example.qa.dao.account.AccountDao;
import com.example.qa.dao.account.AccountRecord;
import com.example.qa.service.ServiceException;
import com.scalar.db.api.DistributedTransaction;
import com.scalar.db.api.DistributedTransactionManager;
import com.scalar.db.exception.transaction.AbortException;
import com.scalar.db.exception.transaction.CommitException;
import com.scalar.db.exception.transaction.TransactionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Profile;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;

@Profile("transaction")
@Lazy
@Service("com.example.qa.service.account.AccountServiceForTransaction")
public class AccountServiceForTransaction extends AccountService implements AuthenticationManager {
  private final DistributedTransactionManager transactionManager;
  private final Logger log = LoggerFactory.getLogger(this.getClass());

  @Autowired
  public AccountServiceForTransaction(AccountDao accountDao, ScalarDbManager scalarDbManager) {
    super(accountDao, LoggerFactory.getLogger(AccountServiceForTransaction.class.getSimpleName()));
    this.transactionManager = scalarDbManager.getDistributedTransactionManager();
  }

  @Override
  public Authentication authenticate(Authentication authentication)
          throws AuthenticationException {
    String email = authentication.getPrincipal().toString();
    String password = authentication.getCredentials().toString();

    AccountRecord existingAccount = null;
    DistributedTransaction transaction = null;
    try {
      transaction = transactionManager.start();
      existingAccount = accountDao.get(email, transaction);

      boolean isAuthenticated =
          existingAccount != null
              && email.equals(existingAccount.getEmail())
              && getMD5(password).equals(existingAccount.getPassword());

      transaction.commit();
      if (isAuthenticated) {
        return new UsernamePasswordAuthenticationToken(email, password);
      } else {
        throw new BadCredentialsException("Could not authenticate");
      }
    } catch (CommitException | DaoException e) {
      try {
        transaction.abort();
      } catch (AbortException ae){
        log.error(ae.getMessage(), ae);
      }
      throw new AuthenticationServiceException(
          "Could not authenticate user due to an internal error" + email, e);
    } catch (TransactionException e) {
      throw new AuthenticationServiceException(
          "Error : the transaction to authenticate the user is in an unknown state", e);
    }
  }

  public void put(String email, String password) throws ServiceException {
    DistributedTransaction transaction = null;

    try {
      transaction = transactionManager.start();
      accountDao.put(email, getMD5(password), transaction);
      transaction.commit();
    } catch (DaoException | CommitException e) {
      try {
        transaction.abort();
      } catch (AbortException ae){
        log.error(ae.getMessage(), ae);
      }
      throw new ServiceException("Could not insert insert user " + email + " in database", e);
    } catch (TransactionException e) {
      throw new com.example.qa.service.UnknownTransactionStatusException(
          "Error : the transaction to insert the user is in an unknown state", e);
    }
  }

  public AccountRecord get(String email) throws ServiceException {
    DistributedTransaction transaction = null;

    AccountRecord accountRecord = null;
    try {
      transaction = transactionManager.start();
      accountRecord = accountDao.get(email, transaction);
      transaction.commit();
    } catch (CommitException | DaoException e) {
      try {
        transaction.abort();
      } catch (AbortException ae){
        log.error(ae.getMessage(), ae);
      }
      throw new ServiceException("Could not retrieve data of user " + email, e);
    } catch (TransactionException e) {
      throw new com.example.qa.service.UnknownTransactionStatusException(
          "Error : the transaction to retrieve the user is in an unknown state", e);
    }

    return accountRecord;
  }
}
