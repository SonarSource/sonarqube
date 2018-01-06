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
package org.sonar.server.computation.task.projectanalysis.metric;

import com.google.common.base.Function;
import com.google.common.collect.FluentIterable;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.core.metric.ScannerMetrics;

public class ReportMetricValidatorImpl implements ReportMetricValidator {

  private static final Logger LOG = Loggers.get(ReportMetricValidatorImpl.class);

  private Map<String, org.sonar.api.measures.Metric> metricByKey;
  private Set<String> alreadyLoggedMetricKeys = new HashSet<>();

  public ReportMetricValidatorImpl(ScannerMetrics scannerMetrics) {
    this.metricByKey = FluentIterable.from(scannerMetrics.getMetrics()).uniqueIndex(MetricToKey.INSTANCE);
  }

  @Override
  public boolean validate(String metricKey) {
    org.sonar.api.measures.Metric metric = metricByKey.get(metricKey);
    if (metric == null) {
      if (!alreadyLoggedMetricKeys.contains(metricKey)) {
        LOG.debug("The metric '{}' is ignored and should not be send in the batch report", metricKey);
        alreadyLoggedMetricKeys.add(metricKey);
      }
      return false;
    }
    return true;
  }

  private enum MetricToKey implements Function<org.sonar.api.measures.Metric, String> {
    INSTANCE;

    @Nullable
    @Override
    public String apply(@Nonnull org.sonar.api.measures.Metric input) {
      return input.key();
    }
  }
}
