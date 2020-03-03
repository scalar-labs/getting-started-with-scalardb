package com.example.qa.security;

import static java.util.Collections.emptyList;

import com.example.qa.dao.DaoException;
import com.example.qa.dao.ScalarDbManager;
import com.example.qa.dao.account.AccountDao;
import com.example.qa.dao.account.AccountRecord;
import com.scalar.database.api.DistributedTransaction;
import com.scalar.database.api.DistributedTransactionManager;
import com.scalar.database.exception.transaction.CommitException;
import io.jsonwebtoken.Jwts;
import javax.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Profile("transaction")
/** Javascript Web Token (JWT) based authentication */
@Component("com.example.qa.security.JWTAuthenticationServiceForTransaction")
class JWTAuthenticationServiceForTransaction extends JWTAuthenticationService {
  private final DistributedTransactionManager transactionManager;

  @Autowired
  public JWTAuthenticationServiceForTransaction(
      AccountDao accountDao, ScalarDbManager scalarDbManager) {
    super(accountDao, scalarDbManager);
    this.transactionManager = scalarDbManager.getDistributedTransactionManager();
  }

  /**
   * Return an authentication object
   *
   * @param request the HTTP request
   * @return if the authentication is successful, return an Authentication object
   */
  Authentication verifyAuthentication(HttpServletRequest request) throws AuthenticationException {
    String token = request.getHeader(HEADER_STRING);
    if (token != null) {
      String user =
          Jwts.parser()
              .setSigningKey(SECRET)
              .parseClaimsJws(token.replace(TOKEN_PREFIX, ""))
              .getBody()
              .getSubject();
      // Verify the user specified in the JWT still exist
      AccountRecord account = null;
      DistributedTransaction transaction = transactionManager.start();
      try {
        account = accountDao.get(user, transaction);
        transaction.commit();
      } catch (CommitException | DaoException e) {
        transaction.abort();
        throw new AuthenticationException("Error retrieving user to validate JWT authenticity", e);
      } catch (com.scalar.database.exception.transaction.UnknownTransactionStatusException e) {
        throw new UnknownTransactionStatusException(
            "Error : the transaction to retrieve the user associated with the JWT is in an unknown state",
            e);
      }

      if (account != null && account.getEmail().equals(user)) {
        return new UsernamePasswordAuthenticationToken(user, null, emptyList());
      }
    }
    return null;
  }
}
