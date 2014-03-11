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
package org.sonar.core.persistence.profiling;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Statement;

class ProfilingConnectionHandler implements InvocationHandler {

  private final Connection connection;

  ProfilingConnectionHandler(Connection connection) {
    this.connection = connection;
  }

  @Override
  public Object invoke(Object target, Method method, Object[] args) throws Throwable {
    Object result = InvocationUtils.invokeQuietly(connection, method, args);
    if ("prepareStatement".equals(method.getName())) {
      PreparedStatement statement = (PreparedStatement) result;
      String sql = (String) args[0];
      return Proxy.newProxyInstance(ProfilingConnectionHandler.class.getClassLoader(), new Class[] { PreparedStatement.class },
        new ProfilingPreparedStatementHandler(statement, sql));

    } else if ("createStatement".equals(method.getName())) {
      Statement statement = (Statement) result;
      return Proxy.newProxyInstance(ProfilingConnectionHandler.class.getClassLoader(), new Class[] { Statement.class },
        new ProfilingStatementHandler(statement));

    } else {
      return result;
    }
  }

}
