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
package org.sonar.db.project;

import javax.annotation.CheckForNull;
import org.sonar.api.utils.System2;
import org.sonar.core.util.UuidFactory;
import org.sonar.db.Dao;
import org.sonar.db.DbSession;
import org.sonar.db.audit.AuditPersister;
import org.sonar.db.audit.model.ProjectBadgeTokenNewValue;

public class ProjectBadgeTokenDao implements Dao {
  private final System2 system2;
  private final AuditPersister auditPersister;
  private final UuidFactory uuidFactory;

  public ProjectBadgeTokenDao(System2 system2, AuditPersister auditPersister, UuidFactory uuidFactory) {
    this.system2 = system2;
    this.auditPersister = auditPersister;
    this.uuidFactory = uuidFactory;
  }

  public ProjectBadgeTokenDto insert(DbSession session, String token, ProjectDto projectDto,
    String userUuid, String userLogin) {
    ProjectBadgeTokenDto projectBadgeTokenDto = new ProjectBadgeTokenDto(uuidFactory.create(), token,
      projectDto.getUuid(), system2.now(), system2.now());

    auditPersister.addProjectBadgeToken(session, new ProjectBadgeTokenNewValue(projectDto.getKey(), userUuid, userLogin));

    mapper(session).insert(projectBadgeTokenDto);
    return projectBadgeTokenDto;
  }

  public void upsert(DbSession session, String token, ProjectDto projectDto, String userUuid, String userLogin) {
    if(selectTokenByProject(session, projectDto) == null) {
      insert(session, token, projectDto, userUuid, userLogin);
    } else {
      mapper(session).update(token, projectDto.getUuid(), system2.now());
      auditPersister.updateProjectBadgeToken(session, new ProjectBadgeTokenNewValue(projectDto.getKey(), userUuid, userLogin));
    }
  }

  private static ProjectBadgeTokenMapper mapper(DbSession session) {
    return session.getMapper(ProjectBadgeTokenMapper.class);
  }

  @CheckForNull
  public ProjectBadgeTokenDto selectTokenByProject(DbSession session, ProjectDto projectDto) {
    return mapper(session).selectTokenByProjectUuid(projectDto.getUuid());

  }
}
