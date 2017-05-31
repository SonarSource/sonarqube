/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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

import com.google.common.base.Preconditions;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonar.api.utils.System2;
import org.sonar.db.Dao;
import org.sonar.db.DatabaseUtils;
import org.sonar.db.DbSession;
import org.sonar.db.KeyLongValue;
import org.sonar.db.RowNotFoundException;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.organization.OrganizationDto;

import static org.sonar.db.DatabaseUtils.executeLargeInputs;
import static org.sonar.db.DatabaseUtils.executeLargeUpdates;

public class QualityProfileDao implements Dao {

  private final System2 system;

  public QualityProfileDao(System2 system) {
    this.system = system;
  }

  @CheckForNull
  public QProfileDto selectByUuid(DbSession session, String uuid) {
    return mapper(session).selectByUuid(uuid);
  }

  public QProfileDto selectOrFailByUuid(DbSession session, String uuid) {
    QProfileDto dto = selectByUuid(session, uuid);
    if (dto == null) {
      throw new RowNotFoundException("Quality profile not found: " + uuid);
    }
    return dto;
  }

  public List<QProfileDto> selectByUuids(DbSession session, List<String> uuids) {
    return executeLargeInputs(uuids, mapper(session)::selectByUuids);
  }

  public List<QProfileDto> selectAll(DbSession session, OrganizationDto organization) {
    return mapper(session).selectAll(organization.getUuid());
  }

  public void insert(DbSession session, QProfileDto profile, QProfileDto... otherProfiles) {
    QualityProfileMapper mapper = mapper(session);
    doInsert(mapper, profile);
    for (QProfileDto other : otherProfiles) {
      doInsert(mapper, other);
    }
  }

  private void doInsert(QualityProfileMapper mapper, QProfileDto profile) {
    Preconditions.checkArgument(profile.getId() == null, "Quality profile is already persisted (got id %d)", profile.getId());
    long now = system.now();
    mapper.insertRulesProfile(profile, new Date(now));
    mapper.insertOrgQProfile(profile, now);
  }

  public void update(DbSession session, QProfileDto profile, QProfileDto... otherProfiles) {
    QualityProfileMapper mapper = mapper(session);
    long now = system.now();
    doUpdate(mapper, profile, now);
    for (QProfileDto otherProfile : otherProfiles) {
      doUpdate(mapper, otherProfile, now);
    }
  }

  private void doUpdate(QualityProfileMapper mapper, QProfileDto profile, long now) {
    mapper.updateRulesProfile(profile, new Date(now));
    mapper.updateOrgQProfile(profile, now);
  }

  public List<QProfileDto> selectDefaultProfiles(DbSession session, OrganizationDto organization, Collection<String> languages) {
    return mapper(session).selectDefaultProfiles(organization.getUuid(), languages);
  }

  @CheckForNull
  public QProfileDto selectDefaultProfile(DbSession session, OrganizationDto organization, String language) {
    return mapper(session).selectDefaultProfile(organization.getUuid(), language);
  }

  @CheckForNull
  public QProfileDto selectAssociatedToProjectAndLanguage(DbSession session, ComponentDto project, String language) {
    return mapper(session).selectAssociatedToProjectUuidAndLanguage(project.getOrganizationUuid(), project.projectUuid(), language);
  }

  public List<QProfileDto> selectAssociatedToProjectUuidAndLanguages(DbSession session, ComponentDto project, Collection<String> languages) {
    return mapper(session).selectAssociatedToProjectUuidAndLanguages(project.getOrganizationUuid(), project.uuid(), languages);
  }

  public List<QProfileDto> selectByLanguage(DbSession dbSession, OrganizationDto organization, String language) {
    return mapper(dbSession).selectByLanguage(organization.getUuid(), language);
  }

  public List<QProfileDto> selectChildren(DbSession session, String uuid) {
    return mapper(session).selectChildren(uuid);
  }

  /**
   * All descendants, in the top-down order.
   */
  public List<QProfileDto> selectDescendants(DbSession session, String uuid) {
    List<QProfileDto> descendants = new ArrayList<>();
    for (QProfileDto child : selectChildren(session, uuid)) {
      descendants.add(child);
      descendants.addAll(selectDescendants(session, child.getKee()));
    }
    return descendants;
  }

