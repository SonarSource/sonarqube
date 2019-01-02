/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
package org.sonar.db.alm;

import java.util.List;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.apache.ibatis.annotations.Param;

public interface ProjectAlmBindingMapper {

  int bindingCount(@Param("almId") String almId, @Param("repoId") String repoId);

  void insert(@Param("uuid") String uuid, @Param("almId") String almId, @Param("repoId") String repoId, @Param("projectUuid") String projectUuid,
    @Nullable @Param("githubSlug") String githubSlug, @Param("url") String url, @Param("now") long now);

  int update(@Param("almId") String almId, @Param("repoId") String repoId, @Param("projectUuid") String projectUuid,
    @Nullable @Param("githubSlug") String githubSlug, @Param("url") String url, @Param("now") long now);

  List<ProjectAlmBindingDto> selectByRepoIds(@Param("almId") String almId, @Param("repoIds") List<String> repoIds);

  ProjectAlmBindingDto selectByRepoId(@Param("almId") String almId, @Param("repoId") String repoId);

  ProjectAlmBindingDto selectByProjectUuid(@Param("projectUuid") String projectUuid);

  @CheckForNull
  String selectProjectKey(@Param("almId") String almId, @Param("repoId") String repoId);
}
