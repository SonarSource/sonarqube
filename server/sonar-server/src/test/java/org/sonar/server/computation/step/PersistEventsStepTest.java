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
import org.sonar.api.utils.System2;
import org.sonar.batch.protocol.Constants;
import org.sonar.batch.protocol.output.BatchReport;
import org.sonar.batch.protocol.output.BatchReportReader;
import org.sonar.batch.protocol.output.BatchReportWriter;
import org.sonar.core.component.ComponentDto;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.persistence.DbTester;
import org.sonar.server.computation.ComputationContext;
import org.sonar.server.db.DbClient;
import org.sonar.server.event.db.EventDao;
import org.sonar.test.DbTests;

import java.io.File;
import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Category(DbTests.class)
public class PersistEventsStepTest extends BaseStepTest {

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  @ClassRule
  public static DbTester dbTester = new DbTester();

  DbSession session;

  EventDao dao;

  System2 system2;

  PersistEventsStep step;

  @Before
  public void setup() throws Exception {
    session = dbTester.myBatis().openSession(false);
    dao = new EventDao();
    DbClient dbClient = new DbClient(dbTester.database(), dbTester.myBatis(), dao);

    system2 = mock(System2.class);
    when(system2.now()).thenReturn(1225630680000L);

    step = new PersistEventsStep(dbClient, system2);
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
  public void nothing_to_do_when_no_events_in_report() throws Exception {
    dbTester.prepareDbUnit(getClass(), "nothing_to_do_when_no_events_in_report.xml");

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

    dbTester.assertDbUnit(getClass(), "nothing_to_do_when_no_events_in_report.xml", "events");
  }

  @Test
  public void persist_report_events() throws Exception {
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
      .setSnapshotId(1000L)
      .addEvent(BatchReport.Event.newBuilder()
        .setName("Red (was Orange)")
        .setCategory(Constants.EventCategory.ALERT)
        .setDescription("Open issues > 0")
        .build()
      )
      .addEvent(BatchReport.Event.newBuilder()
        .setName("Changes in 'Default' (Java)")
        .setCategory(Constants.EventCategory.PROFILE)
        .setEventData("from=2014-10-12T08:36:25+0000;key=java-default;to=2014-10-12T10:36:25+0000")
        .build()
      )
      .build());

    step.execute(new ComputationContext(new BatchReportReader(reportDir), mock(ComponentDto.class)));

    dbTester.assertDbUnit(getClass(), "add_events-result.xml", "events");
  }

  @Test
  public void persist_report_events_with_component_children() throws Exception {
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
      .setSnapshotId(1000L)
      .addEvent(BatchReport.Event.newBuilder()
        .setName("Red (was Orange)")
        .setCategory(Constants.EventCategory.ALERT)
        .setDescription("Open issues > 0")
        .build())
      .addChildRef(2)
      .build());

    writer.writeComponent(BatchReport.Component.newBuilder()
      .setRef(2)
      .setType(Constants.ComponentType.MODULE)
      .setUuid("BCDE")
      .setSnapshotId(1001L)
      .addEvent(BatchReport.Event.newBuilder()
          .setName("Red (was Orange)")
          .setCategory(Constants.EventCategory.ALERT)
          .setDescription("Open issues > 0")
          .build()
      ).build());

    step.execute(new ComputationContext(new BatchReportReader(reportDir), mock(ComponentDto.class)));

    dbTester.assertDbUnit(getClass(), "persist_report_events_with_component_children-result.xml", "events");
  }

  @Test
  public void create_version_event() throws Exception {
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
      .setSnapshotId(1000L)
      .setVersion("1.0")
      .build());

    step.execute(new ComputationContext(new BatchReportReader(reportDir), mock(ComponentDto.class)));

    dbTester.assertDbUnit(getClass(), "add_version_event-result.xml", "events");
  }

  @Test
  public void keep_one_event_by_version() throws Exception {
    dbTester.prepareDbUnit(getClass(), "keep_one_event_by_version.xml");

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
      .setSnapshotId(1001L)
      .setVersion("1.5-SNAPSHOT")
      .build());

    step.execute(new ComputationContext(new BatchReportReader(reportDir), mock(ComponentDto.class)));

    dbTester.assertDbUnit(getClass(), "keep_one_event_by_version-result.xml", "events");
  }

}
