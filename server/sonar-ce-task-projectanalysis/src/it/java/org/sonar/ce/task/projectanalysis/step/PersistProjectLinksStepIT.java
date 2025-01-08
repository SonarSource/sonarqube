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

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mockito;
import org.sonar.api.utils.System2;
import org.sonar.ce.task.projectanalysis.analysis.AnalysisMetadataHolderRule;
import org.sonar.ce.task.projectanalysis.analysis.Branch;
import org.sonar.ce.task.projectanalysis.batch.BatchReportReader;
import org.sonar.ce.task.projectanalysis.batch.BatchReportReaderRule;
import org.sonar.ce.task.projectanalysis.component.Component;
import org.sonar.ce.task.projectanalysis.component.ReportComponent;
import org.sonar.ce.task.projectanalysis.component.TreeRootHolder;
import org.sonar.ce.task.projectanalysis.component.TreeRootHolderRule;
import org.sonar.ce.task.step.ComputationStep;
import org.sonar.ce.task.step.TestComputationStepContext;
import org.sonar.core.util.UuidFactory;
import org.sonar.core.util.UuidFactoryFast;
import org.sonar.db.DbClient;
import org.sonar.db.DbTester;
import org.sonar.db.component.ProjectData;
import org.sonar.db.component.ProjectLinkDto;
import org.sonar.scanner.protocol.output.ScannerReport;
import org.sonar.scanner.protocol.output.ScannerReport.Component.ComponentType;
import org.sonar.server.project.Project;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.sonar.scanner.protocol.output.ScannerReport.ComponentLink.ComponentLinkType.CI;
import static org.sonar.scanner.protocol.output.ScannerReport.ComponentLink.ComponentLinkType.HOME;
import static org.sonar.scanner.protocol.output.ScannerReport.ComponentLink.ComponentLinkType.ISSUE;
import static org.sonar.scanner.protocol.output.ScannerReport.ComponentLink.ComponentLinkType.SCM;

public class PersistProjectLinksStepIT extends BaseStepTest {

