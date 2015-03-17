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

package org.sonar.server.computation.step;

import org.junit.*;
import org.junit.experimental.categories.Category;
import org.junit.rules.TemporaryFolder;
import org.sonar.api.i18n.I18n;
import org.sonar.batch.protocol.Constants;
import org.sonar.batch.protocol.output.BatchReport;
import org.sonar.batch.protocol.output.BatchReportReader;
import org.sonar.batch.protocol.output.BatchReportWriter;
import org.sonar.core.component.ComponentDto;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.persistence.DbTester;
import org.sonar.server.component.db.ComponentLinkDao;
import org.sonar.server.computation.ComputationContext;
import org.sonar.server.db.DbClient;
import org.sonar.test.DbTests;

import java.io.File;
import java.io.IOException;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.failBecauseExceptionWasNotThrown;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Category(DbTests.class)
public class PersistComponentLinksStepTest extends BaseStepTest {

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  @ClassRule
  public static DbTester dbTester = new DbTester();

  DbSession session;

  ComponentLinkDao dao;

  I18n i18n;

  PersistComponentLinksStep step;

  @Before
  public void setup() throws Exception {
    session = dbTester.myBatis().openSession(false);
    dao = new ComponentLinkDao();
    DbClient dbClient = new DbClient(dbTester.database(), dbTester.myBatis(), dao);

    i18n = mock(I18n.class);
    when(i18n.message(Locale.ENGLISH, "project_links.homepage", null)).thenReturn("Home");
    when(i18n.message(Locale.ENGLISH, "project_links.scm", null)).thenReturn("Sources");
    when(i18n.message(Locale.ENGLISH, "project_links.scm_dev", null)).thenReturn("Developer connection");
    when(i18n.message(Locale.ENGLISH, "project_links.ci", null)).thenReturn("Continuous integration");
    when(i18n.message(Locale.ENGLISH, "project_links.issue", null)).thenReturn("Issues");

    step = new PersistComponentLinksStep(dbClient, i18n);
  }

  @Override
  protected ComputationStep step() throws IOException {
    return step;
  }

  @After
  public void tearDown() throws Exception {
    session.close();
  }

  @Test
  public void execute_only_on_projects() throws Exception {
    assertThat(step.supportedProjectQualifiers()).containsOnly("TRK");
  }

  @Test
  public void add_links_on_project_and_module() throws Exception {
    dbTester.prepareDbUnit(getClass(), "empty.xml");

    File reportDir = temp.newFolder();
    // project and 1 module
    BatchReportWriter writer = new BatchReportWriter(reportDir);
    writer.writeMetadata(BatchReport.Metadata.newBuilder()
      .setRootComponentRef(1)
      .setProjectKey("PROJECT_KEY")
      .setAnalysisDate(150000000L)
      .build());

    writer.writeComponent(BatchReport.Component.newBuilder()
      .setRef(1)
      .setType(Constants.ComponentType.PROJECT)
      .setUuid("ABCD")
      .addChildRef(2)
      .addLink(BatchReport.ComponentLink.newBuilder().setType(Constants.ComponentLinkType.HOME).setHref("http://www.sonarqube.org").build())
      .addLink(BatchReport.ComponentLink.newBuilder().setType(Constants.ComponentLinkType.SCM).setHref("https://github.com/SonarSource/sonar").build())
      .addLink(BatchReport.ComponentLink.newBuilder().setType(Constants.ComponentLinkType.SCM_DEV).setHref("scm:git:git@github.com:SonarSource/sonar.git/sonar").build())
      .addLink(BatchReport.ComponentLink.newBuilder().setType(Constants.ComponentLinkType.ISSUE).setHref("http://jira.sonarsource.com/").build())
      .addLink(BatchReport.ComponentLink.newBuilder().setType(Constants.ComponentLinkType.CI).setHref("http://bamboo.ci.codehaus.org/browse/SONAR").build())
      .build());
    writer.writeComponent(BatchReport.Component.newBuilder()
      .setRef(2)
      .setType(Constants.ComponentType.MODULE)
      .setUuid("BCDE")
      .addLink(BatchReport.ComponentLink.newBuilder().setType(Constants.ComponentLinkType.SCM).setHref("https://github.com/SonarSource/sonar/server").build())
      .build());

    step.execute(new ComputationContext(new BatchReportReader(reportDir), mock(ComponentDto.class)));

    dbTester.assertDbUnit(getClass(), "add_links_on_project_and_module-result.xml", "project_links");
  }

  @Test
  public void nothing_to_do_when_link_already_exists() throws Exception {
    dbTester.prepareDbUnit(getClass(), "nothing_to_do_when_link_already_exists.xml");

    File reportDir = temp.newFolder();
    BatchReportWriter writer = new BatchReportWriter(reportDir);
    writer.writeMetadata(BatchReport.Metadata.newBuilder()
      .setRootComponentRef(1)
      .setProjectKey("PROJECT_KEY")
      .setAnalysisDate(150000000L)
      .build());

    writer.writeComponent(BatchReport.Component.newBuilder()
      .setRef(1)
      .setType(Constants.ComponentType.PROJECT)
      .setUuid("ABCD")
      .addLink(BatchReport.ComponentLink.newBuilder().setType(Constants.ComponentLinkType.HOME).setHref("http://www.sonarqube.org").build())
      .build());

    step.execute(new ComputationContext(new BatchReportReader(reportDir), mock(ComponentDto.class)));

    dbTester.assertDbUnit(getClass(), "nothing_to_do_when_link_already_exists.xml", "project_links");
  }

