/*
 * SonarQube
 * Copyright (C) 2009-2020 SonarSource SA
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
package org.sonar.server.platform.db.migration.version.v82;

import org.sonar.server.platform.db.migration.step.MigrationStepRegistry;
import org.sonar.server.platform.db.migration.version.DbVersion;

public class DbVersion82 implements DbVersion {
  @Override
  public void addSteps(MigrationStepRegistry registry) {
    registry
      .add(3200, "Drop 'In Review' Security Hotspots status ", DropSecurityHotSpotsInReviewStatus.class)
      .add(3201, "Migrate Manual Vulnerabilities to Security Hotspots ", MigrateManualVulnerabilitiesToSecurityHotSpots.class)
      .add(3202, "Remove 'newsbox.dismiss.hotspots' user property", RemoveNewsboxDismissHotspotsProperty.class)
      .add(3203, "Ensure Security Hotspots have status TO_REVIEW", EnsureHotspotDefaultStatusIsToReview.class)
      .add(3204, "Rename table 'PROJECTS' to 'COMPONENTS'", RenameProjectsTableToComponents.class)
      .add(3205, "Add PROJECTS table", CreateProjectsTable.class)
      .add(3206, "Populate PROJECTS table", PopulateProjectsTable.class)
      .add(3207, "Drop 'TAGS' column from COMPONENTS table", DropTagsColumnFromComponentsTable.class)
      .add(3208, "Remove old Security Review Rating measures", DeleteSecurityReviewRatingMeasures.class)
      .add(3209, "Create ALM_PATS table", CreateAlmPatsTable.class)
      .add(3210, "Add index on ALM_slug", AddIndexOnSlugOfProjectAlmSettings.class)
      .add(3211, "Delete conditions using 'security_hotspots' and 'new_security_hotspots' metrics", DeleteQgateConditionsUsingSecurityHotspotMetrics.class);
  }
}
