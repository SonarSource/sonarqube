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
package org.sonar.db.alm.setting;

import java.util.List;
import java.util.Optional;
import org.sonar.api.utils.System2;
import org.sonar.core.util.UuidFactory;
import org.sonar.db.Dao;
import org.sonar.db.DbSession;

public class AlmSettingDao implements Dao {

  private final System2 system2;
  private final UuidFactory uuidFactory;

  public AlmSettingDao(System2 system2, UuidFactory uuidFactory) {
    this.system2 = system2;
    this.uuidFactory = uuidFactory;
  }

  private static AlmSettingMapper getMapper(DbSession dbSession) {
    return dbSession.getMapper(AlmSettingMapper.class);
  }

  public void insert(DbSession dbSession, AlmSettingDto almSettingDto) {
    String uuid = uuidFactory.create();
    long now = system2.now();
    getMapper(dbSession).insert(almSettingDto, uuid, now);
    almSettingDto.setUuid(uuid);
    almSettingDto.setCreatedAt(now);
    almSettingDto.setUpdatedAt(now);
  }

  public Optional<AlmSettingDto> selectByUuid(DbSession dbSession, String uuid) {
    return Optional.ofNullable(getMapper(dbSession).selectByUuid(uuid));
  }

  public Optional<AlmSettingDto> selectByKey(DbSession dbSession, String key) {
    return Optional.ofNullable(getMapper(dbSession).selectByKey(key));
  }

  public List<AlmSettingDto> selectByAlm(DbSession dbSession, ALM alm) {
    return getMapper(dbSession).selectByAlm(alm.getId());
  }


  public List<AlmSettingDto> selectAll(DbSession dbSession) {
    return getMapper(dbSession).selectAll();
  }

  public void delete(DbSession dbSession, AlmSettingDto almSettingDto){
    getMapper(dbSession).deleteByKey(almSettingDto.getKey());
  }

  public void update(DbSession dbSession, AlmSettingDto almSettingDto) {
    long now = system2.now();
    getMapper(dbSession).update(almSettingDto, now);
    almSettingDto.setUpdatedAt(now);
  }
}
