package com.example.qa.service.account;

import com.example.qa.dao.DaoException;
import com.example.qa.dao.ScalarDbManager;
import com.example.qa.dao.account.AccountDao;
import com.example.qa.dao.account.AccountRecord;
import com.example.qa.service.ServiceException;
import com.scalar.database.api.DistributedStorage;
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

/**
 * Only one {@link AuthenticationManager} can be instantiated and {@link
 * AccountServiceForTransaction} is already implementing it *
 */
@Profile("storage")
@Service("com.example.qa.service.account.AccountServiceForStorage")
@Lazy
public class AccountServiceForStorage extends AccountService implements AuthenticationManager {
  private final DistributedStorage storage;

  @Autowired
  public AccountServiceForStorage(AccountDao accountDao, ScalarDbManager scalarDbManager) {
    super(accountDao, LoggerFactory.getLogger(AccountServiceForStorage.class.getSimpleName()));
    this.storage = scalarDbManager.getDistributedStorage();
  }

  @Override
  public Authentication authenticate(Authentication authentication) throws AuthenticationException {
    String email = authentication.getPrincipal().toString();
    String password = authentication.getCredentials().toString();

    AccountRecord existingAccount = null;
    try {
      existingAccount = accountDao.get(email, storage);
    } catch (DaoException e) {
      throw new AuthenticationServiceException(
          "Could not authenticate user due to an internal error" + email, e);
    }
    boolean isAuthenticated =
        existingAccount != null
            && email.equals(existingAccount.getEmail())
            && getMD5(password).equals(existingAccount.getPassword());

    if (isAuthenticated) {
      return new UsernamePasswordAuthenticationToken(email, password);
    } else {
      throw new BadCredentialsException("Could not authenticate");
    }
  }

  public void put(String email, String password) throws ServiceException {

    try {
      accountDao.put(email, getMD5(password), storage);
    } catch (DaoException e) {
      throw new ServiceException("Could not insert insert user " + email + " in database", e);
    }
  }

  public AccountRecord get(String email) throws ServiceException {
    try {
      return accountDao.get(email, storage);
    } catch (DaoException e) {
      throw new ServiceException("Could not retrieve data of user " + email, e);
    }
  }
}
