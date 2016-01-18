/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package org.sonar.db.user;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nonnull;
import org.sonar.db.Dao;
import org.sonar.db.DatabaseUtils;
import org.sonar.db.DbSession;
import org.sonar.db.RowNotFoundException;

import static java.lang.String.format;

public class UserTokenDao implements Dao {
  public void insert(DbSession dbSession, UserTokenDto userTokenDto) {
    mapper(dbSession).insert(userTokenDto);
  }

  public UserTokenDto selectOrFailByTokenHash(DbSession dbSession, String tokenHash) {
    UserTokenDto userToken = mapper(dbSession).selectByTokenHash(tokenHash);
    if (userToken == null) {
      throw new RowNotFoundException(format("User token with token hash '%s' not found", tokenHash));
    }

    return userToken;
  }

  public Optional<UserTokenDto> selectByTokenHash(DbSession dbSession, String tokenHash) {
    return Optional.fromNullable(mapper(dbSession).selectByTokenHash(tokenHash));
  }

  public Optional<UserTokenDto> selectByLoginAndName(DbSession dbSession, String login, String name) {
    return Optional.fromNullable(mapper(dbSession).selectByLoginAndName(login, name));
  }

  public List<UserTokenDto> selectByLogin(DbSession dbSession, String login) {
    return mapper(dbSession).selectByLogin(login);
  }

  public Map<String, Integer> countTokensByLogins(final DbSession dbSession, List<String> logins) {
    final Map<String, Integer> result = new HashMap<>();
    DatabaseUtils.executeLargeInputs(logins, new Function<List<String>, List<UserTokenCount>>() {
      @Override
      public List<UserTokenCount> apply(@Nonnull List<String> input) {
        List<UserTokenCount> userTokenCounts = mapper(dbSession).countTokensByLogins(input);
        for (UserTokenCount userTokenCount : userTokenCounts) {
          result.put(userTokenCount.getLogin(), userTokenCount.tokenCount());
        }
        return userTokenCounts;
      }
    });

    return result;
  }

  public void deleteByLogin(DbSession dbSession, String login) {
    mapper(dbSession).deleteByLogin(login);
  }

  public void deleteByLoginAndName(DbSession dbSession, String login, String name) {
    mapper(dbSession).deleteByLoginAndName(login, name);
  }

  private static UserTokenMapper mapper(DbSession dbSession) {
    return dbSession.getMapper(UserTokenMapper.class);
  }
}
