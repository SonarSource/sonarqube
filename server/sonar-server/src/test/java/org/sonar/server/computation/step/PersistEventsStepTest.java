/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package org.sonar.server.computation.step;

import com.google.common.collect.ImmutableList;
import java.util.Collections;
import java.util.Date;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.utils.System2;
import org.sonar.db.DbTester;
import org.sonar.server.computation.analysis.AnalysisMetadataHolderRule;
import org.sonar.server.computation.batch.TreeRootHolderRule;
import org.sonar.server.computation.component.Component;
import org.sonar.server.computation.component.DbIdsRepositoryImpl;
import org.sonar.server.computation.component.ReportComponent;
import org.sonar.server.computation.event.Event;
import org.sonar.server.computation.event.EventRepository;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.server.computation.component.Component.Type.DIRECTORY;
import static org.sonar.server.computation.component.Component.Type.FILE;
import static org.sonar.server.computation.component.Component.Type.MODULE;
import static org.sonar.server.computation.component.Component.Type.PROJECT;
import static org.sonar.server.computation.component.ReportComponent.builder;

public class PersistEventsStepTest extends BaseStepTest {

  private static final ReportComponent ROOT = builder(PROJECT, 1)
    .setUuid("ABCD")
    .addChildren(
      builder(MODULE, 2)
        .setUuid("BCDE")
        .addChildren(
          builder(DIRECTORY, 3)
            .setUuid("Q")
            .addChildren(
              builder(FILE, 4)
                .setUuid("Z")
                .build())
            .build())
        .build())
    .build();

  System2 system2 = mock(System2.class);

  @Rule
  public DbTester dbTester = DbTester.create(system2);
  @Rule
  public AnalysisMetadataHolderRule analysisMetadataHolder = new AnalysisMetadataHolderRule();
  @Rule
  public TreeRootHolderRule treeRootHolder = new TreeRootHolderRule();

  private Date someDate = new Date(150000000L);

  DbIdsRepositoryImpl dbIdsRepository = new DbIdsRepositoryImpl();

  EventRepository eventRepository = mock(EventRepository.class);
  PersistEventsStep step;

  @Before
  public void setup() {
    when(system2.now()).thenReturn(1225630680000L);
    analysisMetadataHolder.setAnalysisDate(someDate.getTime());
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

    treeRootHolder.setRoot(ROOT);

    step.execute();

    dbTester.assertDbUnit(getClass(), "nothing_to_do_when_no_events_in_report.xml", "events");
  }

  @Test
  public void persist_report_events_with_component_children() {
    dbTester.prepareDbUnit(getClass(), "empty.xml");

    treeRootHolder.setRoot(ROOT);

    dbIdsRepository.setSnapshotId(ROOT, 1000L);

    when(eventRepository.getEvents(ROOT)).thenReturn(ImmutableList.of(Event.createAlert("Red (was Orange)", null, "Open issues > 0")));

    treeRootHolder.setRoot(ROOT);
    step.execute();

    dbTester.assertDbUnit(getClass(), "persist_report_events_with_component_children-result.xml", "events");
  }

  @Test
  public void create_version_event() {
    dbTester.prepareDbUnit(getClass(), "empty.xml");

    Component project = builder(PROJECT, 1)
      .setUuid("ABCD")
      .setVersion("1.0")
      .addChildren(
        builder(MODULE, 2)
          .setUuid("BCDE")
          .addChildren(
            builder(DIRECTORY, 3)
              .setUuid("Q")
              .addChildren(
                builder(FILE, 4)
                  .setUuid("Z")
                  .build())
              .build())
          .build())
      .build();
    treeRootHolder.setRoot(project);
    dbIdsRepository.setSnapshotId(ROOT, 1000L);

    step.execute();

    dbTester.assertDbUnit(getClass(), "add_version_event-result.xml", "events");
  }

  @Test
  public void keep_one_event_by_version() {
    dbTester.prepareDbUnit(getClass(), "keep_one_event_by_version.xml");

    Component project = builder(PROJECT, 1)
      .setUuid("ABCD")
      .setVersion("1.5-SNAPSHOT")
      .addChildren(
        builder(MODULE, 2)
          .setUuid("BCDE")
          .addChildren(
            builder(DIRECTORY, 3)
              .setUuid("Q")
              .addChildren(
                builder(FILE, 4)
                  .setUuid("Z")
                  .build())
              .build())
          .build())
      .build();
    treeRootHolder.setRoot(project);
    dbIdsRepository.setSnapshotId(ROOT, 1001L);

    step.execute();

    dbTester.assertDbUnit(getClass(), "keep_one_event_by_version-result.xml", "events");
  }

}
