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

import com.google.common.collect.ImmutableList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.utils.System2;
import org.sonar.ce.task.projectanalysis.analysis.AnalysisMetadataHolderRule;
import org.sonar.ce.task.projectanalysis.component.Component;
import org.sonar.ce.task.projectanalysis.component.ReportComponent;
import org.sonar.ce.task.projectanalysis.component.TreeRootHolderRule;
import org.sonar.ce.task.projectanalysis.event.Event;
import org.sonar.ce.task.projectanalysis.event.EventRepository;
import org.sonar.ce.task.step.ComputationStep;
import org.sonar.ce.task.step.TestComputationStepContext;
import org.sonar.core.util.UuidFactory;
import org.sonar.core.util.UuidFactoryImpl;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.event.EventDto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.ce.task.projectanalysis.component.Component.Type.DIRECTORY;
import static org.sonar.ce.task.projectanalysis.component.Component.Type.FILE;
import static org.sonar.ce.task.projectanalysis.component.Component.Type.PROJECT;
import static org.sonar.ce.task.projectanalysis.component.ReportComponent.builder;
import static org.sonar.db.event.EventDto.CATEGORY_ALERT;
import static org.sonar.db.event.EventDto.CATEGORY_PROFILE;
import static org.sonar.db.event.EventDto.CATEGORY_VERSION;

public class PersistEventsStepTest extends BaseStepTest {

