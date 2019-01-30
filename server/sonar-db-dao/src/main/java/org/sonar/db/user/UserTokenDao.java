/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
package org.sonar.db.user;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.CheckForNull;
import org.sonar.db.Dao;
import org.sonar.db.DbSession;

import static org.sonar.core.util.stream.MoreCollectors.toList;
import static org.sonar.db.DatabaseUtils.executeLargeInputs;

public class UserTokenDao implements Dao {

  public void insert(DbSession dbSession, UserTokenDto userTokenDto) {
    mapper(dbSession).insert(userTokenDto);
  }

  public void update(DbSession dbSession, UserTokenDto userTokenDto) {
    mapper(dbSession).update(userTokenDto);
  }

  @CheckForNull
  public UserTokenDto selectByTokenHash(DbSession dbSession, String tokenHash) {
    return mapper(dbSession).selectByTokenHash(tokenHash);
  }

  @CheckForNull
  public UserTokenDto selectByUserAndName(DbSession dbSession, UserDto user, String name) {
    return mapper(dbSession).selectByUserUuidAndName(user.getUuid(), name);
  }

  public List<UserTokenDto> selectByUser(DbSession dbSession, UserDto user) {
    return mapper(dbSession).selectByUserUuid(user.getUuid());
  }

  public Map<String, Integer> countTokensByUsers(DbSession dbSession, Collection<UserDto> users) {
    Map<String, Integer> result = new HashMap<>(users.size());
    executeLargeInputs(
      users.stream().map(UserDto::getUuid).collect(toList()),
      input -> {
        List<UserTokenCount> userTokenCounts = mapper(dbSession).countTokensByUserUuids(input);
        for (UserTokenCount userTokenCount : userTokenCounts) {
          result.put(userTokenCount.getUserUuid(), userTokenCount.tokenCount());
        }
        return userTokenCounts;
      });

    return result;
  }

  public void deleteByUser(DbSession dbSession, UserDto user) {
    mapper(dbSession).deleteByUserUuid(user.getUuid());
  }

  public void deleteByUserAndName(DbSession dbSession, UserDto user, String name) {
    mapper(dbSession).deleteByUserUuidAndName(user.getUuid(), name);
  }

  private static UserTokenMapper mapper(DbSession dbSession) {
    return dbSession.getMapper(UserTokenMapper.class);
  }
}
