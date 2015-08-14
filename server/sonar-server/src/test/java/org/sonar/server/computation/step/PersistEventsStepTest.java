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
import java.util.Date;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.sonar.api.utils.System2;
import org.sonar.db.DbTester;
import org.sonar.server.computation.analysis.MutableAnalysisMetadataHolderRule;
import org.sonar.server.computation.batch.TreeRootHolderRule;
import org.sonar.server.computation.component.Component;
import org.sonar.server.computation.component.DbIdsRepositoryImpl;
import org.sonar.server.computation.component.ReportComponent;
import org.sonar.server.computation.event.Event;
import org.sonar.server.computation.event.EventRepository;
import org.sonar.test.DbTests;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Category(DbTests.class)
public class PersistEventsStepTest extends BaseStepTest {

  System2 system2 = mock(System2.class);

  @Rule
  public DbTester dbTester = DbTester.create(system2);

  @Rule
  public MutableAnalysisMetadataHolderRule analysisMetadataHolder = new MutableAnalysisMetadataHolderRule();

  @Rule
  public TreeRootHolderRule treeRootHolder = new TreeRootHolderRule();

  private Date someDate = new Date(150000000L);

  DbIdsRepositoryImpl dbIdsRepository = new DbIdsRepositoryImpl();

  EventRepository eventRepository = mock(EventRepository.class);
  PersistEventsStep step;

  @Before
  public void setup() {
    when(system2.now()).thenReturn(1225630680000L);
    analysisMetadataHolder.setAnalysisDate(someDate);
    step = new PersistEventsStep(dbTester.getDbClient(), system2, treeRootHolder, analysisMetadataHolder, eventRepository, dbIdsRepository);
    when(eventRepository.getEvents(any(Component.class))).thenReturn(Collections.<Event>emptyList());
  }

  @Override
  protected ComputationStep step() {
    return step;
  }

  @Test
  public void nothing_to_do_when_no_events_in_report() {
    dbTester.prepareDbUnit(getClass(), "nothing_to_do_when_no_events_in_report.xml");

    treeRootHolder.setRoot(ReportComponent.builder(Component.Type.PROJECT, 1).setUuid("ABCD").build());

    step.execute();

    dbTester.assertDbUnit(getClass(), "nothing_to_do_when_no_events_in_report.xml", "events");
  }

  @Test
  public void persist_report_events_with_component_children() {
    dbTester.prepareDbUnit(getClass(), "empty.xml");

    ReportComponent module = ReportComponent.builder(Component.Type.MODULE, 2).setUuid("BCDE").build();
    ReportComponent root = ReportComponent.builder(Component.Type.PROJECT, 1).setUuid("ABCD").addChildren(module).build();
    treeRootHolder.setRoot(root);

    dbIdsRepository.setSnapshotId(root, 1000L);
    dbIdsRepository.setSnapshotId(module, 1001L);

    Component child = root.getChildren().get(0);

    when(eventRepository.getEvents(root)).thenReturn(ImmutableList.of(Event.createAlert("Red (was Orange)", null, "Open issues > 0")));
    when(eventRepository.getEvents(child)).thenReturn(ImmutableList.of(Event.createAlert("Red (was Orange)", null, "Open issues > 0")));

    treeRootHolder.setRoot(root);
    step.execute();

    dbTester.assertDbUnit(getClass(), "persist_report_events_with_component_children-result.xml", "events");
  }

  @Test
  public void create_version_event() {
    dbTester.prepareDbUnit(getClass(), "empty.xml");

    Component project = ReportComponent.builder(Component.Type.PROJECT, 1).setUuid("ABCD").setVersion("1.0").build();
    treeRootHolder.setRoot(project);
    dbIdsRepository.setSnapshotId(project, 1000L);

    step.execute();

    dbTester.assertDbUnit(getClass(), "add_version_event-result.xml", "events");
  }

  @Test
  public void keep_one_event_by_version() {
    dbTester.prepareDbUnit(getClass(), "keep_one_event_by_version.xml");

    Component project = ReportComponent.builder(Component.Type.PROJECT, 1).setUuid("ABCD").setVersion("1.5-SNAPSHOT").build();
    treeRootHolder.setRoot(project);
    dbIdsRepository.setSnapshotId(project, 1001L);

    step.execute();

    dbTester.assertDbUnit(getClass(), "keep_one_event_by_version-result.xml", "events");
  }

}
