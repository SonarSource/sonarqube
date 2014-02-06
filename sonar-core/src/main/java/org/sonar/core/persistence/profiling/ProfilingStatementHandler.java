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

import org.sonar.core.profiling.StopWatch;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.sql.Statement;

class ProfilingStatementHandler implements InvocationHandler {

  private static final SqlProfiling PROFILING = new SqlProfiling();
  private final Statement statement;

  ProfilingStatementHandler(Statement statement) {
    this.statement = statement;
  }

  @Override
  public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
    if (method.getName().startsWith("execute")) {
      StopWatch watch = PROFILING.start();
      Object result = method.invoke(statement, args);
      PROFILING.stop(watch, (String) args[0]);
      return result;
    } else {
      return method.invoke(statement, args);
    }
  }
}
