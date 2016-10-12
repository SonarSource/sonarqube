/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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

package org.sonar.server.project.es;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.resources.Scopes;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.ResultSetIterator;

public class ProjectMeasuresResultSetIterator extends ResultSetIterator<ProjectMeasuresDoc> {

  private static final String[] FIELDS = {
    "p.uuid",
    "p.kee",
    "p.name",
    "s.created_at"
  };

  private static final String SQL_ALL = "SELECT " + StringUtils.join(FIELDS, ",") + " FROM projects p " +
    "LEFT OUTER JOIN snapshots s ON s.component_uuid=p.uuid AND s.islast=? " +
    "WHERE p.enabled=? AND p.scope=? AND p.qualifier=?";

  private ProjectMeasuresResultSetIterator(PreparedStatement stmt) throws SQLException {
    super(stmt);
  }

  static ProjectMeasuresResultSetIterator create(DbClient dbClient, DbSession session) {
    try {
      PreparedStatement stmt = dbClient.getMyBatis().newScrollingSelectStatement(session, SQL_ALL);
      stmt.setBoolean(1, true);
      stmt.setBoolean(2, true);
      stmt.setString(3, Scopes.PROJECT);
      stmt.setString(4, Qualifiers.PROJECT);
      return new ProjectMeasuresResultSetIterator(stmt);
    } catch (SQLException e) {
      throw new IllegalStateException("Fail to prepare SQL request to select all project measures", e);
    }
  }

  @Override
  protected ProjectMeasuresDoc read(ResultSet rs) throws SQLException {
    ProjectMeasuresDoc doc = new ProjectMeasuresDoc()
      .setId(rs.getString(1))
      .setKey(rs.getString(2))
      .setName(rs.getString(3));
    long analysisDate = rs.getLong(4);
    doc.setAnalysedAt(analysisDate != 0 ? new Date(analysisDate) : null);
    return doc;
  }
}
