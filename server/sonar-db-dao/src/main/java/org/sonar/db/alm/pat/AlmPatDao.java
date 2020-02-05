/*
 * SonarQube
 * Copyright (C) 2009-2020 SonarSource SA
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
import org.sonar.api.utils.System2;
import org.sonar.core.util.UuidFactory;
import org.sonar.db.Dao;
import org.sonar.db.DbSession;
import org.sonar.db.alm.setting.AlmSettingDto;
import org.sonar.db.user.UserDto;

public class AlmPatDao implements Dao {

  private final System2 system2;
  private final UuidFactory uuidFactory;

  public AlmPatDao(System2 system2, UuidFactory uuidFactory) {
    this.system2 = system2;
    this.uuidFactory = uuidFactory;
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

  public void insert(DbSession dbSession, AlmPatDto almPatDto) {
    String uuid = uuidFactory.create();
    long now = system2.now();
    getMapper(dbSession).insert(almPatDto, uuid, now);
    almPatDto.setUuid(uuid);
    almPatDto.setCreatedAt(now);
    almPatDto.setUpdatedAt(now);
  }

  public void update(DbSession dbSession, AlmPatDto almPatDto) {
    long now = system2.now();
    getMapper(dbSession).update(almPatDto, now);
    almPatDto.setUpdatedAt(now);
  }

  public void delete(DbSession dbSession, AlmPatDto almPatDto) {
    getMapper(dbSession).deleteByUuid(almPatDto.getUuid());
  }

  public void deleteByUser(DbSession dbSession, UserDto user) {
    getMapper(dbSession).deleteByUser(user.getUuid());
  }

  public void deleteByAlmSetting(DbSession dbSession, AlmSettingDto almSetting) {
    getMapper(dbSession).deleteByAlmSetting(almSetting.getUuid());
  }



}
