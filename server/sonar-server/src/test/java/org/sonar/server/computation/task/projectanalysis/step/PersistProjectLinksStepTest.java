/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
package org.sonar.server.computation.task.projectanalysis.step;

import java.util.Locale;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.i18n.I18n;
import org.sonar.api.utils.System2;
import org.sonar.db.DbTester;
import org.sonar.scanner.protocol.output.ScannerReport;
import org.sonar.scanner.protocol.output.ScannerReport.Component.ComponentType;
import org.sonar.scanner.protocol.output.ScannerReport.ComponentLink.ComponentLinkType;
import org.sonar.server.computation.task.projectanalysis.batch.BatchReportReaderRule;
import org.sonar.server.computation.task.projectanalysis.component.TreeRootHolderRule;
import org.sonar.server.computation.task.projectanalysis.component.Component;
import org.sonar.server.computation.task.projectanalysis.component.ReportComponent;
import org.sonar.server.computation.task.projectanalysis.component.VisitException;
import org.sonar.server.computation.task.step.ComputationStep;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.failBecauseExceptionWasNotThrown;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PersistProjectLinksStepTest extends BaseStepTest {

  @Rule
  public DbTester dbTester = DbTester.create(System2.INSTANCE);

  @Rule
  public TreeRootHolderRule treeRootHolder = new TreeRootHolderRule();

  @Rule
  public BatchReportReaderRule reportReader = new BatchReportReaderRule();

  PersistProjectLinksStep step;

  @Before
  public void setup() {
    I18n i18n = mock(I18n.class);
    when(i18n.message(Locale.ENGLISH, "project_links.homepage", null)).thenReturn("Home");
    when(i18n.message(Locale.ENGLISH, "project_links.scm", null)).thenReturn("Sources");
    when(i18n.message(Locale.ENGLISH, "project_links.scm_dev", null)).thenReturn("Developer connection");
    when(i18n.message(Locale.ENGLISH, "project_links.ci", null)).thenReturn("Continuous integration");
    when(i18n.message(Locale.ENGLISH, "project_links.issue", null)).thenReturn("Issues");

    step = new PersistProjectLinksStep(dbTester.getDbClient(), i18n, treeRootHolder, reportReader);
  }

  @Override
  protected ComputationStep step() {
    return step;
  }

  @Test
  public void add_links_on_project_and_module() {
    dbTester.prepareDbUnit(getClass(), "empty.xml");

    treeRootHolder.setRoot(ReportComponent.builder(Component.Type.PROJECT, 1).setUuid("ABCD").addChildren(
      ReportComponent.builder(Component.Type.MODULE, 2).setUuid("BCDE").build())
      .build());

    // project and 1 module
    reportReader.putComponent(ScannerReport.Component.newBuilder()
      .setRef(1)
      .setType(ComponentType.PROJECT)
      .addChildRef(2)
      .addLink(ScannerReport.ComponentLink.newBuilder().setType(ComponentLinkType.HOME).setHref("http://www.sonarqube.org").build())
      .addLink(ScannerReport.ComponentLink.newBuilder().setType(ComponentLinkType.SCM).setHref("https://github.com/SonarSource/sonar").build())
      .addLink(ScannerReport.ComponentLink.newBuilder().setType(ComponentLinkType.SCM_DEV).setHref("scm:git:git@github.com:SonarSource/sonar.git/sonar").build())
      .addLink(ScannerReport.ComponentLink.newBuilder().setType(ComponentLinkType.ISSUE).setHref("http://jira.sonarsource.com/").build())
      .addLink(ScannerReport.ComponentLink.newBuilder().setType(ComponentLinkType.CI).setHref("http://bamboo.ci.codehaus.org/browse/SONAR").build())
      .build());
    reportReader.putComponent(ScannerReport.Component.newBuilder()
      .setRef(2)
      .setType(ComponentType.MODULE)
      .addLink(ScannerReport.ComponentLink.newBuilder().setType(ComponentLinkType.SCM).setHref("https://github.com/SonarSource/sonar/server").build())
      .build());

    step.execute();

    dbTester.assertDbUnit(getClass(), "add_links_on_project_and_module-result.xml", "project_links");
  }

  @Test
  public void nothing_to_do_when_link_already_exists() {
    dbTester.prepareDbUnit(getClass(), "nothing_to_do_when_link_already_exists.xml");

    treeRootHolder.setRoot(ReportComponent.builder(Component.Type.PROJECT, 1).setUuid("ABCD").build());

    reportReader.putComponent(ScannerReport.Component.newBuilder()
      .setRef(1)
      .setType(ComponentType.PROJECT)
      .addLink(ScannerReport.ComponentLink.newBuilder().setType(ComponentLinkType.HOME).setHref("http://www.sonarqube.org").build())
      .build());

    step.execute();

    dbTester.assertDbUnit(getClass(), "nothing_to_do_when_link_already_exists.xml", "project_links");
  }

  @Test
  public void do_not_add_links_on_file() {
    dbTester.prepareDbUnit(getClass(), "empty.xml");

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
      .addLink(ScannerReport.ComponentLink.newBuilder().setType(ComponentLinkType.HOME).setHref("http://www.sonarqube.org").build())
      .build());

    step.execute();

    assertThat(dbTester.countRowsOfTable("project_links")).isEqualTo(0);
  }

  @Test
  public void update_link() {
    dbTester.prepareDbUnit(getClass(), "update_link.xml");

    treeRootHolder.setRoot(ReportComponent.builder(Component.Type.PROJECT, 1).setUuid("ABCD").build());

    reportReader.putComponent(ScannerReport.Component.newBuilder()
      .setRef(1)
      .setType(ComponentType.PROJECT)
      .addLink(ScannerReport.ComponentLink.newBuilder().setType(ComponentLinkType.HOME).setHref("http://www.sonarqube.org").build())
      .build());

    step.execute();

    dbTester.assertDbUnit(getClass(), "update_link-result.xml", "project_links");
  }

  @Test
  public void delete_link() {
    dbTester.prepareDbUnit(getClass(), "delete_link.xml");

    treeRootHolder.setRoot(ReportComponent.builder(Component.Type.PROJECT, 1).setUuid("ABCD").build());

    reportReader.putComponent(ScannerReport.Component.newBuilder()
      .setRef(1)
      .setType(ComponentType.PROJECT)
      .build());

    step.execute();

    assertThat(dbTester.countRowsOfTable("project_links")).isEqualTo(0);
  }

  @Test
  public void not_delete_custom_link() {
    dbTester.prepareDbUnit(getClass(), "not_delete_custom_link.xml");

    treeRootHolder.setRoot(ReportComponent.builder(Component.Type.PROJECT, 1).setUuid("ABCD").build());

    reportReader.putComponent(ScannerReport.Component.newBuilder()
      .setRef(1)
      .setType(ComponentType.PROJECT)
      .build());

    step.execute();

    dbTester.assertDbUnit(getClass(), "not_delete_custom_link.xml", "project_links");
  }

  @Test
  public void fail_when_trying_to_add_same_link_type_multiple_times() {
    dbTester.prepareDbUnit(getClass(), "empty.xml");

    treeRootHolder.setRoot(ReportComponent.builder(Component.Type.PROJECT, 1).setUuid("ABCD").build());

    reportReader.putComponent(ScannerReport.Component.newBuilder()
      .setRef(1)
      .setType(ComponentType.PROJECT)
      .addLink(ScannerReport.ComponentLink.newBuilder().setType(ComponentLinkType.HOME).setHref("http://www.sonarqube.org").build())
      .addLink(ScannerReport.ComponentLink.newBuilder().setType(ComponentLinkType.HOME).setHref("http://www.sonarqube.org").build())
      .build());

    try {
      step.execute();
      failBecauseExceptionWasNotThrown(VisitException.class);
    } catch (VisitException e) {
      assertThat(e.getCause()).isInstanceOf(IllegalArgumentException.class);
      assertThat(e.getCause()).hasMessage("Link of type 'homepage' has already been declared on component 'ABCD'");
    }
  }
}