  @Test
  public void do_not_add_links_on_file() throws Exception {
    dbTester.prepareDbUnit(getClass(), "empty.xml");

    File reportDir = temp.newFolder();
    BatchReportWriter writer = new BatchReportWriter(reportDir);
    writer.writeMetadata(BatchReport.Metadata.newBuilder()
      .setRootComponentRef(1)
      .setProjectKey("PROJECT_KEY")
      .setAnalysisDate(150000000L)
      .build());

    writer.writeComponent(BatchReport.Component.newBuilder()
      .setRef(1)
      .setType(Constants.ComponentType.FILE)
      .setUuid("ABCD")
      .addLink(BatchReport.ComponentLink.newBuilder().setType(Constants.ComponentLinkType.HOME).setHref("http://www.sonarqube.org").build())
      .build());

    step.execute(new ComputationContext(new BatchReportReader(reportDir), mock(ComponentDto.class)));

    assertThat(dbTester.countRowsOfTable("project_links")).isEqualTo(0);
  }

  @Test
  public void update_link() throws Exception {
    dbTester.prepareDbUnit(getClass(), "update_link.xml");

    File reportDir = temp.newFolder();
    BatchReportWriter writer = new BatchReportWriter(reportDir);
    writer.writeMetadata(BatchReport.Metadata.newBuilder()
      .setRootComponentRef(1)
      .setProjectKey("PROJECT_KEY")
      .setAnalysisDate(150000000L)
      .build());

    writer.writeComponent(BatchReport.Component.newBuilder()
      .setRef(1)
      .setType(Constants.ComponentType.PROJECT)
      .setUuid("ABCD")
      .addLink(BatchReport.ComponentLink.newBuilder().setType(Constants.ComponentLinkType.HOME).setHref("http://www.sonarqube.org").build())
      .build());

    step.execute(new ComputationContext(new BatchReportReader(reportDir), mock(ComponentDto.class)));

    dbTester.assertDbUnit(getClass(), "update_link-result.xml", "project_links");
  }

  @Test
  public void delete_link() throws Exception {
    dbTester.prepareDbUnit(getClass(), "delete_link.xml");

    File reportDir = temp.newFolder();
    BatchReportWriter writer = new BatchReportWriter(reportDir);
    writer.writeMetadata(BatchReport.Metadata.newBuilder()
      .setRootComponentRef(1)
      .setProjectKey("PROJECT_KEY")
      .setAnalysisDate(150000000L)
      .build());

    writer.writeComponent(BatchReport.Component.newBuilder()
      .setRef(1)
      .setType(Constants.ComponentType.PROJECT)
      .setUuid("ABCD")
      .build());

    step.execute(new ComputationContext(new BatchReportReader(reportDir), mock(ComponentDto.class)));

    assertThat(dbTester.countRowsOfTable("project_links")).isEqualTo(0);
  }

  @Test
  public void not_delete_custom_link() throws Exception {
    dbTester.prepareDbUnit(getClass(), "not_delete_custom_link.xml");

    File reportDir = temp.newFolder();
    BatchReportWriter writer = new BatchReportWriter(reportDir);
    writer.writeMetadata(BatchReport.Metadata.newBuilder()
      .setRootComponentRef(1)
      .setProjectKey("PROJECT_KEY")
      .setAnalysisDate(150000000L)
      .build());

    writer.writeComponent(BatchReport.Component.newBuilder()
      .setRef(1)
      .setType(Constants.ComponentType.PROJECT)
      .setUuid("ABCD")
      .build());

    step.execute(new ComputationContext(new BatchReportReader(reportDir), mock(ComponentDto.class)));

    dbTester.assertDbUnit(getClass(), "not_delete_custom_link.xml", "project_links");
  }

  @Test
  public void fail_when_trying_to_add_same_link_type_multiple_times() throws Exception {
    dbTester.prepareDbUnit(getClass(), "empty.xml");

    File reportDir = temp.newFolder();
    BatchReportWriter writer = new BatchReportWriter(reportDir);
    writer.writeMetadata(BatchReport.Metadata.newBuilder()
      .setRootComponentRef(1)
      .setProjectKey("PROJECT_KEY")
      .setAnalysisDate(150000000L)
      .build());

    writer.writeComponent(BatchReport.Component.newBuilder()
      .setRef(1)
      .setType(Constants.ComponentType.PROJECT)
      .setUuid("ABCD")
      .addLink(BatchReport.ComponentLink.newBuilder().setType(Constants.ComponentLinkType.HOME).setHref("http://www.sonarqube.org").build())
      .addLink(BatchReport.ComponentLink.newBuilder().setType(Constants.ComponentLinkType.HOME).setHref("http://www.sonarqube.org").build())
      .build());

    try {
      step.execute(new ComputationContext(new BatchReportReader(reportDir), mock(ComponentDto.class)));
      failBecauseExceptionWasNotThrown(IllegalArgumentException.class);
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage("Link of type 'homepage' has already been declared on component 'ABCD'");
    }
  }
}
