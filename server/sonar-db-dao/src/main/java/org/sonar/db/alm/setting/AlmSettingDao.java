/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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
import org.sonar.db.audit.AuditPersister;
import org.sonar.db.audit.model.DevOpsPlatformSettingNewValue;
import org.sonar.db.audit.model.SecretNewValue;

public class AlmSettingDao implements Dao {

  private final System2 system2;
  private final UuidFactory uuidFactory;
  private final AuditPersister auditPersister;

  public AlmSettingDao(System2 system2, UuidFactory uuidFactory, AuditPersister auditPersister) {
    this.system2 = system2;
    this.uuidFactory = uuidFactory;
    this.auditPersister = auditPersister;
  }

  private static AlmSettingMapper getMapper(DbSession dbSession) {
    return dbSession.getMapper(AlmSettingMapper.class);
  }

  public void insert(DbSession dbSession, AlmSettingDto almSettingDto) {
    String uuid = uuidFactory.create();
    long now = system2.now();
    almSettingDto.setUuid(uuid);
    almSettingDto.setCreatedAt(now);
    almSettingDto.setUpdatedAt(now);
    getMapper(dbSession).insert(almSettingDto);

    auditPersister.addDevOpsPlatformSetting(dbSession, new DevOpsPlatformSettingNewValue(almSettingDto));
  }

  public Optional<AlmSettingDto> selectByUuid(DbSession dbSession, String uuid) {
    return Optional.ofNullable(getMapper(dbSession).selectByUuid(uuid));
  }

  public Optional<AlmSettingDto> selectByKey(DbSession dbSession, String key) {
    return Optional.ofNullable(getMapper(dbSession).selectByKey(key));
  }

  public Optional<AlmSettingDto> selectByAlmAndAppId(DbSession dbSession, ALM alm, String appId) {
    return selectByAlm(dbSession, alm)
      .stream()
      .filter(almSettingDto -> appId.equals(almSettingDto.getAppId()))
      .findAny();
  }

  public List<AlmSettingDto> selectByAlm(DbSession dbSession, ALM alm) {
    return getMapper(dbSession).selectByAlm(alm.getId());
  }

  public List<AlmSettingDto> selectAll(DbSession dbSession) {
    return getMapper(dbSession).selectAll();
  }

  public void delete(DbSession dbSession, AlmSettingDto almSettingDto) {
    int deletedRows = getMapper(dbSession).deleteByKey(almSettingDto.getKey());

    if (deletedRows > 0) {
      auditPersister.deleteDevOpsPlatformSetting(dbSession, new DevOpsPlatformSettingNewValue(almSettingDto.getUuid(), almSettingDto.getKey()));
    }
  }

  public void update(DbSession dbSession, AlmSettingDto almSettingDto, boolean updateSecret) {
    long now = system2.now();
    almSettingDto.setUpdatedAt(now);
    getMapper(dbSession).update(almSettingDto);
    if (updateSecret) {
      auditPersister.updateDevOpsPlatformSecret(dbSession, new SecretNewValue("DevOpsPlatform", almSettingDto.getRawAlm()));
    }
    auditPersister.updateDevOpsPlatformSetting(dbSession, new DevOpsPlatformSettingNewValue(almSettingDto));
  }
}
