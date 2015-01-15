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
package org.sonar.batch.sensor.coverage;

import com.google.common.collect.ImmutableList;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Metric;

import java.util.Collection;

public class CoverageConstants {

  private CoverageConstants() {
  }

  public static final Collection<Metric> COVERAGE_METRICS = ImmutableList.<Metric>of(CoreMetrics.LINES_TO_COVER, CoreMetrics.UNCOVERED_LINES, CoreMetrics.NEW_LINES_TO_COVER,
    CoreMetrics.NEW_UNCOVERED_LINES, CoreMetrics.CONDITIONS_TO_COVER, CoreMetrics.UNCOVERED_CONDITIONS,
    CoreMetrics.NEW_CONDITIONS_TO_COVER, CoreMetrics.NEW_UNCOVERED_CONDITIONS);

  public static final Collection<Metric> LINE_COVERAGE_METRICS = ImmutableList.<Metric>of(CoreMetrics.UNCOVERED_LINES, CoreMetrics.LINES_TO_COVER, CoreMetrics.NEW_UNCOVERED_LINES,
    CoreMetrics.NEW_LINES_TO_COVER);

  public static final Collection<Metric> BRANCH_COVERAGE_METRICS = ImmutableList.<Metric>of(CoreMetrics.UNCOVERED_CONDITIONS, CoreMetrics.CONDITIONS_TO_COVER,
    CoreMetrics.NEW_UNCOVERED_CONDITIONS, CoreMetrics.NEW_CONDITIONS_TO_COVER);
}
