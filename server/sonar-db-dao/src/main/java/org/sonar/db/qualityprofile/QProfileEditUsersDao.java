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

import static org.sonar.db.DatabaseUtils.executeLargeUpdates;

import java.util.List;

import java.util.Set;
import java.util.stream.Collectors;
import org.sonar.api.utils.System2;
import org.sonar.db.Dao;
import org.sonar.db.DbSession;
import org.sonar.db.Pagination;
import org.sonar.db.audit.AuditPersister;
import org.sonar.db.audit.model.UserEditorNewValue;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.user.SearchUserMembershipDto;
import org.sonar.db.user.UserDto;

public class QProfileEditUsersDao implements Dao {

  private final System2 system2;
  private final AuditPersister auditPersister;

  public QProfileEditUsersDao(System2 system2, AuditPersister auditPersister) {
    this.system2 = system2;
    this.auditPersister = auditPersister;
  }

  public boolean exists(DbSession dbSession, QProfileDto profile, UserDto user) {
    return mapper(dbSession).selectByQProfileAndUser(profile.getKee(), user.getUuid()) != null;
  }

  public int countByQuery(DbSession dbSession, SearchQualityProfilePermissionQuery query) {
    return mapper(dbSession).countByQuery(query);
  }

  public List<SearchUserMembershipDto> selectByQuery(DbSession dbSession, SearchQualityProfilePermissionQuery query, Pagination pagination) {
    return mapper(dbSession).selectByQuery(query, pagination);
  }

  public List<String> selectQProfileUuidsByOrganizationAndUser(DbSession dbSession, OrganizationDto organization, UserDto userDto) {
    return mapper(dbSession).selectQProfileUuidsByOrganizationAndUser(organization.getUuid(), userDto.getUuid());
  }

  public void insert(DbSession dbSession, QProfileEditUsersDto dto, String qualityProfileName, String userLogin, String organizationUuid) {
    mapper(dbSession).insert(dto, system2.now());
    auditPersister.addQualityProfileEditor(dbSession, organizationUuid, new UserEditorNewValue(dto, qualityProfileName, userLogin));
  }

  public void deleteByQProfileAndUser(DbSession dbSession, QProfileDto profile, UserDto user) {
    int deletedRows = mapper(dbSession).delete(profile.getKee(), user.getUuid());

    if (deletedRows > 0) {
      auditPersister.deleteQualityProfileEditor(dbSession, profile.getOrganizationUuid(), new UserEditorNewValue(profile, user));
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
        partitionedProfiles.forEach(p -> auditPersister.deleteQualityProfileEditor(dbSession, p.getOrganizationUuid(), new UserEditorNewValue(p)));
      }
    });
  }

  public void deleteByUser(DbSession dbSession, UserDto user) {
    // Get all quality profiles that the user has edit permissions for
    List<QProfileEditUsersDto> editors = mapper(dbSession).selectByUser(user.getUuid());

    // Delete all editors for this user
    int deletedRows = mapper(dbSession).deleteByUser(user.getUuid());

    if (deletedRows > 0) {
      // Process quality profiles in batches to handle large datasets
      Set<String> qProfileUuids = editors.stream()
          .map(QProfileEditUsersDto::getQProfileUuid)
          .collect(Collectors.toSet());

      executeLargeUpdates(qProfileUuids, partitionedQProfileUuids -> {
        partitionedQProfileUuids.forEach(qProfileUuid -> {
          QProfileDto profile = dbSession.getMapper(QualityProfileMapper.class).selectByUuid(qProfileUuid);
          if (profile != null) {
            auditPersister.deleteQualityProfileEditor(dbSession, profile.getOrganizationUuid(), new UserEditorNewValue(user));
          }
        });
      });
    }
  }

  public void deleteByOrganizationAndUser(DbSession dbSession, OrganizationDto organization, UserDto user) {
    mapper(dbSession).deleteByOrganizationAndUser(organization.getUuid(), user.getUuid());
  }

  private static QProfileEditUsersMapper mapper(DbSession dbSession) {
    return dbSession.getMapper(QProfileEditUsersMapper.class);
  }
}
