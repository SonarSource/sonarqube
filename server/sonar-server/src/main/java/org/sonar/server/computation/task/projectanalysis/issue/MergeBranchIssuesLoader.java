/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
package org.sonar.server.computation.task.projectanalysis.issue;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.sonar.core.issue.DefaultIssue;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.ComponentDto;
import org.sonar.server.computation.task.projectanalysis.analysis.AnalysisMetadataHolder;

public class MergeBranchIssuesLoader {
  private final DbClient dbClient;
  private final ComponentIssuesLoader issuesLoader;
  private final AnalysisMetadataHolder analysisMetadataHolder;
  private Map<String, String> uuidsByKey;

  public MergeBranchIssuesLoader(DbClient dbClient, ComponentIssuesLoader issuesLoader, AnalysisMetadataHolder analysisMetadataHolder) {
    this.dbClient = dbClient;
    this.issuesLoader = issuesLoader;
    this.analysisMetadataHolder = analysisMetadataHolder;
  }

  public void loadMergeBranchComponents() {
    String mergeBranchUuid = analysisMetadataHolder.getBranch().get().getMergeBranchUuid().get();

    uuidsByKey = new HashMap<>();
    try (DbSession dbSession = dbClient.openSession(false)) {

      List<ComponentDto> components = dbClient.componentDao().selectByProjectUuid(mergeBranchUuid, dbSession);
      for (ComponentDto dto : components) {
        uuidsByKey.put(dto.getDbKey(), dto.uuid());
      }
    }
  }

  public List<DefaultIssue> loadForKey(String componentKey) {
    if (uuidsByKey == null) {
      loadMergeBranchComponents();
    }

    String componentUuid = uuidsByKey.get(componentKey);

    if (componentUuid == null) {
      return Collections.emptyList();
    }

    return issuesLoader.loadForComponentUuid(componentUuid);
  }
}
