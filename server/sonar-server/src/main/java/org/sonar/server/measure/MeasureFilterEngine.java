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
package org.sonar.server.measure;

import com.google.common.base.Joiner;
import org.sonar.api.ServerSide;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.api.utils.log.Profiler;

import javax.annotation.Nullable;

import java.util.List;
import java.util.Map;

@ServerSide
public class MeasureFilterEngine {

  private static final Logger LOG = Loggers.get("MeasureFilter");

  private final MeasureFilterFactory factory;
  private final MeasureFilterExecutor executor;

  public MeasureFilterEngine(MeasureFilterFactory factory, MeasureFilterExecutor executor) {
    this.executor = executor;
    this.factory = factory;
  }

  public MeasureFilterResult execute(Map<String, Object> filterMap, @Nullable Long userId) {
    Profiler profiler = Profiler.createIfDebug(LOG).start();
    MeasureFilterResult result = new MeasureFilterResult();
    MeasureFilterContext context = new MeasureFilterContext();
    context.setUserId(userId);
    context.setData(String.format("{%s}", Joiner.on('|').withKeyValueSeparator("=").join(filterMap)));
    try {
      profiler.addContext("request", context.getData());
      MeasureFilter filter = factory.create(filterMap);
      List<MeasureFilterRow> rows = executor.execute(filter, context);
      result.setRows(rows);

    } catch (NumberFormatException e) {
      result.setError(MeasureFilterResult.Error.VALUE_SHOULD_BE_A_NUMBER);
      LOG.debug("Value selected for the metric should be a number: " + context);
    } catch (Exception e) {
      result.setError(MeasureFilterResult.Error.UNKNOWN);
      LOG.error("Fail to execute measure filter: " + context, e);
    } finally {
      profiler.addContext("result", result.toString());
      profiler.stopDebug("Measure filter executed");
    }
    return result;
  }
}
