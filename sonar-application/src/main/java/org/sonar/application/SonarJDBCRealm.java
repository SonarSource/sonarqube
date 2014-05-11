/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.application;

import org.apache.catalina.realm.GenericPrincipal;
import org.apache.catalina.realm.JDBCRealm;
import org.apache.commons.codec.binary.Hex;
import org.slf4j.LoggerFactory;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.Principal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.UUID;


// modified
public class SonarJDBCRealm extends JDBCRealm {

  private final static String SALT_COLUMN = "SALT";
  private static String SALT_VALUE;
  public static final String RANDOM_PASSWORD = UUID.randomUUID().toString();

  public SonarJDBCRealm(Props props) {
    setConnectionURL(props.of("sonar.jdbc.url", "jdbc:h2:tcp://localhost:9092/sonar"));
    setUserTable("USERS");
    setUserRoleTable("USER_ROLES");
    setUserNameCol("LOGIN");
    setUserCredCol("CRYPTED_PASSWORD");
    setRoleNameCol("ROLE");
    setDriverName(props.of("sonar.jdbc.driverName", "org.h2.Driver"));
    setConnectionName(props.of("sonar.jdbc.username", "sonar"));
    setConnectionPassword(props.of("sonar.jdbc.password", "sonar"));
  }

  @Override
  protected synchronized String getPassword(String username) {

    // Look up the user's credentials
    String userPwd = null;
    PreparedStatement stmt;
    ResultSet rs = null;

    // Number of tries is the number of attempts to connect to the database
    // during this login attempt (if we need to open the database)
    // This needs rewritten with better pooling support, the existing code
    // needs signature changes since the Prepared statements needs cached
    // with the connections.
    // The code below will try twice if there is a SQLException so the
    // connection may try to be opened again. On normal conditions (including
    // invalid login - the above is only used once.
    int numberOfTries = 2;
    while (numberOfTries > 0) {
      try {
        // Ensure that we have an open database connection
        open();

        stmt = credentials(dbConnection, username);
        rs = stmt.executeQuery();
        dbConnection.commit();

        if (rs.next()) {
          userPwd = rs.getString(1);
          SALT_VALUE = rs.getString(2);
        }

        if (SALT_VALUE != null) {
          SALT_VALUE = SALT_VALUE.trim();
        }

        if (userPwd != null) {
          userPwd = userPwd.trim();
        }
        return userPwd;

      } catch (SQLException e) {
        // Log the problem for posterity
        LoggerFactory.getLogger(getClass()).warn(sm.getString("jdbcRealm.exception"), e);
      } finally {
        if (rs != null) {
          try {
            rs.close();
          } catch (SQLException e) {
            LoggerFactory.getLogger(getClass()).warn(sm.getString("jdbcRealm.abnormalCloseResultSet"), e);
          }
        }
      }

      // Close the connection so that it gets reopened next time
      if (dbConnection != null) {
        close(dbConnection);
      }

      numberOfTries--;
    }

    return (null);
  }

  private String getHashPasswordString(String userPwd) throws IllegalStateException {
    try {
      // if the keys will be changed in site_keys.rb, you should modify this string accordingly.
      MessageDigest md = MessageDigest.getInstance("SHA-1");
      md.reset();
      md.update(getComposedPwdString(userPwd).getBytes());
      return new String(Hex.encodeHex(md.digest()));
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("no SHA-1 implementation found!", e);
    }
  }

  private String getComposedPwdString(String userPwd) {
    return ("--" + SALT_VALUE + "--" + userPwd + "--");
  }

  @Override
  public synchronized Principal authenticate(Connection dbConnection, String username, String credentials) {
    // No user or no credentials
    // Can't possibly authenticate, don't bother the database then
    if (username == null || credentials == null) {
      return null;
    }

    // Validate the user's credentials
    boolean validated = false;
    if (!credentials.equals(RANDOM_PASSWORD)) {
      // Look up the user's credentials
      String dbCredentials = getPassword(username);
      if (hasMessageDigest()) {
        // Hex hashes should be compared case-insensitive
        validated = (getHashPasswordString(credentials).equalsIgnoreCase(dbCredentials));
      } else {
        validated = (getHashPasswordString(credentials).equals(dbCredentials));
      }
    } else {
      validated = true;
    }

    if (validated) {
      if (LoggerFactory.getLogger(getClass()).isTraceEnabled())
        LoggerFactory.getLogger(getClass()).trace(sm.getString("sonarJdbcRealm.authenticateSuccess",
                username));
    } else {
      if (LoggerFactory.getLogger(getClass()).isTraceEnabled())
        LoggerFactory.getLogger(getClass()).trace(sm.getString("sonarJdbcRealm.authenticateFailure",
                username));
      return (null);
    }

    ArrayList<String> roles = getRoles(username);

    // Create and return a suitable Principal for this user
    return (new GenericPrincipal(username, credentials, roles));
  }

  @Override
  protected PreparedStatement credentials(Connection dbConnection, String username) throws SQLException {
    if (preparedCredentials == null) {
      StringBuilder sb = new StringBuilder("SELECT ");
      sb.append(userCredCol);
      sb.append(",");
      sb.append(SALT_COLUMN);
      sb.append(" FROM ");
      sb.append(userTable);
      sb.append(" WHERE ");
      sb.append(userNameCol);
      sb.append(" = ?");

      if (LoggerFactory.getLogger(getClass()).isDebugEnabled()) {
        LoggerFactory.getLogger(getClass()).debug("credentials query: " + sb.toString());
      }

      preparedCredentials =
              dbConnection.prepareStatement(sb.toString());
    }

    if (username == null) {
      preparedCredentials.setNull(1, java.sql.Types.VARCHAR);
    } else {
      preparedCredentials.setString(1, username);
    }

    return (preparedCredentials);
  }

  @Override
  protected ArrayList<String> getRoles(String username) {
    ArrayList<String> roles = new ArrayList<String>();
    roles.add("FAKE-ROLE");
    return roles;
  }

  public String getRandomPassword() {
    return RANDOM_PASSWORD;
  }
}
