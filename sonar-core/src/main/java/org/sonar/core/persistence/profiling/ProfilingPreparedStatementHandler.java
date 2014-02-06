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
package org.sonar.core.persistence.profiling;

import org.apache.commons.lang.StringUtils;
import com.google.common.collect.Lists;
import org.sonar.core.profiling.StopWatch;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.sql.PreparedStatement;
import java.util.List;

class ProfilingPreparedStatementHandler implements InvocationHandler {

  private static final String PARAM_PREFIX = "<";
  private static final String PARAM_SUFFIX = ">";
  private static final String PARAM_SEPARATOR = ", ";
  private static final SqlProfiling PROFILING = new SqlProfiling();

  private final PreparedStatement statement;
  private final List<Object> arguments;
  private final String sql;

  ProfilingPreparedStatementHandler(PreparedStatement statement, String sql) {
    this.statement = statement;
    this.sql = sql;
    this.arguments = Lists.newArrayList();
    for (int argCount = 0; argCount < StringUtils.countMatches(sql, "?"); argCount ++) {
      arguments.add("!");
    }
  }

  @Override
  public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
    if (method.getName().startsWith("execute")) {
      StopWatch watch = PROFILING.start();
      Object result = method.invoke(statement, args);
      StringBuilder sqlBuilder = new StringBuilder().append(sql);
      if (!arguments.isEmpty()) {
        sqlBuilder.append(" - parameters are: ");
        for (Object arg: arguments) {
          sqlBuilder.append(PARAM_PREFIX).append(arg).append(PARAM_SUFFIX).append(PARAM_SEPARATOR);
        }
      }
      PROFILING.stop(watch, StringUtils.removeEnd(sqlBuilder.toString(), PARAM_SEPARATOR));
      return result;
    } else if (method.getName().startsWith("set") && args.length > 1) {
      arguments.set((Integer) args[0] - 1, args[1]);
      return method.invoke(statement, args);
    } else {
      return method.invoke(statement, args);
    }
  }

}
