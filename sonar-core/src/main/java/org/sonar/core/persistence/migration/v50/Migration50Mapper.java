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

package org.sonar.core.persistence.migration.v50;

import org.apache.ibatis.annotations.*;
import org.apache.ibatis.mapping.ResultSetType;

import java.util.List;

public interface Migration50Mapper {

  /**
   * Return root projects (Views and Developers are NOT returned)
   */
  @Select("SELECT " +
    "  p.id AS \"id\", " +
    "  p.uuid AS \"uuid\", " +
    "  p.project_uuid AS \"projectUuid\", " +
    "  s.root_project_id AS \"projectId\", " +
    "  s.id AS \"snapshotId\", " +
    "  s.path AS \"snapshotPath\", " +
    "  p.scope AS \"scope\" " +
    "FROM projects p " +
    "  LEFT OUTER JOIN snapshots s ON s.project_id = p.id AND s.islast = ${_true} " +
    "  WHERE " +
    "   p.scope = 'PRJ' " +
    "   AND p.root_id IS NULL ")
  @Result(javaType = Component.class)
  @Options(resultSetType = ResultSetType.FORWARD_ONLY, fetchSize = 200)
  List<Component> selectRootProjects();

  @Select("SELECT " +
    "  p.id AS \"id\", " +
    "  p.uuid AS \"uuid\", " +
    "  p.project_uuid AS \"projectUuid\", " +
    "  p.module_uuid AS \"moduleUuid\", " +
    "  p.module_uuid_path AS \"moduleUuidPath\", " +
    "  s.root_project_id AS \"projectId\", " +
    "  s.id AS \"snapshotId\", " +
    "  s.path AS \"snapshotPath\", " +
    "  p.scope AS \"scope\" " +
    "FROM projects root " +
    "  INNER JOIN snapshots root_snapshot ON root_snapshot.project_id = root.id AND root_snapshot.islast = ${_true} " +
    "  INNER JOIN snapshots s ON s.root_snapshot_id = root_snapshot.id AND s.islast = ${_true} " +
    "  INNER JOIN projects p ON p.id = s.project_id " +
    "  WHERE root.id = #{id} ")
  @Result(javaType = Component.class)
  List<Component> selectComponentChildrenForProjects(@Param("id") Long projectId);

  /**
   * Return disabled direct children from a project (1st level modules, files on single module project)
   * For migration re-entrance, ignore components already having UUID
   */
  @Select("SELECT " +
    "  p.id AS \"id\", " +
    "  p.uuid AS \"uuid\" " +
    "FROM projects p " +
    "  INNER JOIN projects root ON root.id = p.root_id " +
    "  WHERE root.id = #{id} " +
    "   AND p.uuid IS NULL " +
    "   AND p.enabled=${_false} ")
  @Result(javaType = Component.class)
  List<Component> selectDisabledDirectComponentChildrenForProjects(@Param("id") Long projectId);

  /**
   * Return disabled none direct children (2nd level modules and more, files on modules, etc.)
   * For migration re-entrance, ignore components already having UUID
   */
  @Select("SELECT " +
    "  p.id AS \"id\", " +
    "  p.uuid AS \"uuid\" " +
    "FROM projects p " +
    "  INNER JOIN projects root_one ON root_one.id = p.root_id " +
    "  INNER JOIN projects root_two ON root_two.id = root_one.root_id " +
    "  WHERE root_two.id=#{id} " +
    "   AND p.uuid IS NULL " +
    "   AND p.enabled=${_false} ")
  @Result(javaType = Component.class)
  List<Component> selectDisabledNoneDirectComponentChildrenForProjects(@Param("id") Long projectId);

  /**
   * Return not migrated components
   */
  @Select("SELECT " +
    "  p.id AS \"id\" " +
    "FROM projects p " +
    "  WHERE p.uuid IS NULL ")
  @Result(javaType = Component.class)
  List<Component> selectComponentsWithoutUuid();

  @Select("SELECT " +
    "  p.id AS \"id\", " +
    "  p.uuid AS \"uuid\", " +
    "  p.project_uuid AS \"projectUuid\", " +
    "  p.module_uuid AS \"moduleUuid\", " +
    "  p.module_uuid_path AS \"moduleUuidPath\" " +
    "FROM projects p " +
    "  WHERE p.kee = #{key}")
  @Result(javaType = Component.class)
  Component selectComponentByKey(@Param("key") String key);

  @Update("UPDATE projects " +
    " SET uuid=#{uuid}, project_uuid=#{projectUuid}, module_uuid=#{moduleUuid}, module_uuid_path=#{moduleUuidPath} " +
    " WHERE id=#{id}")
  @Options(useGeneratedKeys = false)
  void updateComponentUuids(Component component);
}
