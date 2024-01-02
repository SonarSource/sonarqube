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
package org.sonar.db.alm.setting;

import java.util.List;
import java.util.Set;
import javax.annotation.CheckForNull;
import org.apache.ibatis.annotations.Param;

public interface ProjectAlmSettingMapper {

  @CheckForNull
  ProjectAlmSettingDto selectByProjectUuid(@Param("projectUuid") String projectUuid);

  int countByAlmSettingUuid(@Param("almSettingUuid") String almSettingUuid);

  void insert(@Param("dto") ProjectAlmSettingDto projectAlmSettingDto, @Param("uuid") String uuid, @Param("now") long now);

  int update(@Param("dto") ProjectAlmSettingDto projectAlmSettingDto, @Param("now") long now);

  int deleteByProjectUuid(@Param("projectUuid") String projectUuid);
  void deleteByAlmSettingUuid(@Param("almSettingUuid") String almSettingUuid);

  List<ProjectAlmSettingDto> selectByAlmSettingAndSlugs(@Param("almSettingUuid") String almSettingUuid, @Param("slugs") List<String> slugs);

  List<ProjectAlmSettingDto> selectByAlmSettingAndRepos(@Param("almSettingUuid") String almSettingUuid, @Param("repos") List<String> repos);

  List<ProjectAlmSettingDto> selectByAlm(@Param("alm") String alm);

  List<ProjectAlmSettingDto> selectByProjectUuidsAndAlm(@Param("projectUuids") Set<String> projectUuids, @Param("alm") String alm);

  List<ProjectAlmKeyAndProject> selectAlmTypeAndUrlByProject();
}