  @CheckForNull
  public QProfileDto selectByNameAndLanguage(DbSession session, OrganizationDto organization, String name, String language) {
    return mapper(session).selectByNameAndLanguage(organization.getUuid(), name, language);
  }

  public List<QProfileDto> selectByNameAndLanguages(DbSession session, OrganizationDto organization, String name, Collection<String> languages) {
    return mapper(session).selectByNameAndLanguages(organization.getUuid(), name, languages);
  }

  public Map<String, Long> countProjectsByProfileUuid(DbSession dbSession, OrganizationDto organization) {
    return KeyLongValue.toMap(mapper(dbSession).countProjectsByProfileUuid(organization.getUuid()));
  }

  public void insertProjectProfileAssociation(DbSession dbSession, ComponentDto project, QProfileDto profile) {
    mapper(dbSession).insertProjectProfileAssociation(project.uuid(), profile.getKee());
  }

  public void deleteProjectProfileAssociation(DbSession dbSession, ComponentDto project, QProfileDto profile) {
    mapper(dbSession).deleteProjectProfileAssociation(project.uuid(), profile.getKee());
  }

  public void updateProjectProfileAssociation(DbSession dbSession, ComponentDto project, String newProfileUuid, String oldProfileUuid) {
    mapper(dbSession).updateProjectProfileAssociation(project.uuid(), newProfileUuid, oldProfileUuid);
  }

  public void deleteProjectAssociationsByProfileUuids(DbSession dbSession, Collection<String> profileUuids) {
    QualityProfileMapper mapper = mapper(dbSession);
    DatabaseUtils.executeLargeUpdates(profileUuids, mapper::deleteProjectAssociationByProfileUuids);
  }

  public List<ProjectQprofileAssociationDto> selectSelectedProjects(DbSession session, OrganizationDto organization, QProfileDto profile, @Nullable String query) {
    String nameQuery = sqlQueryString(query);
    return mapper(session).selectSelectedProjects(organization.getUuid(), profile.getKee(), nameQuery);
  }

  public List<ProjectQprofileAssociationDto> selectDeselectedProjects(DbSession session, OrganizationDto organization, QProfileDto profile, @Nullable String query) {
    String nameQuery = sqlQueryString(query);
    return mapper(session).selectDeselectedProjects(organization.getUuid(), profile.getKee(), nameQuery);
  }

  public List<ProjectQprofileAssociationDto> selectProjectAssociations(DbSession session, OrganizationDto organization, QProfileDto profile, @Nullable String query) {
    String nameQuery = sqlQueryString(query);
    return mapper(session).selectProjectAssociations(organization.getUuid(), profile.getKee(), nameQuery);
  }

  public Collection<String> selectUuidsOfCustomRulesProfiles(DbSession dbSession, String language, String name) {
    return mapper(dbSession).selectUuidsOfCustomQProfiles(language, name);
  }

  public void renameRulesProfilesAndCommit(DbSession dbSession, Collection<String> rulesProfileUuids, String newName) {
    QualityProfileMapper mapper = mapper(dbSession);
    Date now = new Date(system.now());
    executeLargeUpdates(rulesProfileUuids, partition -> {
      mapper.renameRulesProfiles(newName, now, partition);
      dbSession.commit();
    });
  }

  public void deleteByUuids(DbSession dbSession, Collection<String> profileUuids) {
    QualityProfileMapper mapper = mapper(dbSession);
    DatabaseUtils.executeLargeUpdates(profileUuids, mapper::deleteOrgQProfilesByUuids);
    DatabaseUtils.executeLargeUpdates(profileUuids, mapper::deleteRulesProfilesByUuids);
  }

  private static String sqlQueryString(@Nullable String query) {
    if (query == null) {
      return "%";
    }
    return "%" + query.toUpperCase(Locale.ENGLISH) + "%";
  }

  private static QualityProfileMapper mapper(DbSession session) {
    return session.getMapper(QualityProfileMapper.class);
  }
}
