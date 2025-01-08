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
package org.sonar.db.qualitygate;

import java.util.List;
import javax.annotation.Nullable;
import org.sonar.api.utils.System2;
import org.sonar.db.Dao;
import org.sonar.db.DbSession;
import org.sonar.db.Pagination;
import org.sonar.db.audit.AuditPersister;
import org.sonar.db.audit.model.UserEditorNewValue;
import org.sonar.db.user.SearchPermissionQuery;
import org.sonar.db.user.SearchUserMembershipDto;
import org.sonar.db.user.UserDto;

public class QualityGateUserPermissionsDao implements Dao {

  private final System2 system2;
  private final AuditPersister auditPersister;

  public QualityGateUserPermissionsDao(System2 system2, AuditPersister auditPersister) {
    this.system2 = system2;
    this.auditPersister = auditPersister;
  }

  public boolean exists(DbSession dbSession, QualityGateDto qualityGate, UserDto user) {
    return this.exists(dbSession, qualityGate.getUuid(), user.getUuid());
  }

  public boolean exists(DbSession dbSession, @Nullable String qualityGateUuid, @Nullable String userUuid) {
    if (qualityGateUuid == null || userUuid == null) {
      return false;
    }
    return selectByQualityGateAndUser(dbSession, qualityGateUuid, userUuid) != null;
  }

  public QualityGateUserPermissionsDto selectByQualityGateAndUser(DbSession dbSession, String qualityGateUuid, String userUuid) {
    return mapper(dbSession).selectByQualityGateAndUser(qualityGateUuid, userUuid);
  }

  public void insert(DbSession dbSession, QualityGateUserPermissionsDto dto, String qualityGateName, String userLogin) {
    mapper(dbSession).insert(dto, system2.now());
    auditPersister.addQualityGateEditor(dbSession, new UserEditorNewValue(dto, qualityGateName, userLogin));
  }

  public List<SearchUserMembershipDto> selectByQuery(DbSession dbSession, SearchPermissionQuery query, Pagination pagination) {
    return mapper(dbSession).selectByQuery(query, pagination);
  }

  public int countByQuery(DbSession dbSession, SearchPermissionQuery query) {
    return mapper(dbSession).countByQuery(query);
  }

  public void deleteByQualityGateAndUser(DbSession dbSession, QualityGateDto qualityGate, UserDto user) {
    int deletedRows = mapper(dbSession).delete(qualityGate.getUuid(), user.getUuid());

    if (deletedRows > 0) {
      auditPersister.deleteQualityGateEditor(dbSession, new UserEditorNewValue(qualityGate, user));
    }
  }

  public void deleteByUser(DbSession dbSession, UserDto user) {
    int deletedRows = mapper(dbSession).deleteByUser(user.getUuid());

    if (deletedRows > 0) {
      auditPersister.deleteQualityGateEditor(dbSession, new UserEditorNewValue(user));
    }
  }

  public void deleteByQualityGate(DbSession dbSession, QualityGateDto qualityGate) {
    int deletedRows = mapper(dbSession).deleteByQualityGate(qualityGate.getUuid());

    if (deletedRows > 0) {
      auditPersister.deleteQualityGateEditor(dbSession, new UserEditorNewValue(qualityGate));
    }
  }

  private static QualityGateUserPermissionsMapper mapper(DbSession dbSession) {
    return dbSession.getMapper(QualityGateUserPermissionsMapper.class);
  }
}
