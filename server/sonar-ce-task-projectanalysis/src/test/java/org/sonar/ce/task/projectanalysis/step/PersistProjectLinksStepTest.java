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
package org.sonar.ce.task.projectanalysis.step;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
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
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ProjectLinkDto;
import org.sonar.scanner.protocol.output.ScannerReport;
import org.sonar.scanner.protocol.output.ScannerReport.Component.ComponentType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static org.sonar.scanner.protocol.output.ScannerReport.ComponentLink.ComponentLinkType.CI;
import static org.sonar.scanner.protocol.output.ScannerReport.ComponentLink.ComponentLinkType.HOME;
import static org.sonar.scanner.protocol.output.ScannerReport.ComponentLink.ComponentLinkType.ISSUE;
import static org.sonar.scanner.protocol.output.ScannerReport.ComponentLink.ComponentLinkType.SCM;

public class PersistProjectLinksStepTest extends BaseStepTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();
  @Rule
  public AnalysisMetadataHolderRule analysisMetadataHolder = new AnalysisMetadataHolderRule();
  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);
  @Rule
  public TreeRootHolderRule treeRootHolder = new TreeRootHolderRule();
  @Rule
  public BatchReportReaderRule reportReader = new BatchReportReaderRule();

  private PersistProjectLinksStep underTest = new PersistProjectLinksStep(analysisMetadataHolder, db.getDbClient(), treeRootHolder, reportReader, UuidFactoryFast.getInstance());

  @Override
  protected ComputationStep step() {
    return underTest;
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

    verifyZeroInteractions(uuidFactory, reportReader, treeRootHolder, dbClient);
  }

  @Test
  public void add_links_on_project() {
    mockBranch(true);
    treeRootHolder.setRoot(ReportComponent.builder(Component.Type.PROJECT, 1).setUuid("ABCD").build());

    // project
    reportReader.putComponent(ScannerReport.Component.newBuilder()
      .setRef(1)
      .setType(ComponentType.PROJECT)
      .addChildRef(2)
      .addLink(ScannerReport.ComponentLink.newBuilder().setType(HOME).setHref("http://www.sonarqube.org").build())
      .addLink(ScannerReport.ComponentLink.newBuilder().setType(SCM).setHref("https://github.com/SonarSource/sonar").build())
      .addLink(ScannerReport.ComponentLink.newBuilder().setType(ISSUE).setHref("http://jira.sonarsource.com/").build())
      .addLink(ScannerReport.ComponentLink.newBuilder().setType(CI).setHref("http://bamboo.ci.codehaus.org/browse/SONAR").build())
      .build());

    underTest.execute(new TestComputationStepContext());

    assertThat(db.getDbClient().projectLinkDao().selectByProjectUuid(db.getSession(), "ABCD"))
      .extracting(ProjectLinkDto::getType, ProjectLinkDto::getHref, ProjectLinkDto::getName)
      .containsExactlyInAnyOrder(
        tuple("homepage", "http://www.sonarqube.org", null),
        tuple("scm", "https://github.com/SonarSource/sonar", null),
        tuple("issue", "http://jira.sonarsource.com/", null),
        tuple("ci", "http://bamboo.ci.codehaus.org/browse/SONAR", null));
  }

  @Test
  public void nothing_to_do_when_link_already_exists() {
    mockBranch(true);
    ComponentDto project = db.components().insertPrivateProject(p -> p.setUuid("ABCD"));
    db.componentLinks().insertProvidedLink(project, l -> l.setType("homepage").setName("Home").setHref("http://www.sonarqube.org"));

    treeRootHolder.setRoot(ReportComponent.builder(Component.Type.PROJECT, 1).setUuid("ABCD").build());

    reportReader.putComponent(ScannerReport.Component.newBuilder()
      .setRef(1)
      .setType(ComponentType.PROJECT)
      .addLink(ScannerReport.ComponentLink.newBuilder().setType(HOME).setHref("http://www.sonarqube.org").build())
      .build());

    underTest.execute(new TestComputationStepContext());

    assertThat(db.getDbClient().projectLinkDao().selectByProjectUuid(db.getSession(), "ABCD"))
      .extracting(ProjectLinkDto::getType, ProjectLinkDto::getHref)
      .containsExactlyInAnyOrder(tuple("homepage", "http://www.sonarqube.org"));
  }

  @Test
  public void do_not_add_links_on_module() {
    mockBranch(true);
    treeRootHolder.setRoot(ReportComponent.builder(Component.Type.PROJECT, 1).setUuid("ABCD").build());
    reportReader.putComponent(ScannerReport.Component.newBuilder()
      .setRef(1)
      .setType(ComponentType.PROJECT)
      .addChildRef(2)
      .build());
    reportReader.putComponent(ScannerReport.Component.newBuilder()
      .setRef(2)
      .setType(ComponentType.MODULE)
      .addLink(ScannerReport.ComponentLink.newBuilder().setType(HOME).setHref("http://www.sonarqube.org").build())
      .build());

    underTest.execute(new TestComputationStepContext());

    assertThat(db.countRowsOfTable("project_links")).isZero();
  }

  @Test
  public void do_not_add_links_on_file() {
    mockBranch(true);
    treeRootHolder.setRoot(ReportComponent.builder(Component.Type.PROJECT, 1).setUuid("ABCD").addChildren(
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
      .addLink(ScannerReport.ComponentLink.newBuilder().setType(HOME).setHref("http://www.sonarqube.org").build())
      .build());

    underTest.execute(new TestComputationStepContext());

    assertThat(db.countRowsOfTable("project_links")).isZero();
  }

  @Test
  public void update_link() {
    mockBranch(true);
    ComponentDto project = db.components().insertPrivateProject(p -> p.setUuid("ABCD"));
    db.componentLinks().insertProvidedLink(project, l -> l.setType("homepage").setName("Home").setHref("http://www.sonar.org"));

    treeRootHolder.setRoot(ReportComponent.builder(Component.Type.PROJECT, 1).setUuid("ABCD").build());

    reportReader.putComponent(ScannerReport.Component.newBuilder()
      .setRef(1)
      .setType(ComponentType.PROJECT)
      .addLink(ScannerReport.ComponentLink.newBuilder().setType(HOME).setHref("http://www.sonarqube.org").build())
      .build());

    underTest.execute(new TestComputationStepContext());

    assertThat(db.getDbClient().projectLinkDao().selectByProjectUuid(db.getSession(), "ABCD"))
      .extracting(ProjectLinkDto::getType, ProjectLinkDto::getHref)
      .containsExactlyInAnyOrder(tuple("homepage", "http://www.sonarqube.org"));
  }

  @Test
  public void delete_link() {
    mockBranch(true);
    ComponentDto project = db.components().insertPrivateProject(p -> p.setUuid("ABCD"));
    db.componentLinks().insertProvidedLink(project, l -> l.setType("homepage").setName("Home").setHref("http://www.sonar.org"));

    treeRootHolder.setRoot(ReportComponent.builder(Component.Type.PROJECT, 1).setUuid("ABCD").build());

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
    ComponentDto project = db.components().insertPrivateProject(p -> p.setUuid("ABCD"));
    db.componentLinks().insertCustomLink(project);

    treeRootHolder.setRoot(ReportComponent.builder(Component.Type.PROJECT, 1).setUuid("ABCD").build());

    reportReader.putComponent(ScannerReport.Component.newBuilder()
      .setRef(1)
      .setType(ComponentType.PROJECT)
      .build());

    underTest.execute(new TestComputationStepContext());

    assertThat(db.countRowsOfTable("project_links")).isEqualTo(1);
  }

  @Test
  public void fail_when_trying_to_add_same_link_type_multiple_times() {
    mockBranch(true);
    treeRootHolder.setRoot(ReportComponent.builder(Component.Type.PROJECT, 1).setUuid("ABCD").build());

    reportReader.putComponent(ScannerReport.Component.newBuilder()
      .setRef(1)
      .setType(ComponentType.PROJECT)
      .addLink(ScannerReport.ComponentLink.newBuilder().setType(HOME).setHref("http://www.sonarqube.org").build())
      .addLink(ScannerReport.ComponentLink.newBuilder().setType(HOME).setHref("http://www.sonarqube.org").build())
      .build());

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Link of type 'homepage' has already been declared on component 'ABCD'");

    underTest.execute(new TestComputationStepContext());
  }

  private void mockBranch(boolean isMain) {
    Branch branch = Mockito.mock(Branch.class);
    when(branch.isMain()).thenReturn(isMain);
    analysisMetadataHolder.setBranch(branch);
  }
}
