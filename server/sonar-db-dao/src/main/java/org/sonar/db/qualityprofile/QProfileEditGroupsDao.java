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
package org.sonar.db.qualityprofile;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.sonar.api.utils.System2;
import org.sonar.db.Dao;
import org.sonar.db.DatabaseUtils;
import org.sonar.db.DbSession;
import org.sonar.db.Pagination;
import org.sonar.db.audit.AuditPersister;
import org.sonar.db.audit.model.GroupEditorNewValue;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.user.GroupDto;
import org.sonar.db.user.SearchGroupMembershipDto;

import static org.sonar.db.DatabaseUtils.executeLargeInputs;
import static org.sonar.db.DatabaseUtils.executeLargeUpdates;

public class QProfileEditGroupsDao implements Dao {

  private final System2 system2;
  private final AuditPersister auditPersister;

  public QProfileEditGroupsDao(System2 system2, AuditPersister auditPersister) {
    this.system2 = system2;
    this.auditPersister = auditPersister;
  }

  public boolean exists(DbSession dbSession, QProfileDto profile, GroupDto group) {
    return exists(dbSession, profile, Collections.singletonList(group));
  }

  public boolean exists(DbSession dbSession, QProfileDto profile, Collection<GroupDto> groups) {
    return !executeLargeInputs(groups.stream().map(GroupDto::getUuid).toList(), partition -> mapper(dbSession).selectByQProfileAndGroups(profile.getKee(), partition))
      .isEmpty();
  }

  public int countByQuery(DbSession dbSession, SearchQualityProfilePermissionQuery query) {
    return mapper(dbSession).countByQuery(query);
  }

  public List<SearchGroupMembershipDto> selectByQuery(DbSession dbSession, SearchQualityProfilePermissionQuery query, Pagination pagination) {
    return mapper(dbSession).selectByQuery(query, pagination);
  }

  public List<String> selectQProfileUuidsByOrganizationAndGroups(DbSession dbSession, OrganizationDto organization, Collection<GroupDto> groups) {
    return DatabaseUtils.executeLargeInputs(groups.stream().map(GroupDto::getUuid).toList(),
      g -> mapper(dbSession).selectQProfileUuidsByOrganizationAndGroups(organization.getUuid(), g));
  }

  public void insert(DbSession dbSession, QProfileEditGroupsDto dto, String qualityProfileName, String groupName) {
    mapper(dbSession).insert(dto, system2.now());
    auditPersister.addQualityProfileEditor(dbSession, new GroupEditorNewValue(dto, qualityProfileName, groupName));
  }

  public void deleteByQProfileAndGroup(DbSession dbSession, QProfileDto profile, GroupDto group) {
    int deletedRows = mapper(dbSession).delete(profile.getKee(), group.getUuid());

    if (deletedRows > 0) {
      auditPersister.deleteQualityProfileEditor(dbSession, new GroupEditorNewValue(profile, group));
    }
  }

  public void deleteByQProfiles(DbSession dbSession, List<QProfileDto> qProfiles) {
    executeLargeUpdates(qProfiles,
      partitionedProfiles ->
      {
        int deletedRows = mapper(dbSession).deleteByQProfiles(partitionedProfiles
          .stream()
          .map(QProfileDto::getKee)
          .toList());

        if (deletedRows > 0) {
          partitionedProfiles.forEach(p -> auditPersister.deleteQualityProfileEditor(dbSession, new GroupEditorNewValue(p)));
        }
      });
  }

  public void deleteByGroup(DbSession dbSession, GroupDto group) {
    int deletedRows = mapper(dbSession).deleteByGroup(group.getUuid());

    if (deletedRows > 0) {
      auditPersister.deleteQualityProfileEditor(dbSession, new GroupEditorNewValue(group));
    }
  }

  private static QProfileEditGroupsMapper mapper(DbSession dbSession) {
    return dbSession.getMapper(QProfileEditGroupsMapper.class);
  }
}
