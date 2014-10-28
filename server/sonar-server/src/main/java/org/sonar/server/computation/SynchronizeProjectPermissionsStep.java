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

package org.sonar.server.computation;

import org.sonar.core.computation.db.AnalysisReportDto;
import org.sonar.core.persistence.DbSession;
import org.sonar.server.issue.index.IssueAuthorizationIndex;
import org.sonar.server.permission.InternalPermissionService;
import org.sonar.server.search.IndexClient;

public class SynchronizeProjectPermissionsStep implements ComputationStep {

  private final IndexClient index;
  private final InternalPermissionService permissionService;

  public SynchronizeProjectPermissionsStep(IndexClient index, InternalPermissionService permissionService) {
    this.index = index;
    this.permissionService = permissionService;
  }

  @Override
  public void execute(DbSession session, AnalysisReportDto report) {
    synchronizeProjectPermissionsIfNotFound(session, report);
  }

  @Override
  public String description() {
    return "Synchronize project permissions";
  }

  private void synchronizeProjectPermissionsIfNotFound(DbSession session, AnalysisReportDto report) {
    if (index.get(IssueAuthorizationIndex.class).getNullableByKey(report.getProjectKey()) == null) {
      permissionService.synchronizePermissions(session, report.getProject().uuid());
      session.commit();
    }
  }
}
