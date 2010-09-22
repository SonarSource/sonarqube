/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2009 SonarSource SA
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.jpa.entity;

import javax.persistence.*;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

@Entity
@Table(name = SchemaMigration.TABLE_NAME, uniqueConstraints = {@UniqueConstraint(columnNames = {"version"})})
public class SchemaMigration {

  public final static int VERSION_UNKNOWN = -1;
  public static final int LAST_VERSION = 141;

  public final static String TABLE_NAME = "schema_migrations";

  @Id
  @Column(name = "version", updatable = true)
  private String version;

  public String getVersion() {
    return version;
  }

  public void setVersion(String s) {
    this.version = s;
  }

  public void setVersion(int i) {
    this.version = String.valueOf(i);
  }

  public static int getCurrentVersion(Connection connection) {
    Statement stmt = null;
    ResultSet rs = null;
    int version = VERSION_UNKNOWN;
    try {
      stmt = connection.createStatement();
      rs = stmt.executeQuery("SELECT version FROM " + SchemaMigration.TABLE_NAME);
      while (rs.next()) {
        int i = Integer.parseInt(rs.getString(1));
        if (i > version) {
          version = i;
        }
      }
    } catch (SQLException e) {
      // ignore
    } finally {
      close(rs);
      close(stmt);
    }

    return version;
  }

  private static void close(ResultSet rs) {
    if (rs != null) {
      try {
        rs.close();
      } catch (SQLException e) {
        // why does close() throw a checked-exception ???
      }
    }
  }

  private static void close(Statement st) {
    if (st != null) {
      try {
        st.close();
      } catch (SQLException e) {
        // why does close() throw a checked-exception ???
      }
    }
  }
}