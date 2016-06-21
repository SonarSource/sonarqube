/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package org.sonar.server.computation.step;

import com.google.common.collect.Iterables;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonar.core.component.ComponentKeys;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.SnapshotDto;
import org.sonar.db.component.SnapshotQuery;
import org.sonar.scanner.protocol.output.ScannerReport;
import org.sonar.server.computation.analysis.MutableAnalysisMetadataHolder;
import org.sonar.server.computation.batch.BatchReportReader;
import org.sonar.server.computation.component.Component;
import org.sonar.server.computation.component.ComponentImpl;
import org.sonar.server.computation.component.MutableTreeRootHolder;
import org.sonar.server.computation.component.UuidFactory;
import org.sonar.server.computation.snapshot.Snapshot;

import static com.google.common.collect.Iterables.toArray;
import static org.sonar.server.computation.component.ComponentImpl.builder;

/**
 * Populates the {@link MutableTreeRootHolder} and {@link MutableAnalysisMetadataHolder} from the {@link BatchReportReader}
 */
public class BuildComponentTreeStep implements ComputationStep {

  private final DbClient dbClient;
  private final BatchReportReader reportReader;
  private final MutableTreeRootHolder treeRootHolder;
  private final MutableAnalysisMetadataHolder analysisMetadataHolder;

  public BuildComponentTreeStep(DbClient dbClient, BatchReportReader reportReader, MutableTreeRootHolder treeRootHolder, MutableAnalysisMetadataHolder analysisMetadataHolder) {
    this.dbClient = dbClient;
    this.reportReader = reportReader;
    this.treeRootHolder = treeRootHolder;
    this.analysisMetadataHolder = analysisMetadataHolder;
  }

  @Override
  public void execute() {
    String branch = analysisMetadataHolder.getBranch();
    ScannerReport.Component reportProject = reportReader.readComponent(analysisMetadataHolder.getRootComponentRef());
    UuidFactory uuidFactory = new UuidFactory(dbClient, moduleKey(reportProject, branch));
    Component project = new ComponentRootBuilder(reportProject, uuidFactory, branch).build();
    treeRootHolder.setRoot(project);
    setBaseProjectSnapshot(project.getUuid());
  }

  private void setBaseProjectSnapshot(String projectUuid) {
    DbSession dbSession = dbClient.openSession(false);
    try {
      SnapshotDto snapshotDto = dbClient.snapshotDao().selectSnapshotByQuery(dbSession,
        new SnapshotQuery()
          .setComponentUuid(projectUuid)
          .setIsLast(true));
      analysisMetadataHolder.setBaseProjectSnapshot(toSnapshot(snapshotDto));
    } finally {
      dbClient.closeSession(dbSession);
    }
  }

  @CheckForNull
  private static Snapshot toSnapshot(@Nullable SnapshotDto snapshotDto) {
    return snapshotDto == null ? null : new Snapshot.Builder()
      .setId(snapshotDto.getId())
      .setUuid(snapshotDto.getUuid())
      .setCreatedAt(snapshotDto.getCreatedAt())
      .build();
  }

  private class ComponentRootBuilder {

    private final ScannerReport.Component reportProject;

    private final UuidFactory uuidFactory;

    @CheckForNull
    private final String branch;

    public ComponentRootBuilder(ScannerReport.Component reportProject, UuidFactory uuidFactory, @Nullable String branch) {
      this.reportProject = reportProject;
      this.uuidFactory = uuidFactory;
      this.branch = branch;
    }

    private Component build() {
      return buildComponent(reportProject, moduleKey(reportProject, branch));
    }

    private ComponentImpl buildComponent(ScannerReport.Component reportComponent, String latestModuleKey) {
      switch (reportComponent.getType()) {
        case PROJECT:
        case MODULE:
          String moduleKey = moduleKey(reportComponent, branch);
          return buildComponent(reportComponent, moduleKey, moduleKey);
        case DIRECTORY:
        case FILE:
          return buildComponent(reportComponent, ComponentKeys.createEffectiveKey(latestModuleKey, reportComponent.getPath()), latestModuleKey);
        default:
          throw new IllegalStateException(String.format("Unsupported component type '%s'", reportComponent.getType()));
      }
    }

    private ComponentImpl buildComponent(ScannerReport.Component reportComponent, String componentKey, String latestModuleKey) {
      return builder(reportComponent)
        .addChildren(toArray(buildChildren(reportComponent, latestModuleKey), Component.class))
        .setKey(componentKey)
        .setUuid(uuidFactory.getOrCreateForKey(componentKey))
        .build();
    }

    private Iterable<Component> buildChildren(ScannerReport.Component component, final String latestModuleKey) {
      return Iterables.transform(
        component.getChildRefList(),
        componentRef -> buildComponent(reportReader.readComponent(componentRef), latestModuleKey));
    }
  }

  private static String moduleKey(ScannerReport.Component reportComponent, @Nullable String branch) {
    return ComponentKeys.createKey(reportComponent.getKey(), branch);
  }

  @Override
  public String getDescription() {
    return "Build tree of components";
  }
}
