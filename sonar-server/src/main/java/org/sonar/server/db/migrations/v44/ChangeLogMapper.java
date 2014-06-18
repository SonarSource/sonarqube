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
package org.sonar.server.db.migrations.v44;

import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * SONAR-5329
 * Transition ActiveRuleChanges to ActivityLog
 * <p/>
 * Used in the Active Record Migration 548.
 *
 * @since 4.4
 */
public interface ChangeLogMapper {

  @Select("select rule_change.change_date as createdAt," +
    "  rule_change.username as userLogin " +
    "from active_rule_changes rule_change")
  List<ChangeLog> selectAll();
}

