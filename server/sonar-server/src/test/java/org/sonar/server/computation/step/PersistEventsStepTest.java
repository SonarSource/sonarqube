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

import com.google.common.collect.ImmutableList;
import java.util.Collections;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.sonar.api.utils.System2;
import org.sonar.batch.protocol.output.BatchReport;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.server.computation.batch.BatchReportReaderRule;
import org.sonar.server.computation.batch.TreeRootHolderRule;
import org.sonar.server.computation.component.Component;
import org.sonar.server.computation.component.DbIdsRepository;
import org.sonar.server.computation.component.DumbComponent;
import org.sonar.server.computation.event.Event;
import org.sonar.server.computation.event.EventRepository;
import org.sonar.server.db.DbClient;
import org.sonar.db.event.EventDao;
import org.sonar.test.DbTests;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Category(DbTests.class)
public class PersistEventsStepTest extends BaseStepTest {

  @ClassRule
  public static DbTester dbTester = new DbTester();

  @Rule
  public BatchReportReaderRule reportReader = new BatchReportReaderRule();

  @Rule
  public TreeRootHolderRule treeRootHolder = new TreeRootHolderRule();

  DbIdsRepository dbIdsRepository = new DbIdsRepository();

  DbSession session;
  EventRepository eventRepository = mock(EventRepository.class);
  PersistEventsStep step;

  @Before
  public void setup() {
    session = dbTester.myBatis().openSession(false);
    DbClient dbClient = new DbClient(dbTester.database(), dbTester.myBatis(), new EventDao());

    System2 system2 = mock(System2.class);
    when(system2.now()).thenReturn(1225630680000L);

    step = new PersistEventsStep(dbClient, system2, treeRootHolder, reportReader, eventRepository, dbIdsRepository);

    when(eventRepository.getEvents(any(Component.class))).thenReturn(Collections.<Event>emptyList());
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

    treeRootHolder.setRoot(DumbComponent.builder(Component.Type.PROJECT, 1).setUuid("ABCD").build());

    reportReader.setMetadata(BatchReport.Metadata.newBuilder()
      .setRootComponentRef(1)
      .setAnalysisDate(150000000L)
      .build());

    step.execute();

    dbTester.assertDbUnit(getClass(), "nothing_to_do_when_no_events_in_report.xml", "events");
  }

  @Test
  public void persist_report_events_with_component_children() throws Exception {
    dbTester.prepareDbUnit(getClass(), "empty.xml");

    DumbComponent module = DumbComponent.builder(Component.Type.MODULE, 2).setUuid("BCDE").build(); 
    DumbComponent root = DumbComponent.builder(Component.Type.PROJECT, 1).setUuid("ABCD").addChildren(module).build();
    treeRootHolder.setRoot(root);

    dbIdsRepository.setSnapshotId(root, 1000L);
    dbIdsRepository.setSnapshotId(module, 1001L);

    reportReader.setMetadata(BatchReport.Metadata.newBuilder()
      .setRootComponentRef(1)
      .setAnalysisDate(150000000L)
      .build());

    Component child = root.getChildren().get(0);

    when(eventRepository.getEvents(root)).thenReturn(ImmutableList.of(Event.createAlert("Red (was Orange)", null, "Open issues > 0")));
    when(eventRepository.getEvents(child)).thenReturn(ImmutableList.of(Event.createAlert("Red (was Orange)", null, "Open issues > 0")));

    treeRootHolder.setRoot(root);
    step.execute();

    dbTester.assertDbUnit(getClass(), "persist_report_events_with_component_children-result.xml", "events");
  }

  @Test
  public void create_version_event() throws Exception {
    dbTester.prepareDbUnit(getClass(), "empty.xml");

    Component project = DumbComponent.builder(Component.Type.PROJECT, 1).setUuid("ABCD").setVersion("1.0").build();
    treeRootHolder.setRoot(project);
    dbIdsRepository.setSnapshotId(project, 1000L);

    reportReader.setMetadata(BatchReport.Metadata.newBuilder()
      .setRootComponentRef(1)
      .setAnalysisDate(150000000L)
      .build());

    step.execute();

    dbTester.assertDbUnit(getClass(), "add_version_event-result.xml", "events");
  }

  @Test
  public void keep_one_event_by_version() throws Exception {
    dbTester.prepareDbUnit(getClass(), "keep_one_event_by_version.xml");

    Component project = DumbComponent.builder(Component.Type.PROJECT, 1).setUuid("ABCD").setVersion("1.5-SNAPSHOT").build();
    treeRootHolder.setRoot(project);
    dbIdsRepository.setSnapshotId(project, 1001L);

    reportReader.setMetadata(BatchReport.Metadata.newBuilder()
      .setRootComponentRef(1)
      .setAnalysisDate(150000000L)
      .build());

    step.execute();

    dbTester.assertDbUnit(getClass(), "keep_one_event_by_version-result.xml", "events");
  }

}
