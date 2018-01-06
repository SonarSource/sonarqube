/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
package org.sonar.db.profiling;

import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.SQLException;
import org.apache.commons.dbcp.BasicDataSource;

public enum ProfiledConnectionInterceptor implements ConnectionInterceptor {
  INSTANCE;

  @Override
  public Connection getConnection(BasicDataSource dataSource) throws SQLException {
    return buildConnectionProxy(new ProfilingConnectionHandler(dataSource.getConnection()));
  }

  @Override
  public Connection getConnection(BasicDataSource dataSource, String login, String password) throws SQLException {
    return buildConnectionProxy(new ProfilingConnectionHandler(dataSource.getConnection(login, password)));
  }

  private static Connection buildConnectionProxy(ProfilingConnectionHandler connectionHandler) {
    ClassLoader classloader = ProfiledConnectionInterceptor.class.getClassLoader();
    return (Connection) Proxy.newProxyInstance(classloader, new Class[] {Connection.class}, connectionHandler);
  }

}
