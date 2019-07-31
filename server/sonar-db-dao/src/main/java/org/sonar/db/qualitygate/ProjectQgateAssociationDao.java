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
package org.sonar.db.qualitygate;

import java.util.List;
import java.util.Optional;
import org.sonar.db.Dao;
import org.sonar.db.DbSession;

public class ProjectQgateAssociationDao implements Dao {

  public List<ProjectQgateAssociationDto> selectProjects(DbSession dbSession, ProjectQgateAssociationQuery query) {
    return mapper(dbSession).selectProjects(query);
  }

  /**
   * @return quality gate uuid if a specific Quality Gate has been defined for the given component uuid. <br>
   * Returns <code>{@link Optional#empty()}</code> otherwise (ex: default quality gate applies)
   */
  public Optional<String> selectQGateUuidByComponentUuid(DbSession dbSession, String componentUuid) {
    String uuid = mapper(dbSession).selectQGateUuidByComponentUuid(componentUuid);
    return Optional.ofNullable(uuid);
  }

  private static ProjectQgateAssociationMapper mapper(DbSession session) {
    return session.getMapper(ProjectQgateAssociationMapper.class);
  }

  public void deleteByProjectUuid(DbSession dbSession, String projectUuid) {
    mapper(dbSession).deleteByProjectUuid(projectUuid);
  }

  public void deleteByQGateUuid(DbSession dbSession, String qGateUuid) {
    mapper(dbSession).deleteByQGateUuid(qGateUuid);
  }

  public void insertProjectQGateAssociation(DbSession dbSession, String projectUuid, String qGateUuid) {
    mapper(dbSession).insertProjectQGateAssociation(projectUuid, qGateUuid);
  }

  public void updateProjectQGateAssociation(DbSession dbSession, String projectUuid, String qGateUuid) {
    mapper(dbSession).updateProjectQGateAssociation(projectUuid, qGateUuid);
  }
}
