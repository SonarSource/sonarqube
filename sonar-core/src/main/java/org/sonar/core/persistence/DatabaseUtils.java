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
package org.sonar.core.persistence;

import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * @since 2.13
 */
public final class DatabaseUtils {
  private DatabaseUtils() {
  }

  /**
   * List of all the tables.
   * This list is hardcoded because we didn't succeed in using java.sql.DatabaseMetaData#getTables() in the same way
   * for all the supported databases, particularly due to Oracle results.
   */
  static final String[] TABLE_NAMES = {
    "action_plans",
    "active_dashboards",
    "active_rules",
    "active_rule_changes",
    "active_rule_parameters",
    "active_rule_param_changes",
    "alerts",
    "authors",
    "characteristics",
    "characteristic_edges",
    "characteristic_properties",
    "dashboards",
    "dependencies",
    "duplications_index",
    "events",
    "graphs",
    "groups",
    "groups_users",
    "group_roles",
    "issues",
    "issue_changes",
    "issue_filters",
    "issue_filter_favourites",
    "loaded_templates",
    "manual_measures",
    "measure_data",
    "measure_filters",
    "measure_filter_favourites",
    "metrics",
    "notifications",
    "projects",
    "project_links",
    "project_measures",
    "properties",
    "quality_models",
    "resource_index",
    "rules",
    "rules_parameters",
    "rules_profiles",
    "semaphores",
    "schema_migrations",
    "snapshots",
    "snapshot_sources",
    "snapshot_data",
    "users",
    "user_roles",
    "widgets",
    "widget_properties"};

  public static void closeQuietly(@Nullable Connection connection) {
    if (connection != null) {
      try {
        connection.close();
      } catch (SQLException e) {
        LoggerFactory.getLogger(DatabaseUtils.class).warn("Fail to close connection", e);
        // ignore
      }
    }
  }

  public static void closeQuietly(@Nullable Statement stmt) {
    if (stmt != null) {
      try {
        stmt.close();
      } catch (SQLException e) {
        LoggerFactory.getLogger(DatabaseUtils.class).warn("Fail to close statement", e);
        // ignore
      }
    }
  }

  public static void closeQuietly(@Nullable ResultSet rs) {
    if (rs != null) {
      try {
        rs.close();
      } catch (SQLException e) {
        LoggerFactory.getLogger(DatabaseUtils.class).warn("Fail to close result set", e);
        // ignore
      }
    }
  }
}
