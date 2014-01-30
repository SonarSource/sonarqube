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

import com.google.common.base.Joiner;
import org.apache.commons.lang.SystemUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.ServerComponent;
import org.sonar.core.profiling.Profiling;
import org.sonar.core.profiling.Profiling.Level;
import org.sonar.core.profiling.StopWatch;

import javax.annotation.Nullable;

import java.util.List;
import java.util.Map;

public class MeasureFilterEngine implements ServerComponent {

  private static final Logger LOG = LoggerFactory.getLogger("org.sonar.MEASURE_FILTER");

  private final MeasureFilterFactory factory;
  private final MeasureFilterExecutor executor;
  private final Profiling profiling;

  public MeasureFilterEngine(MeasureFilterFactory factory, MeasureFilterExecutor executor, Profiling profiling) {
    this.executor = executor;
    this.factory = factory;
    this.profiling = profiling;
  }

  public MeasureFilterResult execute(Map<String, Object> filterMap, @Nullable Long userId) {
    StopWatch watch = profiling.start("measures", Level.BASIC);
    StopWatch sqlWatch = null;
    MeasureFilterResult result = new MeasureFilterResult();
    MeasureFilterContext context = new MeasureFilterContext();
    context.setUserId(userId);
    context.setData(String.format("{%s}", Joiner.on('|').withKeyValueSeparator("=").join(filterMap)));
    try {
      MeasureFilter filter = factory.create(filterMap);
      sqlWatch = profiling.start("sql", Level.FULL);
      List<MeasureFilterRow> rows = executor.execute(filter, context);
      result.setRows(rows);

    } catch (NumberFormatException e) {
      result.setError(MeasureFilterResult.Error.VALUE_SHOULD_BE_A_NUMBER);
      LOG.error("Value selected for the metric should be a number: " + context);
    } catch (Exception e) {
      result.setError(MeasureFilterResult.Error.UNKNOWN);
      LOG.error("Fail to execute measure filter: " + context, e);
    } finally {
      if (sqlWatch != null) {
        sqlWatch.stop(context.getSql());
      }
      watch.stop(log(context, result));
    }
    return result;
  }

  private String log(MeasureFilterContext context, MeasureFilterResult result) {
    StringBuilder log = new StringBuilder();
    log.append(SystemUtils.LINE_SEPARATOR);
    log.append("request: ").append(context.getData()).append(SystemUtils.LINE_SEPARATOR);
    log.append(" result: ").append(result.toString());
    return log.toString();
  }

}
