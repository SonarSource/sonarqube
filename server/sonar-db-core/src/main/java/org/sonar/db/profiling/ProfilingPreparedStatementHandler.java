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
import java.sql.PreparedStatement;
import org.sonar.api.utils.log.Profiler;

class ProfilingPreparedStatementHandler implements InvocationHandler {

  private final PreparedStatement statement;
  private final String sql;
  private final Object[] sqlParams;

  ProfilingPreparedStatementHandler(PreparedStatement statement, String sql) {
    this.statement = statement;
    this.sql = sql;
    sqlParams = new Object[SqlLogFormatter.countArguments(sql)];
  }

  @Override
  public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
    if (method.getName().startsWith("execute")) {
      Profiler profiler = Profiler.create(ProfiledDataSource.SQL_LOGGER).start();
      Object result = null;
      try {
        result = InvocationUtils.invokeQuietly(statement, method, args);
      } finally {
        profiler.addContext("sql", SqlLogFormatter.formatSql(sql));
        if (sqlParams.length > 0) {
          profiler.addContext("params", SqlLogFormatter.formatParams(sqlParams));
        }
        profiler.stopTrace("");
      }
      return result;
    } else if (method.getName().startsWith("set") && args.length > 1) {
      sqlParams[(int) args[0] - 1] = args[1];
      return InvocationUtils.invokeQuietly(statement, method, args);
    } else {
      return InvocationUtils.invokeQuietly(statement, method, args);
    }
  }

}
