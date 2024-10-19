/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.core.util.UuidFactory;
import org.sonar.db.Dao;
import org.sonar.db.DbSession;
import org.sonar.db.audit.AuditPersister;
import org.sonar.db.audit.model.UserTokenNewValue;

import static org.sonar.db.DatabaseUtils.executeLargeInputs;

public class UserTokenDao implements Dao {
  private final UuidFactory uuidFactory;
  private final AuditPersister auditPersister;
  private final Logger logger = Loggers.get(UserTokenDao.class);

  public UserTokenDao(UuidFactory uuidFactory, AuditPersister auditPersister) {
    this.uuidFactory = uuidFactory;
    this.auditPersister = auditPersister;
  }

  public void insert(DbSession dbSession, UserTokenDto userTokenDto, String userLogin) {
    userTokenDto.setUuid(uuidFactory.create());
    mapper(dbSession).insert(userTokenDto);
    auditPersister.addUserToken(dbSession, new UserTokenNewValue(userTokenDto, userLogin));
  }

  public void update(DbSession dbSession, UserTokenDto userTokenDto, @Nullable String userLogin) {
    mapper(dbSession).update(userTokenDto);
    auditPersister.updateUserToken(dbSession, new UserTokenNewValue(userTokenDto, userLogin));
  }

  public List<UserTokenDto> selectTokensExpiredInDays(DbSession dbSession, long days){
    long timestamp = LocalDate.now().plusDays(days).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli();
    return mapper(dbSession).selectTokensExpiredOnDate(timestamp);
  }

  public void updateWithoutAudit(DbSession dbSession, UserTokenDto userTokenDto) {
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
      users.stream().map(UserDto::getUuid).toList(),
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
    int deletedRows = mapper(dbSession).deleteByUserUuid(user.getUuid());

    if (deletedRows > 0) {
      auditPersister.deleteUserToken(dbSession, new UserTokenNewValue(user));
    }
  }

  public void deleteByUserAndName(DbSession dbSession, UserDto user, String name) {
    int deletedRows = mapper(dbSession).deleteByUserUuidAndName(user.getUuid(), name);
    logger.info("Token revoked for the user: {}", user.getLogin());

    if (deletedRows > 0) {
      auditPersister.deleteUserToken(dbSession, new UserTokenNewValue(user, name));
    }
  }

  public void deleteByProjectUuid(DbSession dbSession, String projectKey, String projectUuid) {
    int deletedRows = mapper(dbSession).deleteByProjectUuid(projectUuid);

    if (deletedRows > 0) {
      auditPersister.deleteUserToken(dbSession, new UserTokenNewValue(projectKey));
    }
  }

  private static UserTokenMapper mapper(DbSession dbSession) {
    return dbSession.getMapper(UserTokenMapper.class);
  }
}
