/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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
package org.sonar.db;

import java.sql.PreparedStatement;
import org.apache.ibatis.session.SqlSessionFactory;

/**
 * Represents a configured and started MyBatis {@link SqlSessionFactory}.
 * In most code, you would use an {@link AbstractDbClient} subtype or {@link DBSessions}
 * instead of using this interface directly. But this interface is implemented by a concrete
 * database configuration to provide the MyBatis session factory to those classes.
 */
public interface MyBatis {
  DbSession openSession(boolean batch);

  /**
   * Create a PreparedStatement for SELECT requests with scrolling of results
   */
  PreparedStatement newScrollingSelectStatement(DbSession session, String sql);
}
