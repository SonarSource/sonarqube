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
package org.sonar.ce.task.projectanalysis.source;

import java.util.HashMap;
import java.util.Map;
import javax.annotation.CheckForNull;
import org.sonar.ce.task.projectanalysis.analysis.AnalysisMetadataHolder;
import org.sonar.ce.task.projectanalysis.component.BranchComponentUuidsDelegate;
import org.sonar.ce.task.projectanalysis.component.Component;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.source.LineHashVersion;

public class DbLineHashVersion {
  private final Map<Component, LineHashVersion> lineHashVersionPerComponent = new HashMap<>();
  private final DbClient dbClient;
  private final AnalysisMetadataHolder analysisMetadataHolder;
  private final BranchComponentUuidsDelegate branchComponentUuidsDelegate;

  public DbLineHashVersion(DbClient dbClient, AnalysisMetadataHolder analysisMetadataHolder, BranchComponentUuidsDelegate branchComponentUuidsDelegate) {
    this.dbClient = dbClient;
    this.analysisMetadataHolder = analysisMetadataHolder;
    this.branchComponentUuidsDelegate = branchComponentUuidsDelegate;
  }

  /**
   * Reads from DB the version of line hashes for a component and returns whether it was generated taking into account the ranges of significant code.
   * The response is cached.
   * Returns false if the component is not in the DB.
   */
  public boolean hasLineHashesWithSignificantCode(Component component) {
    return lineHashVersionPerComponent.computeIfAbsent(component, this::compute) == LineHashVersion.WITH_SIGNIFICANT_CODE;
  }

  /**
   * Reads from DB the version of line hashes for a component and returns whether it was generated taking into account the ranges of significant code.
   * The response is cached.
   * Returns false if the component is not in the DB.
   */
  public boolean hasLineHashesWithoutSignificantCode(Component component) {
    return lineHashVersionPerComponent.computeIfAbsent(component, this::compute) == LineHashVersion.WITHOUT_SIGNIFICANT_CODE;
  }

  @CheckForNull
  private LineHashVersion compute(Component component) {
    try (DbSession session = dbClient.openSession(false)) {
      String referenceComponentUuid = getReferenceComponentUuid(component);
      if (referenceComponentUuid != null) {
        return dbClient.fileSourceDao().selectLineHashesVersion(session, referenceComponentUuid);
      } else {
        return null;
      }
    }
  }

  @CheckForNull
  private String getReferenceComponentUuid(Component component) {
    if (analysisMetadataHolder.isPullRequest()) {
      return branchComponentUuidsDelegate.getComponentUuid(component.getKey());
    } else {
      return component.getUuid();
    }
  }
}
