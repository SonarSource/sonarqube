/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
package org.sonar.server.platform.db.migration.version.v78;

import org.sonar.server.platform.db.migration.step.MigrationStepRegistry;
import org.sonar.server.platform.db.migration.version.DbVersion;

public class DbVersion78 implements DbVersion {

  @Override
  public void addSteps(MigrationStepRegistry registry) {
    registry
      .add(2700, "Drop overall subscriptions on notifications about new and resolved issues", DeleteOverallSubscriptionsOnNewAndResolvedIssuesNotifications.class)
      .add(2701, "Add index to org_qprofile.parent_uuid", AddIndexToOrgQProfileParentUuid.class)
      .add(2702, "Add column webhooks.secret", AddWebhooksSecret.class)
      .add(2703, "Add security fields to Elasticsearch indices", AddSecurityFieldsToElasticsearchIndices.class)
      .add(2704, "Add InternalComponentProperties table", CreateInternalComponentPropertiesTable.class)
      .add(2705, "Add column snapshots.revision", AddSnapshotRevision.class)
      .add(2706, "Migrate revision from analysis_properties to snapshots.revision", MigrateRevision.class)
      .add(2707, "Update statuses of Security Hotspots", UpdateSecurityHotspotsStatuses.class)
      .add(2708, "Remove orphans from PROJECT_BRANCHES", RemoveOrphansFromProjectBranches.class);
  }
}
