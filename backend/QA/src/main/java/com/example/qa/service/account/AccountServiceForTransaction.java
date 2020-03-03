package com.example.qa.service.account;

import com.example.qa.dao.DaoException;
import com.example.qa.dao.ScalarDbManager;
import com.example.qa.dao.account.AccountDao;
import com.example.qa.dao.account.AccountRecord;
import com.example.qa.service.ServiceException;
import com.scalar.database.api.DistributedTransaction;
import com.scalar.database.api.DistributedTransactionManager;
import com.scalar.database.exception.transaction.CommitException;
import com.scalar.database.exception.transaction.UnknownTransactionStatusException;
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

  @Autowired
  public AccountServiceForTransaction(AccountDao accountDao, ScalarDbManager scalarDbManager) {
    super(accountDao, LoggerFactory.getLogger(AccountServiceForTransaction.class.getSimpleName()));
    this.transactionManager = scalarDbManager.getDistributedTransactionManager();
  }

  @Override
  public Authentication authenticate(Authentication authentication) throws AuthenticationException {
    String email = authentication.getPrincipal().toString();
    String password = authentication.getCredentials().toString();

    AccountRecord existingAccount = null;
    DistributedTransaction transaction = transactionManager.start();
    try {
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
      transaction.abort();
      throw new AuthenticationServiceException(
          "Could not authenticate user due to an internal error" + email, e);
    } catch (UnknownTransactionStatusException e) {
      throw new AuthenticationServiceException(
          "Error : the transaction to authenticate the user is in an unknown state", e);
    }
  }

  public void put(String email, String password) throws ServiceException {
    DistributedTransaction transaction = transactionManager.start();
    accountDao.put(email, getMD5(password), transaction);

    try {
      transaction.commit();
    } catch (CommitException e) {
      transaction.abort();
      throw new ServiceException("Could not insert insert user " + email + " in database", e);
    } catch (UnknownTransactionStatusException e) {
      throw new com.example.qa.service.UnknownTransactionStatusException(
          "Error : the transaction to insert the user is in an unknown state", e);
    }
  }

  public AccountRecord get(String email) throws ServiceException {
    DistributedTransaction transaction = transactionManager.start();

    AccountRecord accountRecord = null;
    try {
      accountRecord = accountDao.get(email, transaction);
      transaction.commit();
    } catch (CommitException | DaoException e) {
      transaction.abort();
      throw new ServiceException("Could not retrieve data of user " + email, e);
    } catch (UnknownTransactionStatusException e) {
      throw new com.example.qa.service.UnknownTransactionStatusException(
          "Error : the transaction to retrieve the user is in an unknown state", e);
    }

    return accountRecord;
  }
}
