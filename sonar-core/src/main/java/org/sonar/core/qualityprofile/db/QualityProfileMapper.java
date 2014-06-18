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

package org.sonar.core.qualityprofile.db;

import org.apache.ibatis.annotations.Param;
import org.sonar.core.component.ComponentDto;

import javax.annotation.CheckForNull;

import java.util.List;

public interface QualityProfileMapper {

  void insert(QualityProfileDto dto);

  void update(QualityProfileDto dto);

  void delete(Integer id);

  List<QualityProfileDto> selectAll();

  @CheckForNull
  QualityProfileDto selectDefaultProfile(@Param("language") String language, @Param("propKey") String propKey);

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

  List<ComponentDto> selectProjects(@Param("value") String propertyValue, @Param("key") String propertyKey);

  int countProjects(@Param("value") String propertyValue, @Param("key") String propertyKey);

  QualityProfileDto selectByProjectAndLanguage(@Param("projectId") Long projectId, @Param("language") String language, @Param("key") String propertyKeyPrefix);
}
