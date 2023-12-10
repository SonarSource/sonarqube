/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import org.sonar.api.utils.System2;
import org.sonar.core.util.UuidFactory;
import org.sonar.db.Dao;
import org.sonar.db.DbSession;
import org.sonar.db.audit.AuditPersister;
import org.sonar.db.audit.model.DevOpsPlatformSettingNewValue;
import org.sonar.db.project.ProjectDto;

import static org.sonar.db.DatabaseUtils.executeLargeInputs;

public class ProjectAlmSettingDao implements Dao {

  private final System2 system2;
  private final UuidFactory uuidFactory;
  private final AuditPersister auditPersister;

  public ProjectAlmSettingDao(System2 system2, UuidFactory uuidFactory, AuditPersister auditPersister) {
    this.system2 = system2;
    this.uuidFactory = uuidFactory;
    this.auditPersister = auditPersister;
  }

  public void insertOrUpdate(DbSession dbSession, ProjectAlmSettingDto projectAlmSettingDto, String key, String projectName, String projectKey) {
    String uuid = uuidFactory.create();
    long now = system2.now();
    ProjectAlmSettingMapper mapper = getMapper(dbSession);
    boolean isUpdate = true;

    if (mapper.update(projectAlmSettingDto, now) == 0) {
      mapper.insert(projectAlmSettingDto, uuid, now);
      projectAlmSettingDto.setUuid(uuid);
      projectAlmSettingDto.setCreatedAt(now);
      isUpdate = false;
    }
    projectAlmSettingDto.setUpdatedAt(now);

    DevOpsPlatformSettingNewValue value = new DevOpsPlatformSettingNewValue(projectAlmSettingDto, key, projectName, projectKey);
    if (isUpdate) {
      auditPersister.updateDevOpsPlatformSetting(dbSession, value);
    } else {
      auditPersister.addDevOpsPlatformSetting(dbSession, value);
    }
  }

  public void deleteByProject(DbSession dbSession, ProjectDto project) {
    int deletedRows = getMapper(dbSession).deleteByProjectUuid(project.getUuid());

    if (deletedRows > 0) {
      auditPersister.deleteDevOpsPlatformSetting(dbSession, new DevOpsPlatformSettingNewValue(project));
    }
  }

  public void deleteByAlmSetting(DbSession dbSession, AlmSettingDto almSetting) {
    getMapper(dbSession).deleteByAlmSettingUuid(almSetting.getUuid());
  }

  public int countByAlmSetting(DbSession dbSession, AlmSettingDto almSetting) {
    return getMapper(dbSession).countByAlmSettingUuid(almSetting.getUuid());
  }

  public Optional<ProjectAlmSettingDto> selectByProject(DbSession dbSession, ProjectDto project) {
    return selectByProject(dbSession, project.getUuid());
  }

  public Optional<ProjectAlmSettingDto> selectByProject(DbSession dbSession, String projectUuid) {
    return Optional.ofNullable(getMapper(dbSession).selectByProjectUuid(projectUuid));
  }

  private static ProjectAlmSettingMapper getMapper(DbSession dbSession) {
    return dbSession.getMapper(ProjectAlmSettingMapper.class);
  }

  public List<ProjectAlmSettingDto> selectByAlmSettingAndSlugs(DbSession dbSession, AlmSettingDto almSettingDto, Set<String> almSlugs) {
    return executeLargeInputs(almSlugs, slugs -> getMapper(dbSession).selectByAlmSettingAndSlugs(almSettingDto.getUuid(), slugs));
  }

  public List<ProjectAlmSettingDto> selectByAlmSettingAndRepos(DbSession dbSession, AlmSettingDto almSettingDto, Set<String> almRepos) {
    return executeLargeInputs(almRepos, repos -> getMapper(dbSession).selectByAlmSettingAndRepos(almSettingDto.getUuid(), repos));
  }

  public List<ProjectAlmSettingDto> selectByAlm(DbSession dbSession, ALM alm) {
    return getMapper(dbSession).selectByAlm(alm.getId().toLowerCase(Locale.ROOT));
  }

  public List<ProjectAlmSettingDto> selectByProjectUuidsAndAlm(DbSession dbSession, Set<String> projectUuids, ALM alm) {
    if (projectUuids.isEmpty()) {
      return Collections.emptyList();
    }
    return getMapper(dbSession).selectByProjectUuidsAndAlm(projectUuids, alm.getId().toLowerCase(Locale.ROOT));
  }

  public List<ProjectAlmKeyAndProject> selectAlmTypeAndUrlByProject(DbSession dbSession) {
    return getMapper(dbSession).selectAlmTypeAndUrlByProject();
  }
}
