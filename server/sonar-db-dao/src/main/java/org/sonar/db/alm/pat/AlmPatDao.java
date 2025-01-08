/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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
package org.sonar.db.alm.pat;

import java.util.Optional;
import javax.annotation.Nullable;
import org.sonar.api.utils.System2;
import org.sonar.core.util.UuidFactory;
import org.sonar.db.Dao;
import org.sonar.db.DbSession;
import org.sonar.db.alm.setting.AlmSettingDto;
import org.sonar.db.audit.AuditPersister;
import org.sonar.db.audit.model.PersonalAccessTokenNewValue;
import org.sonar.db.user.UserDto;

public class AlmPatDao implements Dao {

  private final System2 system2;
  private final UuidFactory uuidFactory;
  private final AuditPersister auditPersister;

  public AlmPatDao(System2 system2, UuidFactory uuidFactory, AuditPersister auditPersister) {
    this.system2 = system2;
    this.uuidFactory = uuidFactory;
    this.auditPersister = auditPersister;
  }

  private static AlmPatMapper getMapper(DbSession dbSession) {
    return dbSession.getMapper(AlmPatMapper.class);
  }

  public Optional<AlmPatDto> selectByUuid(DbSession dbSession, String uuid) {
    return Optional.ofNullable(getMapper(dbSession).selectByUuid(uuid));
  }

  public Optional<AlmPatDto> selectByUserAndAlmSetting(DbSession dbSession, String userUuid, AlmSettingDto almSettingDto) {
    return Optional.ofNullable(getMapper(dbSession).selectByUserAndAlmSetting(userUuid, almSettingDto.getUuid()));
  }

  public void insert(DbSession dbSession, AlmPatDto almPatDto, @Nullable String userLogin, @Nullable String almSettingKey) {
    String uuid = uuidFactory.create();
    long now = system2.now();
    almPatDto.setUuid(uuid);
    almPatDto.setCreatedAt(now);
    almPatDto.setUpdatedAt(now);
    getMapper(dbSession).insert(almPatDto);

    auditPersister.addPersonalAccessToken(dbSession, new PersonalAccessTokenNewValue(almPatDto, userLogin, almSettingKey));
  }

  public void update(DbSession dbSession, AlmPatDto almPatDto, @Nullable String userLogin, @Nullable String almSettingKey) {
    long now = system2.now();
    almPatDto.setUpdatedAt(now);
    getMapper(dbSession).update(almPatDto);
    auditPersister.updatePersonalAccessToken(dbSession, new PersonalAccessTokenNewValue(almPatDto, userLogin, almSettingKey));
  }

  public void delete(DbSession dbSession, AlmPatDto almPatDto, @Nullable String userLogin, @Nullable String almSettingKey) {
    int deletedRows = getMapper(dbSession).deleteByUuid(almPatDto.getUuid());
    if (deletedRows > 0) {
      auditPersister.deletePersonalAccessToken(dbSession, new PersonalAccessTokenNewValue(almPatDto, userLogin, almSettingKey));
    }
  }

  public void deleteByUser(DbSession dbSession, UserDto user) {
    int deletedRows = getMapper(dbSession).deleteByUser(user.getUuid());
    if (deletedRows > 0) {
      auditPersister.deletePersonalAccessToken(dbSession, new PersonalAccessTokenNewValue(user));
    }
  }

  public void deleteByAlmSetting(DbSession dbSession, AlmSettingDto almSetting) {
    int deletedRows = getMapper(dbSession).deleteByAlmSetting(almSetting.getUuid());
    if (deletedRows > 0) {
      auditPersister.deletePersonalAccessToken(dbSession, new PersonalAccessTokenNewValue(almSetting));
    }
  }
}
