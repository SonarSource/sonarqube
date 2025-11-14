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
package org.sonar.db.qualityprofile;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import javax.annotation.CheckForNull;
import org.apache.ibatis.annotations.Param;
import org.sonar.db.KeyLongValue;

public interface QualityProfileMapper {

  void insertOrgQProfile(@Param("dto") OrgQProfileDto dto, @Param("now") long now);

  void insertRuleProfile(@Param("dto") RulesProfileDto dto, @Param("now") Date now);

  void updateRuleProfile(@Param("dto") RulesProfileDto dto, @Param("now") Date now);

  void updateOrgQProfile(@Param("dto") OrgQProfileDto dto, @Param("now") long now);

  void deleteRuleProfilesByUuids(@Param("uuids") Collection<String> uuids);

  void deleteOrgQProfilesByUuids(@Param("uuids") Collection<String> uuids);

  List<RulesProfileDto> selectBuiltInRuleProfiles();

  List<RulesProfileDto> selectBuiltInRuleProfilesWithActiveRules();

  @CheckForNull
  RulesProfileDto selectRuleProfile(@Param("uuid") String ruleProfileUuid);

  List<QProfileDto> selectAll();

  @CheckForNull
  QProfileDto selectDefaultProfile(@Param("language") String language);

  List<QProfileDto> selectDefaultProfilesWithoutActiveRules(@Param("languages") List<String> languages, @Param("builtIn") boolean builtIn);

  List<QProfileDto> selectDefaultProfiles(
    @Param("languages") Collection<String> languages);

  List<QProfileDto> selectAllDefaultProfiles();

  @CheckForNull
  String selectDefaultProfileUuid(@Param("language") String language);

  @CheckForNull
  QProfileDto selectByNameAndLanguage(
    @Param("name") String name,
    @Param("language") String language);

  @CheckForNull
  QProfileDto selectByRuleProfileUuid(@Param("ruleProfileUuid") String ruleProfileKee);

  List<QProfileDto> selectByNameAndLanguages(@Param("name") String name, @Param("languages") Collection<String> languages);

  @CheckForNull
  QProfileDto selectByUuid(String uuid);

  List<QProfileDto> selectByUuids(@Param("uuids") Collection<String> uuids);

  List<QProfileDto> selectByLanguage(@Param("language") String language);

  // INHERITANCE

  List<QProfileDto> selectChildren(@Param("uuids") Collection<String> uuids);

  // PROJECTS

  List<KeyLongValue> countProjectsByProfiles(@Param("profileUuids") List<String> profiles);

  @CheckForNull
  QProfileDto selectAssociatedToProjectUuidAndLanguage(
    @Param("projectUuid") String projectUuid,
    @Param("language") String language);

  List<QProfileDto> selectAssociatedToProjectUuidAndLanguages(
    @Param("projectUuid") String projectUuid,
    @Param("languages") Collection<String> languages);

  List<QProfileDto> selectQProfilesByProjectUuid(@Param("projectUuid") String projectUuid);

  void insertProjectProfileAssociation(
    @Param("uuid") String uuid,
    @Param("projectUuid") String projectUuid,
    @Param("profileUuid") String profileUuid);

  void updateProjectProfileAssociation(
    @Param("projectUuid") String projectUuid,
    @Param("profileUuid") String profileUuid,
    @Param("oldProfileUuid") String oldProfileUuid);

  void deleteProjectProfileAssociation(@Param("projectUuid") String projectUuid, @Param("profileUuid") String profileUuid);

  void deleteProjectAssociationByProfileUuids(@Param("profileUuids") Collection<String> profileUuids);

  List<ProjectQprofileAssociationDto> selectSelectedProjects(
    @Param("profileUuid") String profileUuid,
    @Param("nameOrKeyQuery") String nameOrKeyQuery);

  List<ProjectQprofileAssociationDto> selectDeselectedProjects(
    @Param("profileUuid") String profileUuid,
    @Param("nameOrKeyQuery") String nameOrKeyQuery);

  List<ProjectQprofileAssociationDto> selectProjectAssociations(
    @Param("profileUuid") String profileUuid,
    @Param("nameOrKeyQuery") String nameOrKeyQuery);

  List<ProjectQProfileLanguageAssociationDto> selectAllProjectAssociations();

  List<String> selectUuidsOfCustomRuleProfiles(@Param("language") String language, @Param("name") String name);

  void renameRuleProfiles(@Param("newName") String newName, @Param("updatedAt") Date updatedAt, @Param("uuids") Collection<String> uuids);

  List<QProfileDto> selectQProfilesByRuleProfileUuid(@Param("rulesProfileUuid") String rulesProfileUuid);

  int updateLastUsedDate(
    @Param("uuid") String uuid,
    @Param("lastUsedDate") long lastUsedDate,
    @Param("now") long now);
}
