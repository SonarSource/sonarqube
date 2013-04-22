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
package org.sonar.core.measure;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Joiner;
import org.apache.commons.lang.SystemUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.ServerComponent;

import javax.annotation.Nullable;

import java.util.List;
import java.util.Map;

public class MeasureFilterEngine implements ServerComponent {

  private final MeasureFilterFactory factory;
  private final MeasureFilterExecutor executor;


  public MeasureFilterEngine(MeasureFilterFactory factory, MeasureFilterExecutor executor) {
    this.executor = executor;
    this.factory = factory;
  }

  public MeasureFilterResult execute(Map<String, Object> filterMap, @Nullable Long userId) {
    return execute(filterMap, userId, LoggerFactory.getLogger("org.sonar.MEASURE_FILTER"));
  }

  @VisibleForTesting
  MeasureFilterResult execute(Map<String, Object> filterMap, @Nullable Long userId, Logger logger) {
    long start = System.currentTimeMillis();
    MeasureFilterResult result = new MeasureFilterResult();
    MeasureFilterContext context = new MeasureFilterContext();
    context.setUserId(userId);
    context.setData(String.format("{%s}", Joiner.on('|').withKeyValueSeparator("=").join(filterMap)));
    try {
      MeasureFilter filter = factory.create(filterMap);
      List<MeasureFilterRow> rows = executor.execute(filter, context);
      result.setRows(rows);
      log(context, result, logger);

    } catch (Exception e) {
      result.setError(MeasureFilterResult.Error.UNKNOWN);
      logger.error("Fail to execute measure filter: " + context, e);
    } finally {
      result.setDurationInMs(System.currentTimeMillis() - start);
    }
    return result;
  }

  private void log(MeasureFilterContext context, MeasureFilterResult result, Logger logger) {
    if (logger.isDebugEnabled()) {
      StringBuilder log = new StringBuilder();
      log.append(SystemUtils.LINE_SEPARATOR);
      log.append("request: ").append(context.getData()).append(SystemUtils.LINE_SEPARATOR);
      log.append(" result: ").append(result.toString()).append(SystemUtils.LINE_SEPARATOR);
      log.append("    sql: ").append(context.getSql()).append(SystemUtils.LINE_SEPARATOR);
      logger.debug(log.toString());
    }
  }

}
