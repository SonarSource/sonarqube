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

import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.sonar.api.utils.System2;
import org.sonar.batch.protocol.Constants;
import org.sonar.batch.protocol.output.BatchReport;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.persistence.DbTester;
import org.sonar.server.computation.batch.BatchReportReaderRule;
import org.sonar.server.computation.batch.TreeRootHolderRule;
import org.sonar.server.computation.component.Component;
import org.sonar.server.computation.component.DumbComponent;
import org.sonar.server.db.DbClient;
import org.sonar.server.event.db.EventDao;
import org.sonar.test.DbTests;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Category(DbTests.class)
public class PersistEventsStepTest extends BaseStepTest {

  @ClassRule
  public static DbTester dbTester = new DbTester();

  @Rule
  public TreeRootHolderRule treeRootHolder = new TreeRootHolderRule();

  @Rule
  public BatchReportReaderRule reportReader = new BatchReportReaderRule();

  DbSession session;
  PersistEventsStep step;

  @Before
  public void setup() {
    session = dbTester.myBatis().openSession(false);
    DbClient dbClient = new DbClient(dbTester.database(), dbTester.myBatis(), new EventDao());

    System2 system2 = mock(System2.class);
    when(system2.now()).thenReturn(1225630680000L);

    step = new PersistEventsStep(dbClient, system2, treeRootHolder, reportReader);
  }

  @Override
  protected ComputationStep step() {
    return step;
  }

  @After
  public void tearDown() {
    session.close();
  }

  @Test
  public void nothing_to_do_when_no_events_in_report() throws Exception {
    dbTester.prepareDbUnit(getClass(), "nothing_to_do_when_no_events_in_report.xml");

    treeRootHolder.setRoot(new DumbComponent(Component.Type.PROJECT, 1, "ABCD", null));

    reportReader.setMetadata(BatchReport.Metadata.newBuilder()
      .setRootComponentRef(1)
      .setAnalysisDate(150000000L)
      .build());

    reportReader.putComponent(BatchReport.Component.newBuilder()
      .setRef(1)
      .setType(Constants.ComponentType.PROJECT)
      .build());

    step.execute();

    dbTester.assertDbUnit(getClass(), "nothing_to_do_when_no_events_in_report.xml", "events");
  }

  @Test
  public void persist_report_events() throws Exception {
    dbTester.prepareDbUnit(getClass(), "empty.xml");

    treeRootHolder.setRoot(new DumbComponent(Component.Type.PROJECT, 1, "ABCD", null));

    reportReader.setMetadata(BatchReport.Metadata.newBuilder()
      .setRootComponentRef(1)
      .setAnalysisDate(150000000L)
      .build());

    reportReader.putComponent(BatchReport.Component.newBuilder()
      .setRef(1)
      .setType(Constants.ComponentType.PROJECT)
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

    step.execute();

    dbTester.assertDbUnit(getClass(), "add_events-result.xml", "events");
  }

  @Test
  public void persist_report_events_with_component_children() throws Exception {
    dbTester.prepareDbUnit(getClass(), "empty.xml");

    treeRootHolder.setRoot(new DumbComponent(Component.Type.PROJECT, 1, "ABCD", null,
      new DumbComponent(Component.Type.MODULE, 2, "BCDE", null)));

    reportReader.setMetadata(BatchReport.Metadata.newBuilder()
      .setRootComponentRef(1)
      .setAnalysisDate(150000000L)
      .build());

    reportReader.putComponent(BatchReport.Component.newBuilder()
      .setRef(1)
      .setType(Constants.ComponentType.PROJECT)
      .setSnapshotId(1000L)
      .addEvent(BatchReport.Event.newBuilder()
        .setName("Red (was Orange)")
        .setCategory(Constants.EventCategory.ALERT)
        .setDescription("Open issues > 0")
        .build())
      .addChildRef(2)
      .build());

    reportReader.putComponent(BatchReport.Component.newBuilder()
      .setRef(2)
      .setType(Constants.ComponentType.MODULE)
      .setSnapshotId(1001L)
      .addEvent(BatchReport.Event.newBuilder()
        .setName("Red (was Orange)")
        .setCategory(Constants.EventCategory.ALERT)
        .setDescription("Open issues > 0")
        .build()
      ).build());

    step.execute();

    dbTester.assertDbUnit(getClass(), "persist_report_events_with_component_children-result.xml", "events");
  }

  @Test
  public void create_version_event() throws Exception {
    dbTester.prepareDbUnit(getClass(), "empty.xml");

    treeRootHolder.setRoot(new DumbComponent(Component.Type.PROJECT, 1, "ABCD", null));

    reportReader.setMetadata(BatchReport.Metadata.newBuilder()
      .setRootComponentRef(1)
      .setAnalysisDate(150000000L)
      .build());

    reportReader.putComponent(BatchReport.Component.newBuilder()
      .setRef(1)
      .setType(Constants.ComponentType.PROJECT)
      .setSnapshotId(1000L)
      .setVersion("1.0")
      .build());

    step.execute();

    dbTester.assertDbUnit(getClass(), "add_version_event-result.xml", "events");
  }

  @Test
  public void keep_one_event_by_version() throws Exception {
    dbTester.prepareDbUnit(getClass(), "keep_one_event_by_version.xml");

    treeRootHolder.setRoot(new DumbComponent(Component.Type.PROJECT, 1, "ABCD", null));

    reportReader.setMetadata(BatchReport.Metadata.newBuilder()
      .setRootComponentRef(1)
      .setAnalysisDate(150000000L)
      .build());

    reportReader.putComponent(BatchReport.Component.newBuilder()
      .setRef(1)
      .setType(Constants.ComponentType.PROJECT)
      .setSnapshotId(1001L)
      .setVersion("1.5-SNAPSHOT")
      .build());

    step.execute();

    dbTester.assertDbUnit(getClass(), "keep_one_event_by_version-result.xml", "events");
  }

}
