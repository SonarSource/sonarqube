/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
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
package org.sonar.core.resource;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.session.ResultHandler;

import javax.annotation.Nullable;

import java.util.Collection;
import java.util.List;

public interface ResourceMapper {
  SnapshotDto selectSnapshot(Long snapshotId);

  SnapshotDto selectLastSnapshotByResourceKey(String resourceKey);

  SnapshotDto selectLastSnapshotByResourceId(long resourceId);

  ResourceDto selectResource(long id);

  List<ResourceDto> selectDescendantProjects(long rootProjectId);

  /**
   * @since 3.0
   */
  List<ResourceDto> selectResources(ResourceQuery query);

  /**
   * @since 3.0
   */
  List<Long> selectResourceIds(ResourceQuery query);

  /**
   * @since 3.2
   */
  void selectResources(ResourceQuery query, ResultHandler resultHandler);

  /**
   * @since3.6
   */
  List<ResourceDto> selectResourcesById(@Param("ids") List<Long> ids);

  /**
   * @since 3.6
   */
  ResourceDto selectRootProjectByComponentKey(@Param("componentKey") String componentKey);

  /**
   * @since 3.6
   */
  ResourceDto selectRootProjectByComponentId(@Param("componentId") Long componentId);

  /**
   * @since 3.6
   */
  List<Integer> selectAuthorizedChildrenComponentIds(@Param("componentRootKeys") Collection<String> componentRootKeys,
                                                     @Param("userId") @Nullable Integer userId, @Param("role") String role);

  List<ResourceDto> selectComponentsByQualifiers(@Param("qualifiers") Collection<String> qualifier);

  List<ResourceDto> selectComponentsIncludingNotCompletedOnesByQualifiers(@Param("qualifiers") Collection<String> qualifier);

  void insert(ResourceDto resource);

  void update(ResourceDto resource);
}
