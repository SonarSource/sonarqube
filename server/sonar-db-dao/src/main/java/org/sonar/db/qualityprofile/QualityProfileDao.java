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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.utils.System2;
import org.sonar.core.util.UuidFactory;
import org.sonar.db.Dao;
import org.sonar.db.DatabaseUtils;
import org.sonar.db.DbSession;
import org.sonar.db.KeyLongValue;
import org.sonar.db.RowNotFoundException;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.project.ProjectDto;

import static java.util.Collections.emptyList;
import static org.sonar.db.DatabaseUtils.executeLargeInputs;
import static org.sonar.db.DatabaseUtils.executeLargeUpdates;

public class QualityProfileDao implements Dao {

  private final Logger logger = LoggerFactory.getLogger(QualityProfileDao.class);

  private final System2 system;
  private final UuidFactory uuidFactory;

  public QualityProfileDao(UuidFactory uuidFactory, System2 system) {
    this.uuidFactory = uuidFactory;
    this.system = system;
  }

  @CheckForNull
  public QProfileDto selectByUuid(DbSession dbSession, String uuid) {
    return mapper(dbSession).selectByUuid(uuid);
  }

  public QProfileDto selectOrFailByUuid(DbSession dbSession, String uuid) {
    QProfileDto dto = selectByUuid(dbSession, uuid);
    if (dto == null) {
      throw new RowNotFoundException("Quality profile not found: " + uuid);
    }
    return dto;
  }

  public List<QProfileDto> selectByUuids(DbSession dbSession, List<String> uuids) {
    return executeLargeInputs(uuids, mapper(dbSession)::selectByUuids);
  }

  public List<QProfileDto> selectOrderedByOrganizationUuid(DbSession dbSession, OrganizationDto organization) {
    return mapper(dbSession).selectOrderedByOrganizationUuid(organization.getUuid());
  }

  public List<RulesProfileDto> selectBuiltInRuleProfiles(DbSession dbSession) {
    return mapper(dbSession).selectBuiltInRuleProfiles();
  }

  public List<RulesProfileDto> selectBuiltInRuleProfilesWithActiveRules(DbSession dbSession) {
    return mapper(dbSession).selectBuiltInRuleProfilesWithActiveRules();
  }

  @CheckForNull
  public RulesProfileDto selectRuleProfile(DbSession dbSession, String ruleProfileUuid) {
    return mapper(dbSession).selectRuleProfile(ruleProfileUuid);
  }

  public void insert(DbSession dbSession, RulesProfileDto dto) {
    QualityProfileMapper mapper = mapper(dbSession);
    mapper.insertRuleProfile(dto, new Date(system.now()));
  }

  public void insert(DbSession dbSession, OrgQProfileDto dto) {
    QualityProfileMapper mapper = mapper(dbSession);
    mapper.insertOrgQProfile(dto, system.now());
  }

  public void insert(DbSession dbSession, QProfileDto profile, QProfileDto... otherProfiles) {
    QualityProfileMapper mapper = mapper(dbSession);
    doInsert(mapper, profile);
    for (QProfileDto other : otherProfiles) {
      doInsert(mapper, other);
    }
  }

  private void doInsert(QualityProfileMapper mapper, QProfileDto profile) {
    long now = system.now();
    RulesProfileDto rulesProfile = RulesProfileDto.from(profile);
    mapper.insertRuleProfile(rulesProfile, new Date(now));
    mapper.insertOrgQProfile(OrgQProfileDto.from(profile), now);
  }

  public void update(DbSession dbSession, QProfileDto profile, QProfileDto... otherProfiles) {
    QualityProfileMapper mapper = mapper(dbSession);
    long now = system.now();
    doUpdate(mapper, profile, now);
    for (QProfileDto otherProfile : otherProfiles) {
      doUpdate(mapper, otherProfile, now);
    }
  }

  public int updateLastUsedDate(DbSession dbSession, QProfileDto profile, long lastUsedDate) {
    return mapper(dbSession).updateLastUsedDate(profile.getKee(), lastUsedDate, system.now());
  }

  public void update(DbSession dbSession, RulesProfileDto rulesProfile) {
    QualityProfileMapper mapper = mapper(dbSession);
    long now = system.now();
    mapper.updateRuleProfile(rulesProfile, new Date(now));
  }

  public void update(DbSession dbSession, OrgQProfileDto profile) {
    QualityProfileMapper mapper = mapper(dbSession);
    long now = system.now();
    mapper.updateOrgQProfile(profile, now);
  }

