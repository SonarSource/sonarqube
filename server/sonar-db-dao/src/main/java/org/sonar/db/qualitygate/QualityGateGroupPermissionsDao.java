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
package org.sonar.db.qualitygate;

import java.util.Collection;
import java.util.List;
import org.sonar.api.utils.System2;
import org.sonar.db.Dao;
import org.sonar.db.DbSession;
import org.sonar.db.Pagination;
import org.sonar.db.audit.AuditPersister;
import org.sonar.db.audit.model.GroupEditorNewValue;
import org.sonar.db.user.GroupDto;
import org.sonar.db.user.SearchGroupMembershipDto;
import org.sonar.db.user.SearchPermissionQuery;

import static org.sonar.core.util.stream.MoreCollectors.toList;
import static org.sonar.db.DatabaseUtils.executeLargeInputs;

public class QualityGateGroupPermissionsDao implements Dao {
  private final System2 system2;
  private final AuditPersister auditPersister;

  public QualityGateGroupPermissionsDao(System2 system2, AuditPersister auditPersister) {
    this.system2 = system2;
    this.auditPersister = auditPersister;
  }

  public boolean exists(DbSession dbSession, QualityGateDto qualityGate, GroupDto group) {
    return this.exists(dbSession, qualityGate.getUuid(), group.getUuid());
  }

  public boolean exists(DbSession dbSession, String qualityGateUuid, String groupUuid) {
    return mapper(dbSession).selectByQualityGateAndGroup(qualityGateUuid, groupUuid) != null;
  }

  public boolean exists(DbSession dbSession, QualityGateDto qualityGate, Collection<GroupDto> groups) {
    return !executeLargeInputs(groups.stream().map(GroupDto::getUuid).collect(toList()),
      partition -> mapper(dbSession).selectByQualityGateAndGroups(qualityGate.getUuid(), partition))
      .isEmpty();
  }

  public void insert(DbSession dbSession, QualityGateGroupPermissionsDto dto, String qualityGateName, String groupName) {
    mapper(dbSession).insert(dto, system2.now());
    auditPersister.addQualityGateEditor(dbSession, new GroupEditorNewValue(dto, qualityGateName, groupName));
  }

  public List<SearchGroupMembershipDto> selectByQuery(DbSession dbSession, SearchPermissionQuery query, Pagination pagination) {
    return mapper(dbSession).selectByQuery(query, pagination);
  }

  public int countByQuery(DbSession dbSession, SearchPermissionQuery query) {
    return mapper(dbSession).countByQuery(query);
  }

  public void deleteByGroup(DbSession dbSession, GroupDto group) {
    int deletedRows = mapper(dbSession).deleteByGroup(group.getUuid());

    if (deletedRows > 0) {
      auditPersister.deleteQualityGateEditor(dbSession, new GroupEditorNewValue(group));
    }
  }

  public void deleteByQualityGateAndGroup(DbSession dbSession, QualityGateDto qualityGate, GroupDto group) {
    int deletedRows = mapper(dbSession).delete(qualityGate.getUuid(), group.getUuid());

    if (deletedRows > 0) {
      auditPersister.deleteQualityGateEditor(dbSession, new GroupEditorNewValue(qualityGate, group));
    }
  }

  public void deleteByQualityGate(DbSession dbSession, QualityGateDto qualityGate) {
    int deletedRows = mapper(dbSession).deleteByQualityGate(qualityGate.getUuid());

    if (deletedRows > 0) {
      auditPersister.deleteQualityGateEditor(dbSession, new GroupEditorNewValue(qualityGate));
    }
  }

  private static QualityGateGroupPermissionsMapper mapper(DbSession dbSession) {
    return dbSession.getMapper(QualityGateGroupPermissionsMapper.class);
  }
}
