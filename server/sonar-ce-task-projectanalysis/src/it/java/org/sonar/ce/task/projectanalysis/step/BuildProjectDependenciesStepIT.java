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

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sonar.api.utils.System2;
import org.sonar.ce.task.projectanalysis.analysis.MutableAnalysisMetadataHolderRule;
import org.sonar.ce.task.projectanalysis.batch.BatchReportReaderRule;
import org.sonar.ce.task.projectanalysis.component.Component;
import org.sonar.ce.task.projectanalysis.component.DefaultBranchImpl;
import org.sonar.ce.task.projectanalysis.component.MutableTreeRootHolderRule;
import org.sonar.ce.task.projectanalysis.component.ReportComponent;
import org.sonar.ce.task.projectanalysis.dependency.MutableProjectDependenciesHolderRule;
import org.sonar.ce.task.step.TestComputationStepContext;
import org.sonar.db.DbClient;
import org.sonar.db.DbTester;
import org.sonar.scanner.protocol.output.ScannerReport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.db.component.BranchDto.DEFAULT_MAIN_BRANCH_NAME;

class BuildProjectDependenciesStepIT {
  private static final String PROJECT_UUID = "PROJECT";
  private static final String PROJECT_KEY = "PROJECT_KEY";

  @RegisterExtension
  DbTester dbTester = DbTester.create(System2.INSTANCE);
  @RegisterExtension
  BatchReportReaderRule reportReader = new BatchReportReaderRule();
  @RegisterExtension
  MutableTreeRootHolderRule treeRootHolder = new MutableTreeRootHolderRule();
  @RegisterExtension
  MutableAnalysisMetadataHolderRule analysisMetadataHolder = new MutableAnalysisMetadataHolderRule().setBranch(new DefaultBranchImpl(DEFAULT_MAIN_BRANCH_NAME));
  @RegisterExtension
  MutableProjectDependenciesHolderRule dependencyHolder = new MutableProjectDependenciesHolderRule();

  private final DbClient dbClient = dbTester.getDbClient();
  private final BuildProjectDependenciesStep underTest = new BuildProjectDependenciesStep(dbClient, reportReader, dependencyHolder, treeRootHolder, analysisMetadataHolder);

  @BeforeEach
  void setup() {
    initBasicProject();
  }

  @Test
  void buildProjectDependenciesStep_whenNoDependencies_shouldBuildEmptyList() {
    reportReader.putDependencies(List.of());

    TestComputationStepContext context = new TestComputationStepContext();
    underTest.execute(context);

    var dependencies = dependencyHolder.getDependencies();
    assertThat(dependencies).isEmpty();

    context.getStatistics().assertValue("dependencies", 0);
  }

  @Test
  void buildProjectDependenciesStep_shouldBuildDependencyList() {
    var reportDep1 = ScannerReport.Dependency.newBuilder()
      .setKey("mvn+com.google.guava:guava$28.2-jre")
      .setName("guava")
      .setFullName("com.google.guava:guava")
      .setPackageManager("mvn")
      .setDescription("Google Core Libraries for Java")
      .setVersion("28.2-jre")
      .build();
    var reportDep2 = ScannerReport.Dependency.newBuilder()
      .setKey("npm+react$7.0.0")
      .setName("react")
      .build();
    reportReader.putDependencies(List.of(reportDep1, reportDep2));

    TestComputationStepContext context = new TestComputationStepContext();
    underTest.execute(context);

    var dependencies = dependencyHolder.getDependencies();
    assertThat(dependencies).hasSize(2);

    var dependency1 = dependencies.get(0);
    assertThat(dependency1.getKey()).isEqualTo("PROJECT_KEY:mvn+com.google.guava:guava$28.2-jre");
    assertThat(dependency1.getName()).isEqualTo("guava");
    assertThat(dependency1.getFullName()).isEqualTo("com.google.guava:guava");
    assertThat(dependency1.getDescription()).isEqualTo("Google Core Libraries for Java");

    var dependency2 = dependencies.get(1);
    assertThat(dependency2.getKey()).isEqualTo("PROJECT_KEY:npm+react$7.0.0");
    assertThat(dependency2.getName()).isEqualTo("react");
    assertThat(dependency2.getFullName()).isEqualTo("react");
    assertThat(dependency2.getDescription()).isNull();

    context.getStatistics().assertValue("dependencies", 2);
  }

  private void initBasicProject() {
    ReportComponent root = ReportComponent.builder(Component.Type.PROJECT, 1).setUuid(PROJECT_UUID).setKey(PROJECT_KEY).build();
    treeRootHolder.setRoots(root, root);
  }

}
