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

import java.util.Optional;
import org.sonar.api.utils.System2;
import org.sonar.core.util.UuidFactory;
import org.sonar.db.Dao;
import org.sonar.db.DbSession;
import org.sonar.db.component.ComponentDto;

public class ProjectAlmSettingDao implements Dao {

  private final System2 system2;
  private final UuidFactory uuidFactory;

  public ProjectAlmSettingDao(System2 system2, UuidFactory uuidFactory) {
    this.system2 = system2;
    this.uuidFactory = uuidFactory;
  }

  public void insertOrUpdate(DbSession dbSession, ProjectAlmSettingDto projectAlmSettingDto) {
    String uuid = uuidFactory.create();
    long now = system2.now();
    ProjectAlmSettingMapper mapper = getMapper(dbSession);

    if (mapper.update(projectAlmSettingDto, now) == 0) {
      mapper.insert(projectAlmSettingDto, uuid, now);
      projectAlmSettingDto.setUuid(uuid);
      projectAlmSettingDto.setCreatedAt(now);
    }
    projectAlmSettingDto.setUpdatedAt(now);
  }

  public void deleteByProject(DbSession dbSession, ComponentDto project) {
    getMapper(dbSession).deleteByProjectUuid(project.uuid());
  }

  public void deleteByAlmSetting(DbSession dbSession, AlmSettingDto almSetting) {
    getMapper(dbSession).deleteByAlmSettingUuid(almSetting.getUuid());
  }

  public int countByAlmSetting(DbSession dbSession, AlmSettingDto almSetting) {
    return getMapper(dbSession).countByAlmSettingUuid(almSetting.getUuid());
  }

  public Optional<ProjectAlmSettingDto> selectByProject(DbSession dbSession, ComponentDto project) {
    return selectByProject(dbSession, project.uuid());
  }

  public Optional<ProjectAlmSettingDto> selectByProject(DbSession dbSession, String projectUuid) {
    return Optional.ofNullable(getMapper(dbSession).selectByProjectUuid(projectUuid));
  }

  private static ProjectAlmSettingMapper getMapper(DbSession dbSession) {
    return dbSession.getMapper(ProjectAlmSettingMapper.class);
  }

}
