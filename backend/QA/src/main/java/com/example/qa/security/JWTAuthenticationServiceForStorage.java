package com.example.qa.security;

import static java.util.Collections.emptyList;

import com.example.qa.dao.DaoException;
import com.example.qa.dao.ScalarDbManager;
import com.example.qa.dao.account.AccountDao;
import com.example.qa.dao.account.AccountRecord;
import com.scalar.database.api.DistributedStorage;
import io.jsonwebtoken.Jwts;
import javax.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

/** Javascript Web Token (JWT) based authentication */
@Profile("storage")
@Component("com.example.qa.security.JWTAuthenticationServiceForStorage")
class JWTAuthenticationServiceForStorage extends JWTAuthenticationService {
  private final DistributedStorage storage;

  @Autowired
  public JWTAuthenticationServiceForStorage(
      AccountDao accountDao, ScalarDbManager scalarDbManager) {
    super(accountDao, scalarDbManager);
    this.storage = scalarDbManager.getDistributedStorage();
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
      try {
        account = accountDao.get(user, storage);
      } catch (DaoException e) {
        throw new AuthenticationException("Error retrieving user to validate JWT authenticity", e);
      }
      if (account != null && account.getEmail().equals(user)) {
        return new UsernamePasswordAuthenticationToken(user, null, emptyList());
      }
    }
    return null;
  }
}