  private static final long NOW = 1225630680000L;
  private static final ReportComponent ROOT = builder(PROJECT, 1)
    .setUuid("ABCD")
    .setProjectVersion("version_1")
    .addChildren(
      builder(DIRECTORY, 2)
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
  private static final String ANALYSIS_UUID = "uuid_1";

  System2 system2 = mock(System2.class);

  @Rule
  public DbTester dbTester = DbTester.create(system2);
  @Rule
  public AnalysisMetadataHolderRule analysisMetadataHolder = new AnalysisMetadataHolderRule();
  @Rule
  public TreeRootHolderRule treeRootHolder = new TreeRootHolderRule();

  private Date someDate = new Date(150000000L);

  private EventRepository eventRepository = mock(EventRepository.class);
  private UuidFactory uuidFactory = UuidFactoryImpl.INSTANCE;

  private PersistEventsStep underTest;

  @Before
  public void setup() {
    analysisMetadataHolder.setAnalysisDate(someDate.getTime()).setUuid(ANALYSIS_UUID);
    underTest = new PersistEventsStep(dbTester.getDbClient(), system2, treeRootHolder, analysisMetadataHolder, eventRepository, uuidFactory);
    when(eventRepository.getEvents(any(Component.class))).thenReturn(Collections.emptyList());
  }

  @Override
  protected ComputationStep step() {
    return underTest;
  }

  @Test
  public void create_version_event() {
    when(system2.now()).thenReturn(NOW);
    Component project = builder(PROJECT, 1)
      .setUuid("ABCD")
      .setProjectVersion("1.0")
      .addChildren(
        builder(DIRECTORY, 2)
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

    underTest.execute(new TestComputationStepContext());

    assertThat(dbTester.countRowsOfTable(dbTester.getSession(), "events")).isEqualTo(1);
    List<EventDto> eventDtos = dbTester.getDbClient().eventDao().selectByComponentUuid(dbTester.getSession(), ROOT.getUuid());
    assertThat(eventDtos).hasSize(1);
    EventDto eventDto = eventDtos.iterator().next();
    assertThat(eventDto.getComponentUuid()).isEqualTo(ROOT.getUuid());
    assertThat(eventDto.getName()).isEqualTo("1.0");
    assertThat(eventDto.getDescription()).isNull();
    assertThat(eventDto.getCategory()).isEqualTo(CATEGORY_VERSION);
    assertThat(eventDto.getData()).isNull();
    assertThat(eventDto.getDate()).isEqualTo(analysisMetadataHolder.getAnalysisDate());
    assertThat(eventDto.getCreatedAt()).isEqualTo(NOW);
  }

  @Test
  public void persist_alert_events_on_root() {
    when(system2.now()).thenReturn(NOW);
    treeRootHolder.setRoot(ROOT);
    Event alert = Event.createAlert("Red (was Orange)", null, "Open issues > 0");
    when(eventRepository.getEvents(ROOT)).thenReturn(ImmutableList.of(alert));

    underTest.execute(new TestComputationStepContext());

    assertThat(dbTester.countRowsOfTable(dbTester.getSession(), "events")).isEqualTo(2);
    List<EventDto> eventDtos = dbTester.getDbClient().eventDao().selectByComponentUuid(dbTester.getSession(), ROOT.getUuid());
    assertThat(eventDtos)
      .extracting(EventDto::getCategory)
      .containsOnly(CATEGORY_ALERT, CATEGORY_VERSION);
    EventDto eventDto = eventDtos.stream().filter(t -> CATEGORY_ALERT.equals(t.getCategory())).findAny().get();
    assertThat(eventDto.getComponentUuid()).isEqualTo(ROOT.getUuid());
    assertThat(eventDto.getName()).isEqualTo(alert.getName());
    assertThat(eventDto.getDescription()).isEqualTo(alert.getDescription());
    assertThat(eventDto.getCategory()).isEqualTo(CATEGORY_ALERT);
    assertThat(eventDto.getData()).isNull();
    assertThat(eventDto.getDate()).isEqualTo(analysisMetadataHolder.getAnalysisDate());
    assertThat(eventDto.getCreatedAt()).isEqualTo(NOW);
  }

  @Test
  public void persist_profile_events_on_root() {
    when(system2.now()).thenReturn(NOW);
    treeRootHolder.setRoot(ROOT);
    Event profile = Event.createProfile("foo", null, "bar");
    when(eventRepository.getEvents(ROOT)).thenReturn(ImmutableList.of(profile));

    underTest.execute(new TestComputationStepContext());

    assertThat(dbTester.countRowsOfTable(dbTester.getSession(), "events")).isEqualTo(2);
    List<EventDto> eventDtos = dbTester.getDbClient().eventDao().selectByComponentUuid(dbTester.getSession(), ROOT.getUuid());
    assertThat(eventDtos)
      .extracting(EventDto::getCategory)
      .containsOnly(CATEGORY_PROFILE, CATEGORY_VERSION);
    EventDto eventDto = eventDtos.stream().filter(t -> CATEGORY_PROFILE.equals(t.getCategory())).findAny().get();
    assertThat(eventDto.getComponentUuid()).isEqualTo(ROOT.getUuid());
    assertThat(eventDto.getName()).isEqualTo(profile.getName());
    assertThat(eventDto.getDescription()).isEqualTo(profile.getDescription());
    assertThat(eventDto.getCategory()).isEqualTo(EventDto.CATEGORY_PROFILE);
    assertThat(eventDto.getData()).isNull();
    assertThat(eventDto.getDate()).isEqualTo(analysisMetadataHolder.getAnalysisDate());
    assertThat(eventDto.getCreatedAt()).isEqualTo(NOW);
  }

  @Test
  public void keep_one_event_by_version() {
    ComponentDto projectDto = dbTester.components().insertPublicProject();
    EventDto[] existingEvents = new EventDto[] {
      dbTester.events().insertEvent(newVersionEventDto(projectDto, 120_000_000L, "1.3-SNAPSHOT")),
      dbTester.events().insertEvent(newVersionEventDto(projectDto, 130_000_000L, "1.4")),
      dbTester.events().insertEvent(newVersionEventDto(projectDto, 140_000_000L, "1.5-SNAPSHOT"))
    };

    Component project = builder(PROJECT, 1)
      .setUuid(projectDto.uuid())
      .setProjectVersion("1.5-SNAPSHOT")
      .addChildren(
        builder(DIRECTORY, 2)
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

    underTest.execute(new TestComputationStepContext());

    assertThat(dbTester.countRowsOfTable(dbTester.getSession(), "events")).isEqualTo(3);
    List<EventDto> eventDtos = dbTester.getDbClient().eventDao().selectByComponentUuid(dbTester.getSession(), projectDto.uuid());
    assertThat(eventDtos).hasSize(3);
    assertThat(eventDtos)
      .extracting(EventDto::getName)
      .containsOnly("1.3-SNAPSHOT", "1.4", "1.5-SNAPSHOT");
    assertThat(eventDtos)
      .extracting(EventDto::getUuid)
      .contains(existingEvents[0].getUuid(), existingEvents[1].getUuid())
      .doesNotContain(existingEvents[2].getUuid());
  }

  private EventDto newVersionEventDto(ComponentDto project, long date, String name) {
    return new EventDto().setUuid(uuidFactory.create()).setComponentUuid(project.uuid())
      .setAnalysisUuid("analysis_uuid")
      .setCategory(CATEGORY_VERSION)
      .setName(name).setDate(date).setCreatedAt(date);
  }

}