  private static void doUpdate(QualityProfileMapper mapper, QProfileDto profile, long now) {
    mapper.updateRuleProfile(RulesProfileDto.from(profile), new Date(now));
    mapper.updateOrgQProfile(OrgQProfileDto.from(profile), now);
  }

  public List<QProfileDto> selectAll(DbSession dbSession) {
    return mapper(dbSession).selectAll();
  }

  public List<QProfileDto> selectDefaultProfiles(DbSession dbSession, OrganizationDto organization, Collection<String> languages) {
    return executeLargeInputs(languages, partition -> mapper(dbSession).selectDefaultProfiles(organization.getUuid(), partition));
  }

  public List<QProfileDto> selectAllDefaultProfiles(DbSession dbSession) {
    return mapper(dbSession).selectAllDefaultProfiles();
  }

  public List<QProfileDto> selectDefaultProfilesWithoutActiveRules(DbSession dbSession, Set<String> languages, boolean builtIn) {
    return executeLargeInputs(languages, partition -> mapper(dbSession).selectDefaultProfilesWithoutActiveRules(partition, builtIn));
  }

  @CheckForNull
  public QProfileDto selectDefaultProfile(DbSession dbSession, OrganizationDto organization, String language) {
    return mapper(dbSession).selectDefaultProfile(organization.getUuid(), language);
  }

  @CheckForNull
  public List<String> selectDefaultProfileUuid(DbSession dbSession, String language) {
    return mapper(dbSession).selectDefaultProfileUuid(language);
  }

  @CheckForNull
  public QProfileDto selectAssociatedToProjectAndLanguage(DbSession dbSession, ProjectDto project, String language) {
    return mapper(dbSession).selectAssociatedToProjectUuidAndLanguage(project.getUuid(), language);
  }

  public List<QProfileDto> selectAssociatedToProjectAndLanguages(DbSession dbSession, ProjectDto project, Collection<String> languages) {
    return executeLargeInputs(languages, partition -> mapper(dbSession).selectAssociatedToProjectUuidAndLanguages(project.getUuid(), partition));
  }

  public List<QProfileDto> selectQProfilesByProjectUuid(DbSession dbSession, String projectUuid) {
    return mapper(dbSession).selectQProfilesByProjectUuid(projectUuid);
  }

  public List<QProfileDto> selectByLanguage(DbSession dbSession, String language) {
    return mapper(dbSession).selectByLanguage(language);
  }

  public List<QProfileDto> selectChildren(DbSession dbSession, Collection<QProfileDto> profiles) {
    List<String> uuids = profiles.stream().map(QProfileDto::getKee).toList();
    return DatabaseUtils.executeLargeInputs(uuids, chunk -> mapper(dbSession).selectChildren(chunk));
  }

  /**
   * All descendants, in any order. The specified profiles are not included into results.
   */
  public Collection<QProfileDto> selectDescendants(DbSession dbSession, Collection<QProfileDto> profiles) {
    if (profiles.isEmpty()) {
      return emptyList();
    }
    Collection<QProfileDto> children = selectChildren(dbSession, profiles);
    List<QProfileDto> descendants = new ArrayList<>(children);
    descendants.addAll(selectDescendants(dbSession, children));
    return descendants;
  }

  @CheckForNull
  public QProfileDto selectByNameAndLanguage(DbSession dbSession, OrganizationDto organization, String name, String language) {
    return mapper(dbSession).selectByNameAndLanguage(organization.getUuid(), name, language);
  }

  @CheckForNull
  public QProfileDto selectByRuleProfileUuid(DbSession dbSession, String organizationUuid, String ruleProfileKee) {
    return mapper(dbSession).selectByRuleProfileUuid(organizationUuid, ruleProfileKee);
  }

  public List<QProfileDto> selectByNameAndLanguages(DbSession dbSession, String name, Collection<String> languages) {
    return mapper(dbSession).selectByNameAndLanguages(name, languages);
  }

  public Map<String, Long> countProjectsByOrganizationAndProfiles(DbSession dbSession, OrganizationDto organization, List<QProfileDto> profiles) {
    List<String> profileUuids = profiles.stream().map(QProfileDto::getKee).toList();
    return KeyLongValue.toMap(executeLargeInputs(profileUuids, partition -> mapper(dbSession).countProjectsByOrganizationAndProfiles(organization.getUuid(), partition)));
  }

