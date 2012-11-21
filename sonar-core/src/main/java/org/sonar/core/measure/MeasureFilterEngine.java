/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.core.measure;

import com.google.common.annotations.VisibleForTesting;
import org.apache.commons.lang.SystemUtils;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.ServerComponent;

import javax.annotation.Nullable;

import java.util.List;
import java.util.Map;

public class MeasureFilterEngine implements ServerComponent {
  private static final Logger FILTER_LOG = LoggerFactory.getLogger("org.sonar.MEASURE_FILTER");

  private final MeasureFilterDecoder decoder;
  private final MeasureFilterExecutor executor;

  public MeasureFilterEngine(MeasureFilterDecoder decoder, MeasureFilterExecutor executor) {
    this.decoder = decoder;
    this.executor = executor;
  }

  public List<MeasureFilterRow> execute(String filterJson, @Nullable Long userId) throws ParseException {
    return execute(filterJson, userId, FILTER_LOG);
  }

  public List<MeasureFilterRow> execute2(Map<String, String> filterMap, @Nullable Long userId) throws ParseException {
    Logger logger = FILTER_LOG;
    MeasureFilterContext context = new MeasureFilterContext();
    context.setJson(filterMap.toString());
    context.setUserId(userId);
    try {
      long start = System.currentTimeMillis();
      MeasureFilter filter = MeasureFilter.create(filterMap);
      List<MeasureFilterRow> rows = executor.execute(filter, context);
      log(context, rows, (System.currentTimeMillis() - start), logger);
      return rows;
    } catch (Exception e) {
      throw new IllegalStateException("Fail to execute filter: " + context, e);
    }
  }

  @VisibleForTesting
  List<MeasureFilterRow> execute(String filterJson, @Nullable Long userId, Logger logger) {
    MeasureFilterContext context = new MeasureFilterContext();
    context.setJson(filterJson);
    context.setUserId(userId);
    try {
      long start = System.currentTimeMillis();
      MeasureFilter filter = decoder.decode(filterJson);
      List<MeasureFilterRow> rows = executor.execute(filter, context);
      log(context, rows, (System.currentTimeMillis() - start), logger);
      return rows;
    } catch (Exception e) {
      throw new IllegalStateException("Fail to execute filter: " + context, e);
    }
  }

  private void log(MeasureFilterContext context, List<MeasureFilterRow> rows, long durationMs, Logger logger) {
    if (logger.isDebugEnabled()) {
      StringBuilder log = new StringBuilder();
      log.append(SystemUtils.LINE_SEPARATOR);
      log.append(" filter: ").append(context.getJson()).append(SystemUtils.LINE_SEPARATOR);
      log.append("    sql: ").append(context.getSql()).append(SystemUtils.LINE_SEPARATOR);
      log.append("results: ").append(rows.size()).append(" rows in ").append(durationMs).append("ms").append(SystemUtils.LINE_SEPARATOR);
      logger.debug(log.toString());
    }
  }

}
