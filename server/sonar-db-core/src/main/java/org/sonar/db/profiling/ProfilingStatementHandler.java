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

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.sql.Statement;
import org.sonar.api.utils.log.Profiler;

class ProfilingStatementHandler implements InvocationHandler {

  private final Statement statement;

  ProfilingStatementHandler(Statement statement) {
    this.statement = statement;
  }

  @Override
  public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
    if (method.getName().startsWith("execute")) {
      Profiler profiler = Profiler.create(ProfiledDataSource.SQL_LOGGER).start();
      Object result = null;
      try {
        result = InvocationUtils.invokeQuietly(statement, method, args);
      } finally {
        String sql = (String) args[0];
        profiler.addContext("sql", SqlLogFormatter.formatSql(sql));
        profiler.stopTrace("");
      }
      return result;
    } else {
      return InvocationUtils.invokeQuietly(statement, method, args);
    }
  }
}
