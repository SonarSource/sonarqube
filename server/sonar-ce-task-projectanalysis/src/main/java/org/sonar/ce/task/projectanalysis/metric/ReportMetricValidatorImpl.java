/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
package org.sonar.ce.task.projectanalysis.metric;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.measures.Metric;
import org.sonar.core.metric.ScannerMetrics;

public class ReportMetricValidatorImpl implements ReportMetricValidator {
  private static final Logger LOG = LoggerFactory.getLogger(ReportMetricValidatorImpl.class);

  private final Map<String, Metric> metricByKey;
  private final Set<String> alreadyLoggedMetricKeys = new HashSet<>();

  public ReportMetricValidatorImpl(ScannerMetrics scannerMetrics) {
    this.metricByKey = scannerMetrics.getMetrics().stream().collect(Collectors.toMap(Metric::getKey, Function.identity()));
  }

  @Override
  public boolean validate(String metricKey) {
    Metric metric = metricByKey.get(metricKey);
    if (metric == null) {
      if (!alreadyLoggedMetricKeys.contains(metricKey)) {
        LOG.debug("The metric '{}' is ignored and should not be send in the batch report", metricKey);
        alreadyLoggedMetricKeys.add(metricKey);
      }
      return false;
    }
    return true;
  }
}