  public void insertProjectProfileAssociation(DbSession dbSession, ProjectDto project, QProfileDto profile) {
    mapper(dbSession).insertProjectProfileAssociation(uuidFactory.create(), project.getUuid(), profile.getKee());
    logger.info("Attached QProfile to Project :: project: {}, QProfile: {}, QPLanguage: {}", project.getKey(),
            profile.getName(), profile.getLanguage());
  }

  public void deleteProjectProfileAssociation(DbSession dbSession, ProjectDto project, QProfileDto profile) {
    mapper(dbSession).deleteProjectProfileAssociation(project.getUuid(), profile.getKee());
    logger.info("Deleted QProfiles from Project :: project: {}, QProfile: {}, QPLanguage: {}", project.getKey(),
            profile.getName(), profile.getLanguage());
  }

  public void updateProjectProfileAssociation(DbSession dbSession, ProjectDto project, String newProfileUuid, String oldProfileUuid) {
    mapper(dbSession).updateProjectProfileAssociation(project.getUuid(), newProfileUuid, oldProfileUuid);
    logger.info("Updated QProfiles to Project :: project: {}, old_QProfile: {}, new_QProfile: {}", project.getKey(),
            oldProfileUuid, newProfileUuid);
  }

  public void deleteProjectAssociationsByProfileUuids(DbSession dbSession, Collection<String> profileUuids) {
    QualityProfileMapper mapper = mapper(dbSession);
    DatabaseUtils.executeLargeUpdates(profileUuids, mapper::deleteProjectAssociationByProfileUuids);
  }

  public List<ProjectQprofileAssociationDto> selectSelectedProjects(DbSession dbSession, OrganizationDto organization, QProfileDto profile, @Nullable String query) {
    String nameQuery = sqlQueryString(query);
    return mapper(dbSession).selectSelectedProjects(organization != null ? organization.getUuid() : null, profile.getKee(), nameQuery);
  }

  public List<ProjectQprofileAssociationDto> selectDeselectedProjects(DbSession dbSession, OrganizationDto organization, QProfileDto profile, @Nullable String query) {
    String nameQuery = sqlQueryString(query);
    return mapper(dbSession).selectDeselectedProjects(organization != null ? organization.getUuid() : null, profile.getKee(), nameQuery);
  }

  public List<ProjectQprofileAssociationDto> selectProjectAssociations(DbSession dbSession, OrganizationDto organization, QProfileDto profile, @Nullable String query) {
    String nameQuery = sqlQueryString(query);
    return mapper(dbSession).selectProjectAssociations(organization != null ? organization.getUuid() : null, profile.getKee(), nameQuery);
  }

  public List<ProjectQProfileLanguageAssociationDto> selectAllProjectAssociations(DbSession dbSession) {
    return mapper(dbSession).selectAllProjectAssociations();
  }
  public Collection<String> selectUuidsOfCustomRulesProfiles(DbSession dbSession, String language, String name) {
    return mapper(dbSession).selectUuidsOfCustomRuleProfiles(language, name);
  }

  public void renameRulesProfilesAndCommit(DbSession dbSession, Collection<String> rulesProfileUuids, String newName) {
    QualityProfileMapper mapper = mapper(dbSession);
    Date now = new Date(system.now());
    executeLargeUpdates(rulesProfileUuids, partition -> {
      mapper.renameRuleProfiles(newName, now, partition);
      dbSession.commit();
    });
  }

  public void deleteOrgQProfilesByUuids(DbSession dbSession, Collection<String> profileUuids) {
    QualityProfileMapper mapper = mapper(dbSession);
    DatabaseUtils.executeLargeUpdates(profileUuids, mapper::deleteOrgQProfilesByUuids);
  }

  public void deleteRulesProfilesByUuids(DbSession dbSession, Collection<String> rulesProfileUuids) {
    QualityProfileMapper mapper = mapper(dbSession);
    DatabaseUtils.executeLargeUpdates(rulesProfileUuids, mapper::deleteRuleProfilesByUuids);
  }

  public List<QProfileDto> selectQProfilesByRuleProfile(DbSession dbSession, RulesProfileDto rulesProfile) {
    return mapper(dbSession).selectQProfilesByRuleProfileUuid(rulesProfile.getUuid());
  }

  private static String sqlQueryString(@Nullable String query) {
    if (query == null) {
      return "%";
    }
    return "%" + query.toUpperCase(Locale.ENGLISH) + "%";
  }

  private static QualityProfileMapper mapper(DbSession dbSession) {
    return dbSession.getMapper(QualityProfileMapper.class);
  }
}
