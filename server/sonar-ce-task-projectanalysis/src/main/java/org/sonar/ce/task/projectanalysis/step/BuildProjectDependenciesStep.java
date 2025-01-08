/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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
package org.sonar.ce.task.projectanalysis.step;

import java.util.ArrayList;
import java.util.List;
import java.util.function.UnaryOperator;
import org.sonar.ce.task.projectanalysis.analysis.AnalysisMetadataHolder;
import org.sonar.ce.task.projectanalysis.batch.BatchReportReader;
import org.sonar.ce.task.projectanalysis.component.ComponentKeyGenerator;
import org.sonar.ce.task.projectanalysis.component.ComponentUuidFactory;
import org.sonar.ce.task.projectanalysis.component.ComponentUuidFactoryImpl;
import org.sonar.ce.task.projectanalysis.component.TreeRootHolder;
import org.sonar.ce.task.projectanalysis.dependency.MutableProjectDependenciesHolder;
import org.sonar.ce.task.projectanalysis.dependency.ProjectDependency;
import org.sonar.ce.task.projectanalysis.dependency.ProjectDependencyImpl;
import org.sonar.ce.task.step.ComputationStep;
import org.sonar.core.util.CloseableIterator;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.scanner.protocol.output.ScannerReport;

import static org.apache.commons.lang3.StringUtils.trimToNull;

/**
 * Populates the {@link MutableProjectDependenciesHolder} from the {@link BatchReportReader}
 */
public class BuildProjectDependenciesStep implements ComputationStep {

  private final DbClient dbClient;
  private final BatchReportReader reportReader;
  private final MutableProjectDependenciesHolder projectDependenciesHolder;
  private final TreeRootHolder treeRootHolder;
  private final AnalysisMetadataHolder analysisMetadataHolder;

  public BuildProjectDependenciesStep(DbClient dbClient, BatchReportReader reportReader, MutableProjectDependenciesHolder projectDependenciesHolder, TreeRootHolder treeRootHolder,
    AnalysisMetadataHolder analysisMetadataHolder) {
    this.dbClient = dbClient;
    this.reportReader = reportReader;
    this.projectDependenciesHolder = projectDependenciesHolder;
    this.treeRootHolder = treeRootHolder;
    this.analysisMetadataHolder = analysisMetadataHolder;
  }

  @Override
  public String getDescription() {
    return "Build list of dependencies";
  }

  @Override
  public void execute(Context context) {
    try (DbSession dbSession = dbClient.openSession(false)) {
      ComponentKeyGenerator keyGenerator = loadKeyGenerator();

      // root key of branch, not necessarily of project
      String rootKey = treeRootHolder.getRoot().getKey();
      // loads the UUIDs from database. If they don't exist, then generate new ones
      ComponentUuidFactory componentUuidFactory = new ComponentUuidFactoryImpl(dbClient, dbSession, rootKey, analysisMetadataHolder.getBranch());

      List<ProjectDependency> dependencies = new ArrayList<>();
      try (CloseableIterator<ScannerReport.Dependency> reportDependencies = reportReader.readDependencies()) {
        while (reportDependencies.hasNext()) {
          ScannerReport.Dependency reportDependency = reportDependencies.next();
          ProjectDependency dependency = buildDependency(reportDependency, componentUuidFactory::getOrCreateForKey, keyGenerator, rootKey);
          dependencies.add(dependency);
        }
      }

      projectDependenciesHolder.setDependencies(dependencies);

      context.getStatistics().add("dependencies", projectDependenciesHolder.getSize());
    }
  }

  private static ProjectDependency buildDependency(ScannerReport.Dependency reportDependency, UnaryOperator<String> uuidSupplier, ComponentKeyGenerator keyGenerator,
    String rootKey) {
    String dependencyKey = keyGenerator.generateKey(rootKey, reportDependency.getKey());
    String uuid = uuidSupplier.apply(dependencyKey);
    ProjectDependencyImpl.Builder builder = ProjectDependencyImpl.builder()
      .setUuid(uuid)
      .setKey(dependencyKey)
      .setFullName(reportDependency.hasFullName() ? reportDependency.getFullName() : reportDependency.getName())
      .setName(reportDependency.getName())
      .setDescription(reportDependency.hasDescription() ? trimToNull(reportDependency.getDescription()) : null)
      .setVersion(reportDependency.hasVersion() ? trimToNull(reportDependency.getVersion()) : null)
      .setPackageManager(reportDependency.hasPackageManager() ? trimToNull(reportDependency.getPackageManager()): null);
    return builder.build();
  }

  private ComponentKeyGenerator loadKeyGenerator() {
    return analysisMetadataHolder.getBranch();
  }

}
