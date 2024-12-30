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
package org.sonar.server.platform.db.migration.version.v108;

import java.sql.SQLException;
import java.util.Set;
import java.util.stream.Collectors;
import org.sonar.core.metric.SoftwareQualitiesMetrics;
import org.sonar.db.Database;
import org.sonar.server.platform.db.migration.es.MigrationEsClient;
import org.sonar.server.platform.db.migration.step.DataChange;
import org.sonar.server.platform.db.migration.step.MassUpdate;

public class DeleteSoftwareQualityRatingFromProjectMeasures extends DataChange {

  public static final Set<String> SOFTWARE_QUALITY_METRICS_TO_DELETE = Set.of(
    SoftwareQualitiesMetrics.SOFTWARE_QUALITY_MAINTAINABILITY_RATING_KEY,
    SoftwareQualitiesMetrics.NEW_SOFTWARE_QUALITY_MAINTAINABILITY_RATING_KEY,
    SoftwareQualitiesMetrics.SOFTWARE_QUALITY_RELIABILITY_RATING_KEY,
    SoftwareQualitiesMetrics.NEW_SOFTWARE_QUALITY_RELIABILITY_RATING_KEY,
    SoftwareQualitiesMetrics.SOFTWARE_QUALITY_SECURITY_RATING_KEY,
    SoftwareQualitiesMetrics.NEW_SOFTWARE_QUALITY_SECURITY_RATING_KEY,
    SoftwareQualitiesMetrics.EFFORT_TO_REACH_SOFTWARE_QUALITY_MAINTAINABILITY_RATING_A_KEY,
    SoftwareQualitiesMetrics.SOFTWARE_QUALITY_MAINTAINABILITY_REMEDIATION_EFFORT_KEY,
    SoftwareQualitiesMetrics.NEW_SOFTWARE_QUALITY_MAINTAINABILITY_REMEDIATION_EFFORT_KEY,
    SoftwareQualitiesMetrics.SOFTWARE_QUALITY_SECURITY_REMEDIATION_EFFORT_KEY,
    SoftwareQualitiesMetrics.NEW_SOFTWARE_QUALITY_SECURITY_REMEDIATION_EFFORT_KEY,
    SoftwareQualitiesMetrics.SOFTWARE_QUALITY_RELIABILITY_REMEDIATION_EFFORT_KEY,
    SoftwareQualitiesMetrics.NEW_SOFTWARE_QUALITY_RELIABILITY_REMEDIATION_EFFORT_KEY,
    SoftwareQualitiesMetrics.SOFTWARE_QUALITY_MAINTAINABILITY_DEBT_RATIO_KEY,
    SoftwareQualitiesMetrics.NEW_SOFTWARE_QUALITY_MAINTAINABILITY_DEBT_RATIO_KEY,

    // Portfolios
    "software_quality_reliability_rating_distribution",
    "software_quality_security_rating_distribution",
    "software_quality_maintainability_rating_distribution",
    "new_software_quality_maintainability_rating_distribution",
    "new_software_quality_reliability_rating_distribution",
    "new_software_quality_security_rating_distribution",

    // Views
    "last_change_on_software_quality_security_rating",
    "last_change_on_software_quality_reliability_rating",
    "last_change_on_software_quality_maintainability_rating",
    "software_quality_security_rating_effort",
    "software_quality_reliability_rating_effort",
    "software_quality_maintainability_rating_effort");

  private static final String SELECT_QUERY = """
    select pm.uuid from project_measures pm
      inner join metrics m on pm.metric_uuid = m.uuid
     where m.name in (%s)
    """.formatted(SOFTWARE_QUALITY_METRICS_TO_DELETE.stream().map(s -> "'" + s + "'").collect(Collectors.joining(",")));

  private final MigrationEsClient migrationEsClient;

  public DeleteSoftwareQualityRatingFromProjectMeasures(Database db, MigrationEsClient migrationEsClient) {
    super(db);
    this.migrationEsClient = migrationEsClient;
  }

  @Override
  protected void execute(Context context) throws SQLException {
    MassUpdate massUpdate = context.prepareMassUpdate();
    massUpdate.select(SELECT_QUERY);
    massUpdate.update("delete from project_measures where uuid = ?");

    massUpdate.execute((row, update, index) -> {
      update.setString(1, row.getString(1));
      return true;
    });

    // Reindexation of project measures is required to align with removed values
    migrationEsClient.deleteIndexes("projectmeasures");
  }
}
