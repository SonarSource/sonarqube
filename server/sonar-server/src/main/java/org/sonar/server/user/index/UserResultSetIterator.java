/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.server.user.index;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Maps;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.List;
import javax.annotation.Nullable;
import org.apache.commons.lang.StringUtils;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.ResultSetIterator;
import org.sonar.db.es.EsQueueDto;
import org.sonar.db.user.UserDto;

import static org.apache.commons.lang.StringUtils.repeat;
import static org.sonar.core.util.stream.MoreCollectors.toArrayList;

/**
 * Scrolls over table USERS and reads documents to populate the user index
 */
class UserResultSetIterator extends ResultSetIterator<UserDoc> {

  private static final String[] FIELDS = {
    // column 1
    "u.login",
    "u.name",
    "u.email",
    "u.active",
    "u.scm_accounts",
    "u.created_at",
    "u.updated_at",
  };

  private static final String SQL_ALL = "select " + StringUtils.join(FIELDS, ",") + " from users u ";

  private final ListMultimap<String, String> organizationUuidsByLogins;

  private UserResultSetIterator(PreparedStatement stmt, ListMultimap<String, String> organizationUuidsByLogins) throws SQLException {
    super(stmt);
    this.organizationUuidsByLogins = organizationUuidsByLogins;
  }

  static UserResultSetIterator create(DbClient dbClient, DbSession session, @Nullable Collection<EsQueueDto> esQueueDtos) {
    try {
      String sql = SQL_ALL;
      List<String> logins = null;
      if (esQueueDtos != null) {
        logins = esQueueDtos.stream()
          .filter(i -> i.getDocType() == EsQueueDto.Type.USER)
          .map(EsQueueDto::getDocUuid).collect(toArrayList());
        sql += "where (" + repeat("u.login=?", " or ", logins.size()) + ")";
      }

      PreparedStatement stmt = dbClient.getMyBatis().newScrollingSelectStatement(session, sql);
      setParameters(stmt, logins);

      ListMultimap<String, String> organizationUuidsByLogin = ArrayListMultimap.create();
      if (esQueueDtos == null) {
        dbClient.organizationMemberDao().selectAllForUserIndexing(session, organizationUuidsByLogin::put);
      } else {

        dbClient.organizationMemberDao().selectForUserIndexing(session, logins, organizationUuidsByLogin::put);
      }

      return new UserResultSetIterator(stmt, organizationUuidsByLogin);
    } catch (SQLException e) {
      throw new IllegalStateException("Fail to prepare SQL request to select all users", e);
    }
  }

  private static void setParameters(PreparedStatement stmt, @Nullable Collection<String> logins) throws SQLException {
    if (logins == null) {
      return;
    }

    int paramIndex = 1;
    for (String login : logins) {
      stmt.setString(paramIndex, login);
      paramIndex++;
    }
  }

  @Override
  protected UserDoc read(ResultSet rs) throws SQLException {
    UserDoc doc = new UserDoc(Maps.newHashMapWithExpectedSize(6));

    String login = rs.getString(1);

    // all the keys must be present, even if value is null
    doc.setLogin(login);
    doc.setName(rs.getString(2));
    doc.setEmail(rs.getString(3));
    doc.setActive(rs.getBoolean(4));
    doc.setScmAccounts(UserDto.decodeScmAccounts(rs.getString(5)));
    doc.setOrganizationUuids(organizationUuidsByLogins.get(login));
    return doc;
  }
}
