/*
 * SonarQube
 * Copyright (C) SonarSource Sàrl
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
package org.sonar.server.monitoring;

import org.apache.ibatis.cache.CacheKey;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Signature;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import org.sonar.api.config.Configuration;
import org.sonar.process.ProcessProperties;

@Intercepts({
  @Signature(type = Executor.class, method = "update", args = {MappedStatement.class, Object.class}),
  @Signature(type = Executor.class, method = "query", args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class}),
  @Signature(type = Executor.class, method = "query", args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class, CacheKey.class, BoundSql.class})
})
public class MyBatisMetricsInterceptor implements Interceptor {

  private final ServerMonitoringMetrics metrics;
  private final Configuration config;

  public MyBatisMetricsInterceptor(ServerMonitoringMetrics metrics, Configuration config) {
    this.metrics = metrics;
    this.config = config;
  }

  @Override
  public Object intercept(Invocation invocation) throws Throwable {
    if (!isEnabled()) {
      return invocation.proceed();
    }
    long startNanos = System.nanoTime();
    try {
      return invocation.proceed();
    } finally {
      Object[] args = invocation.getArgs();
      if (args.length > 0 && args[0] instanceof MappedStatement mappedStatement) {
        double durationSeconds = (System.nanoTime() - startNanos) / 1_000_000_000.0;
        metrics.observeDbQueryDuration(durationSeconds, shortMapperMethod(mappedStatement.getId()));
      }
    }
  }

  private boolean isEnabled() {
    return config.getBoolean(ProcessProperties.Property.PERFORMANCE_MONITORING_ENABLED.getKey()).orElse(false);
  }

  static String shortMapperMethod(String fullyQualifiedId) {
    int lastDot = fullyQualifiedId.lastIndexOf('.');
    if (lastDot < 0) {
      return fullyQualifiedId;
    }
    int secondLastDot = fullyQualifiedId.lastIndexOf('.', lastDot - 1);
    return fullyQualifiedId.substring(secondLastDot + 1);
  }
}
