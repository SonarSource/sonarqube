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
package org.sonar.server.platform.telemetry;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.measure.ProjectLocDistributionDto;
import org.sonar.db.metric.MetricDto;
import org.sonar.telemetry.core.Dimension;
import org.sonar.telemetry.core.Granularity;
import org.sonar.telemetry.core.TelemetryDataProvider;
import org.sonar.telemetry.core.TelemetryDataType;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toMap;
import static org.sonar.api.measures.CoreMetrics.NCLOC_KEY;
import static org.sonar.api.measures.CoreMetrics.NCLOC_LANGUAGE_DISTRIBUTION_KEY;

public class TelemetryNclocProvider implements TelemetryDataProvider<Long> {

  public static final String METRIC_KEY = "ncloc_per_language";

  private final DbClient dbClient;

  public TelemetryNclocProvider(DbClient dbClient) {
    this.dbClient = dbClient;
  }

  @Override
  public String getMetricKey() {
    return METRIC_KEY;
  }

  @Override
  public Dimension getDimension() {
    return Dimension.LANGUAGE;
  }

  @Override
  public Granularity getGranularity() {
    return Granularity.DAILY;
  }

  @Override
  public TelemetryDataType getType() {
    return TelemetryDataType.INTEGER;
  }

  @Override
  public Map<String, Long> getValues() {
    try (DbSession dbSession = dbClient.openSession(false)) {
      return getNclocDistribution(dbSession);
    }
  }

  private Map<String, Long> getNclocDistribution(DbSession dbSession) {
    Map<String, String> metricUuidMap = getNclocMetricUuidMap(dbSession);
    String nclocUuid = metricUuidMap.get(NCLOC_KEY);
    String nclocDistributionUuid = metricUuidMap.get(NCLOC_LANGUAGE_DISTRIBUTION_KEY);
    List<ProjectLocDistributionDto> branchesWithLargestNcloc = dbClient.liveMeasureDao().selectLargestBranchesLocDistribution(dbSession, nclocUuid, nclocDistributionUuid);
    List<LanguageDistribution> languageDistributions = getLanguageDistributionList(branchesWithLargestNcloc);
    return getNclocDistributionPerLanguage(languageDistributions);
  }

  private Map<String, String> getNclocMetricUuidMap(DbSession dbSession) {
    return dbClient.metricDao().selectByKeys(dbSession, asList(NCLOC_KEY, NCLOC_LANGUAGE_DISTRIBUTION_KEY))
      .stream()
      .collect(toMap(MetricDto::getKey, MetricDto::getUuid));
  }

  private static List<LanguageDistribution> getLanguageDistributionList(List<ProjectLocDistributionDto> branchesWithLargestNcloc) {
    return branchesWithLargestNcloc.stream()
      .flatMap(measure -> Arrays.stream(measure.locDistribution().split(";"))
        .map(languageAndLoc -> languageAndLoc.split("="))
        .map(languageAndLoc -> new LanguageDistribution(
          languageAndLoc[0],
          Long.parseLong(languageAndLoc[1]))))
      .toList();
  }

  private static Map<String, Long> getNclocDistributionPerLanguage(List<LanguageDistribution> languageDistributions) {
    // a Map<String, Integer> that contains the sum of ncloc per language
    Map<String, Long> nclocPerLanguage = new HashMap<>();
    languageDistributions.forEach(languageDistribution -> nclocPerLanguage.merge(languageDistribution.language, languageDistribution.ncloc, Long::sum));
    return nclocPerLanguage;
  }

  private record LanguageDistribution(String language, long ncloc) {
  }
}
