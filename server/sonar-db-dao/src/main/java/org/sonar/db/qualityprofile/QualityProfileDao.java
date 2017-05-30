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
import com.google.common.collect.Lists;
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
  public RulesProfileDto selectByKey(DbSession session, String key) {
    return mapper(session).selectByKey(key);
  }

  public RulesProfileDto selectOrFailByKey(DbSession session, String key) {
    RulesProfileDto dto = selectByKey(session, key);
    if (dto == null) {
      throw new RowNotFoundException("Quality profile not found: " + key);
    }
    return dto;
  }

  public List<RulesProfileDto> selectByKeys(DbSession session, List<String> keys) {
    return executeLargeInputs(keys, mapper(session)::selectByKeys);
  }

  public List<RulesProfileDto> selectAll(DbSession session, OrganizationDto organization) {
    return mapper(session).selectAll(organization.getUuid());
  }

  public void insert(DbSession session, RulesProfileDto profile, RulesProfileDto... otherProfiles) {
    QualityProfileMapper mapper = mapper(session);
    doInsert(mapper, profile);
    for (RulesProfileDto other : otherProfiles) {
      doInsert(mapper, other);
    }
  }

  private void doInsert(QualityProfileMapper mapper, RulesProfileDto profile) {
    Preconditions.checkArgument(profile.getId() == null, "Quality profile is already persisted (got id %d)", profile.getId());
    mapper.insert(profile, new Date(system.now()));
  }

  public void update(DbSession session, RulesProfileDto profile, RulesProfileDto... otherProfiles) {
    QualityProfileMapper mapper = mapper(session);
    doUpdate(mapper, profile);
    for (RulesProfileDto otherProfile : otherProfiles) {
      doUpdate(mapper, otherProfile);
    }
  }

  private void doUpdate(QualityProfileMapper mapper, RulesProfileDto profile) {
    Preconditions.checkArgument(profile.getId() != null, "Quality profile is not persisted");
    mapper.update(profile, new Date(system.now()));
  }

  public List<RulesProfileDto> selectDefaultProfiles(DbSession session, OrganizationDto organization, Collection<String> languageKeys) {
    return mapper(session).selectDefaultProfiles(organization.getUuid(), languageKeys);
  }

  @CheckForNull
  public RulesProfileDto selectDefaultProfile(DbSession session, OrganizationDto organization, String language) {
    return mapper(session).selectDefaultProfile(organization.getUuid(), language);
  }

  @CheckForNull
  public RulesProfileDto selectAssociatedToProjectAndLanguage(DbSession session, ComponentDto project, String language) {
    return mapper(session).selectAssociatedToProjectUuidAndLanguage(project.getOrganizationUuid(), project.projectUuid(), language);
  }

  public List<RulesProfileDto> selectAssociatedToProjectUuidAndLanguages(DbSession session, ComponentDto project, Collection<String> languages) {
    return mapper(session).selectAssociatedToProjectUuidAndLanguages(project.getOrganizationUuid(), project.uuid(), languages);
  }

  public List<RulesProfileDto> selectByLanguage(DbSession dbSession, OrganizationDto organization, String language) {
    return mapper(dbSession).selectByLanguage(organization.getUuid(), language);
  }

  public List<RulesProfileDto> selectChildren(DbSession session, String key) {
    return mapper(session).selectChildren(key);
  }

  /**
   * All descendants, in the top-down order.
   */
  public List<RulesProfileDto> selectDescendants(DbSession session, String key) {
    List<RulesProfileDto> descendants = Lists.newArrayList();
    for (RulesProfileDto child : selectChildren(session, key)) {
      descendants.add(child);
      descendants.addAll(selectDescendants(session, child.getKee()));
    }
    return descendants;
  }

  @CheckForNull
  public RulesProfileDto selectByNameAndLanguage(OrganizationDto organization, String name, String language, DbSession session) {
    return mapper(session).selectByNameAndLanguage(organization.getUuid(), name, language);
  }

  public List<RulesProfileDto> selectByNameAndLanguages(OrganizationDto organization, String name, Collection<String> languageKeys, DbSession session) {
    return mapper(session).selectByNameAndLanguages(organization.getUuid(), name, languageKeys);
  }

  public Map<String, Long> countProjectsByProfileKey(DbSession dbSession, OrganizationDto organization) {
    return KeyLongValue.toMap(mapper(dbSession).countProjectsByProfileKey(organization.getUuid()));
  }

  public void insertProjectProfileAssociation(DbSession dbSession, ComponentDto project, RulesProfileDto profile) {
    mapper(dbSession).insertProjectProfileAssociation(project.uuid(), profile.getKee());
  }

  public void deleteProjectProfileAssociation(DbSession dbSession, ComponentDto project, RulesProfileDto profile) {
    mapper(dbSession).deleteProjectProfileAssociation(project.uuid(), profile.getKee());
  }

  public void updateProjectProfileAssociation(DbSession dbSession, ComponentDto project, String newProfileKey, String oldProfileKey) {
    mapper(dbSession).updateProjectProfileAssociation(project.uuid(), newProfileKey, oldProfileKey);
  }

  public void deleteProjectAssociationsByProfileKeys(DbSession dbSession, Collection<String> profileKeys) {
    QualityProfileMapper mapper = mapper(dbSession);
    DatabaseUtils.executeLargeUpdates(profileKeys, mapper::deleteProjectAssociationByProfileKeys);
  }

  public List<ProjectQprofileAssociationDto> selectSelectedProjects(OrganizationDto organization, String profileKey, @Nullable String query, DbSession session) {
    String nameQuery = sqlQueryString(query);
    return mapper(session).selectSelectedProjects(organization.getUuid(), profileKey, nameQuery);
  }

  public List<ProjectQprofileAssociationDto> selectDeselectedProjects(OrganizationDto organization, String profileKey, @Nullable String query, DbSession session) {
    String nameQuery = sqlQueryString(query);
    return mapper(session).selectDeselectedProjects(organization.getUuid(), profileKey, nameQuery);
  }

  public List<ProjectQprofileAssociationDto> selectProjectAssociations(OrganizationDto organization, String profileKey, @Nullable String query, DbSession session) {
    String nameQuery = sqlQueryString(query);
    return mapper(session).selectProjectAssociations(organization.getUuid(), profileKey, nameQuery);
  }

  public Collection<String> selectOutdatedProfiles(DbSession dbSession, String language, String name) {
    return mapper(dbSession).selectOutdatedProfiles(language, name);
  }

  public void renameAndCommit(DbSession dbSession, Collection<String> keys, String newName) {
    QualityProfileMapper mapper = mapper(dbSession);
    Date now = new Date(system.now());
    executeLargeUpdates(keys, partition -> {
      mapper.rename(newName, now, partition);
      dbSession.commit();
    });
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

  public void deleteByKeys(DbSession dbSession, Collection<String> profileKeys) {
    QualityProfileMapper mapper = mapper(dbSession);
    DatabaseUtils.executeLargeUpdates(profileKeys, mapper::deleteByKeys);
  }
}