  @Rule
  public AnalysisMetadataHolderRule analysisMetadataHolder = new AnalysisMetadataHolderRule();
  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);
  @Rule
  public TreeRootHolderRule treeRootHolder = new TreeRootHolderRule();
  @Rule
  public BatchReportReaderRule reportReader = new BatchReportReaderRule();

  private ProjectData project;

  private PersistProjectLinksStep underTest = new PersistProjectLinksStep(analysisMetadataHolder, db.getDbClient(), treeRootHolder, reportReader, UuidFactoryFast.getInstance());

  @Override
  protected ComputationStep step() {
    return underTest;
  }

  @Before
  public void setup(){
    this.project = db.components().insertPrivateProject();
    analysisMetadataHolder.setProject(Project.fromProjectDtoWithTags(project.getProjectDto()));
  }

  @Test
  public void no_effect_if_branch_is_not_main() {
    DbClient dbClient = mock(DbClient.class);
    TreeRootHolder treeRootHolder = mock(TreeRootHolder.class);
    BatchReportReader reportReader = mock(BatchReportReader.class);
    UuidFactory uuidFactory = mock(UuidFactory.class);
    mockBranch(false);
    PersistProjectLinksStep underTest = new PersistProjectLinksStep(analysisMetadataHolder, dbClient, treeRootHolder, reportReader, uuidFactory);

    underTest.execute(new TestComputationStepContext());

    verifyNoInteractions(uuidFactory, reportReader, treeRootHolder, dbClient);
  }

  @Test
  public void add_links_on_project() {
    mockBranch(true);
    treeRootHolder.setRoot(ReportComponent.builder(Component.Type.PROJECT, 1).setUuid(project.getMainBranchComponent().uuid()).build());
    // project
    reportReader.putComponent(ScannerReport.Component.newBuilder()
      .setRef(1)
      .setType(ComponentType.PROJECT)
      .addChildRef(2)
      .addLink(ScannerReport.ComponentLink.newBuilder().setType(HOME).setHref("https://www.sonarsource.com/products/sonarqube").build())
      .addLink(ScannerReport.ComponentLink.newBuilder().setType(SCM).setHref("https://github.com/SonarSource/sonar").build())
      .addLink(ScannerReport.ComponentLink.newBuilder().setType(ISSUE).setHref("http://jira.sonarsource.com/").build())
      .addLink(ScannerReport.ComponentLink.newBuilder().setType(CI).setHref("http://bamboo.ci.codehaus.org/browse/SONAR").build())
      .build());

    underTest.execute(new TestComputationStepContext());

    assertThat(db.getDbClient().projectLinkDao().selectByProjectUuid(db.getSession(), project.projectUuid()))
      .extracting(ProjectLinkDto::getType, ProjectLinkDto::getHref, ProjectLinkDto::getName)
      .containsExactlyInAnyOrder(
        tuple("homepage", "https://www.sonarsource.com/products/sonarqube", null),
        tuple("scm", "https://github.com/SonarSource/sonar", null),
        tuple("issue", "http://jira.sonarsource.com/", null),
        tuple("ci", "http://bamboo.ci.codehaus.org/browse/SONAR", null));
  }

  @Test
  public void nothing_to_do_when_link_already_exists() {
    mockBranch(true);
    db.projectLinks().insertProvidedLink(project.getProjectDto(), l -> l.setType("homepage").setName("Home").setHref("https://www.sonarsource.com/products/sonarqube"));

    treeRootHolder.setRoot(ReportComponent.builder(Component.Type.PROJECT, 1).setUuid(project.getMainBranchComponent().uuid()).build());

    reportReader.putComponent(ScannerReport.Component.newBuilder()
      .setRef(1)
      .setType(ComponentType.PROJECT)
      .addLink(ScannerReport.ComponentLink.newBuilder().setType(HOME).setHref("https://www.sonarsource.com/products/sonarqube").build())
      .build());

    underTest.execute(new TestComputationStepContext());

    assertThat(db.getDbClient().projectLinkDao().selectByProjectUuid(db.getSession(), project.projectUuid()))
      .extracting(ProjectLinkDto::getType, ProjectLinkDto::getHref)
      .containsExactlyInAnyOrder(tuple("homepage", "https://www.sonarsource.com/products/sonarqube"));
  }

  @Test
  public void do_not_add_links_on_module() {
    mockBranch(true);
    treeRootHolder.setRoot(ReportComponent.builder(Component.Type.PROJECT, 1).setUuid(project.getMainBranchComponent().uuid()).build());
    reportReader.putComponent(ScannerReport.Component.newBuilder()
      .setRef(1)
      .setType(ComponentType.PROJECT)
      .addChildRef(2)
      .build());
    reportReader.putComponent(ScannerReport.Component.newBuilder()
      .setRef(2)
      .setType(ComponentType.MODULE)
      .addLink(ScannerReport.ComponentLink.newBuilder().setType(HOME).setHref("https://www.sonarsource.com/products/sonarqube").build())
      .build());

    underTest.execute(new TestComputationStepContext());

    assertThat(db.countRowsOfTable("project_links")).isZero();
  }

  @Test
  public void do_not_add_links_on_file() {
    mockBranch(true);
    treeRootHolder.setRoot(ReportComponent.builder(Component.Type.PROJECT, 1).setUuid(project.getMainBranchComponent().uuid()).addChildren(
      ReportComponent.builder(Component.Type.FILE, 2).setUuid("BCDE").build())
      .build());

    reportReader.putComponent(ScannerReport.Component.newBuilder()
      .setRef(1)
      .setType(ComponentType.PROJECT)
      .addChildRef(2)
      .build());
    reportReader.putComponent(ScannerReport.Component.newBuilder()
      .setRef(2)
      .setType(ComponentType.FILE)
      .addLink(ScannerReport.ComponentLink.newBuilder().setType(HOME).setHref("https://www.sonarsource.com/products/sonarqube").build())
      .build());

    underTest.execute(new TestComputationStepContext());

    assertThat(db.countRowsOfTable("project_links")).isZero();
  }

  @Test
  public void update_link() {
    mockBranch(true);
    analysisMetadataHolder.setProject(Project.fromProjectDtoWithTags(project.getProjectDto()));
    treeRootHolder.setRoot(ReportComponent.builder(Component.Type.PROJECT, 1).setUuid(project.getMainBranchComponent().uuid()).build());

    reportReader.putComponent(ScannerReport.Component.newBuilder()
      .setRef(1)
      .setType(ComponentType.PROJECT)
      .addLink(ScannerReport.ComponentLink.newBuilder().setType(HOME).setHref("https://www.sonarsource.com/products/sonarqube").build())
      .build());

    underTest.execute(new TestComputationStepContext());

    assertThat(db.getDbClient().projectLinkDao().selectByProjectUuid(db.getSession(), project.getProjectDto().getUuid()))
      .extracting(ProjectLinkDto::getType, ProjectLinkDto::getHref)
      .containsExactlyInAnyOrder(tuple("homepage", "https://www.sonarsource.com/products/sonarqube"));
  }

  @Test
  public void delete_link() {
    mockBranch(true);
    db.projectLinks().insertProvidedLink(project.getProjectDto(), l -> l.setType("homepage").setName("Home").setHref("http://www.sonar.org"));

    treeRootHolder.setRoot(ReportComponent.builder(Component.Type.PROJECT, 1).setUuid(project.getMainBranchComponent().uuid()).build());

    reportReader.putComponent(ScannerReport.Component.newBuilder()
      .setRef(1)
      .setType(ComponentType.PROJECT)
      .build());

    underTest.execute(new TestComputationStepContext());

    assertThat(db.countRowsOfTable("project_links")).isZero();
  }

  @Test
  public void not_delete_custom_link() {
    mockBranch(true);
    db.projectLinks().insertCustomLink(project.getProjectDto());

    treeRootHolder.setRoot(ReportComponent.builder(Component.Type.PROJECT, 1).setUuid(project.getMainBranchComponent().uuid()).build());

    reportReader.putComponent(ScannerReport.Component.newBuilder()
      .setRef(1)
      .setType(ComponentType.PROJECT)
      .build());

    underTest.execute(new TestComputationStepContext());

    assertThat(db.countRowsOfTable("project_links")).isOne();
  }

  @Test
  public void fail_when_trying_to_add_same_link_type_multiple_times() {
    mockBranch(true);
    treeRootHolder.setRoot(ReportComponent.builder(Component.Type.PROJECT, 1).setUuid(project.getMainBranchComponent().uuid()).build());

    reportReader.putComponent(ScannerReport.Component.newBuilder()
      .setRef(1)
      .setType(ComponentType.PROJECT)
      .addLink(ScannerReport.ComponentLink.newBuilder().setType(HOME).setHref("https://www.sonarsource.com/products/sonarqube").build())
      .addLink(ScannerReport.ComponentLink.newBuilder().setType(HOME).setHref("https://www.sonarsource.com/products/sonarqube").build())
      .build());

    assertThatThrownBy(() -> underTest.execute(new TestComputationStepContext()))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Link of type 'homepage' has already been declared on component '%s'".formatted(project.projectUuid()));
  }

  private void mockBranch(boolean isMain) {
    Branch branch = Mockito.mock(Branch.class);
    when(branch.isMain()).thenReturn(isMain);
    analysisMetadataHolder.setBranch(branch);
  }
}
