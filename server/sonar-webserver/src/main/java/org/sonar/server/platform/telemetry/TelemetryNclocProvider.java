/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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
import org.sonar.telemetry.legacy.ProjectLocDistributionDto;
import org.sonar.telemetry.core.AbstractTelemetryDataProvider;
import org.sonar.telemetry.core.Dimension;
import org.sonar.telemetry.core.Granularity;
import org.sonar.telemetry.core.TelemetryDataType;
import org.sonar.telemetry.legacy.ProjectLocDistributionDataProvider;

public class TelemetryNclocProvider extends AbstractTelemetryDataProvider<Long> {

  public static final String METRIC_KEY = "ncloc_per_language";

  private final DbClient dbClient;
  private final ProjectLocDistributionDataProvider projectLocDistributionDataProvider;

  public TelemetryNclocProvider(DbClient dbClient, ProjectLocDistributionDataProvider projectLocDistributionDataProvider) {
    super(METRIC_KEY, Dimension.LANGUAGE, Granularity.DAILY, TelemetryDataType.INTEGER);
    this.dbClient = dbClient;
    this.projectLocDistributionDataProvider = projectLocDistributionDataProvider;
  }

  @Override
  public Map<String, Long> getValues() {
    try (DbSession dbSession = dbClient.openSession(false)) {
      return getNclocDistribution(dbSession);
    }
  }

  private Map<String, Long> getNclocDistribution(DbSession dbSession) {
    List<ProjectLocDistributionDto> branchesWithLargestNcloc = projectLocDistributionDataProvider.getProjectLocDistribution(dbSession);
    List<LanguageDistribution> languageDistributions = getLanguageDistributionList(branchesWithLargestNcloc);
    return getNclocDistributionPerLanguage(languageDistributions);
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
