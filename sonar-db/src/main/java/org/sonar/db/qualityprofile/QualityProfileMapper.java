/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package org.sonar.db.qualityprofile;

import java.util.List;
import javax.annotation.CheckForNull;
import org.apache.ibatis.annotations.Param;
import org.sonar.db.component.ComponentDto;

public interface QualityProfileMapper {

  void insert(QualityProfileDto dto);

  void update(QualityProfileDto dto);

  void delete(int id);

  List<QualityProfileDto> selectAll();

  @CheckForNull
  QualityProfileDto selectDefaultProfile(@Param("language") String language);

  @CheckForNull
  QualityProfileDto selectByNameAndLanguage(@Param("name") String name, @Param("language") String language);

  @CheckForNull
  QualityProfileDto selectById(@Param("id") Integer id);

  @CheckForNull
  QualityProfileDto selectByKey(String key);

  List<QualityProfileDto> selectByLanguage(String language);

  // INHERITANCE

  @CheckForNull
  QualityProfileDto selectParent(String childKey);

  @CheckForNull
  QualityProfileDto selectParentById(int childId);

  List<QualityProfileDto> selectChildren(String key);

  // PROJECTS

  List<ComponentDto> selectProjects(@Param("profileName") String profileName, @Param("language") String language);

  int countProjects(@Param("profileName") String profileName, @Param("language") String language);

  List<QualityProfileProjectCount> countProjectsByProfile();

  QualityProfileDto selectByProjectIdAndLanguage(@Param("projectId") Long projectId, @Param("language") String language);

  QualityProfileDto selectByProjectAndLanguage(@Param("projectKey") String projectKey, @Param("language") String language);

  void insertProjectProfileAssociation(@Param("projectUuid") String projectUuid, @Param("profileKey") String profileKey);

  void updateProjectProfileAssociation(@Param("projectUuid") String projectUuid, @Param("profileKey") String profileKey);

  void deleteProjectProfileAssociation(@Param("projectUuid") String projectUuid, @Param("profileKey") String profileKey);

  void deleteAllProjectProfileAssociation(@Param("profileKey") String profileKey);

  List<ProjectQprofileAssociationDto> selectSelectedProjects(@Param("profileKey") String profileKey, @Param("nameQuery") String nameQuery);

  List<ProjectQprofileAssociationDto> selectDeselectedProjects(@Param("profileKey") String profileKey, @Param("nameQuery") String nameQuery);

  List<ProjectQprofileAssociationDto> selectProjectAssociations(@Param("profileKey") String profileKey, @Param("nameQuery") String nameQuery);
}
