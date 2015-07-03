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
package org.sonar.db.profiling;

import com.google.common.collect.Lists;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.sql.PreparedStatement;
import java.util.List;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.utils.log.Profiler;

class ProfilingPreparedStatementHandler implements InvocationHandler {

  private final PreparedStatement statement;
  private final List<Object> arguments;
  private final String sql;

  ProfilingPreparedStatementHandler(PreparedStatement statement, String sql) {
    this.statement = statement;
    this.sql = sql;
    this.arguments = Lists.newArrayList();
    for (int argCount = 0; argCount < StringUtils.countMatches(sql, "?"); argCount++) {
      arguments.add("!");
    }
  }

  @Override
  public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
    if (method.getName().startsWith("execute")) {
      Profiler profiler = Profiler.create(ProfiledDataSource.SQL_LOGGER).start();
      Object result = null;
      try {
        result = InvocationUtils.invokeQuietly(statement, method, args);
      } finally {
        profiler.addContext("sql", StringUtils.remove(sql, '\n'));
        profiler.stopTrace("");
      }
      return result;
    } else if (method.getName().startsWith("set") && args.length > 1) {
      arguments.set((Integer) args[0] - 1, args[1]);
      return InvocationUtils.invokeQuietly(statement, method, args);
    } else {
      return InvocationUtils.invokeQuietly(statement, method, args);
    }
  }

}
