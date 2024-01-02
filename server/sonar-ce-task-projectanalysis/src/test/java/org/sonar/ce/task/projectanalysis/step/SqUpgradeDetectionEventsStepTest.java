/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.sonar.api.utils.Version;
import org.sonar.ce.task.projectanalysis.component.Component;
import org.sonar.ce.task.projectanalysis.component.ReportComponent;
import org.sonar.ce.task.projectanalysis.component.TreeRootHolderRule;
import org.sonar.ce.task.projectanalysis.event.Event;
import org.sonar.ce.task.projectanalysis.event.EventRepository;
import org.sonar.ce.task.step.TestComputationStepContext;
import org.sonar.core.platform.SonarQubeVersion;
import org.sonar.db.DbClient;
import org.sonar.db.event.EventDto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class SqUpgradeDetectionEventsStepTest {

  @Rule
  public TreeRootHolderRule treeRootHolder = new TreeRootHolderRule();

  private final DbClient dbClient = mock();
  private final EventRepository eventRepository = mock();

  private final ArgumentCaptor<Event> eventArgumentCaptor = ArgumentCaptor.forClass(Event.class);

  private final SonarQubeVersion sonarQubeVersion = mock();

  private final SqUpgradeDetectionEventsStep underTest = new SqUpgradeDetectionEventsStep(treeRootHolder, dbClient, eventRepository, sonarQubeVersion);

  @Before
  public void setUp() {
    treeRootHolder.setRoot(ReportComponent.builder(Component.Type.PROJECT, 1).setUuid("uuid").setKey("key").build());
  }

  @Test
  public void givenNoPreviousUpgradeEvents_whenStepIsExecuted_thenANewUpgradeEventIsCreated() {
    when(sonarQubeVersion.get()).thenReturn(Version.parse("10.3"));
    when(dbClient.eventDao()).thenReturn(mock());
    when(dbClient.eventDao().selectSqUpgradesByMostRecentFirst(any(), any())).thenReturn(Collections.emptyList());

    underTest.execute(new TestComputationStepContext());

    verify(eventRepository, times(1)).add(eventArgumentCaptor.capture());
    verifyNoMoreInteractions(eventRepository);

    assertThat(eventArgumentCaptor.getAllValues())
      .extracting(Event::getCategory, Event::getName)
      .containsExactly(tuple(Event.Category.SQ_UPGRADE, "10.3"));
  }

  @Test
  public void givenUpgradeEventWithTheSameSqVersion_whenStepIsExecuted_thenNothingIsPersisted() {
    when(sonarQubeVersion.get()).thenReturn(Version.parse("10.3"));
    when(dbClient.eventDao()).thenReturn(mock());
    when(dbClient.eventDao().selectSqUpgradesByMostRecentFirst(any(), any())).thenReturn(getUpgradeEvents("10.3", "10.2"));

    underTest.execute(new TestComputationStepContext());

    verifyNoMoreInteractions(eventRepository);
  }

  private List<EventDto> getUpgradeEvents(String... versions) {
    return Arrays.stream(versions)
      .map(version -> new EventDto().setCategory(EventDto.CATEGORY_SQ_UPGRADE).setName(version))
      .toList();
  }

  @Test
  public void givenUpgradeEventWithDifferentSqVersion_whenStepIsExecuted_thenANewUpgradeEventIsCreated() {
    when(sonarQubeVersion.get()).thenReturn(Version.parse("10.3"));
    when(dbClient.eventDao()).thenReturn(mock());
    when(dbClient.eventDao().selectSqUpgradesByMostRecentFirst(any(), any())).thenReturn(getUpgradeEvents("10.2", "10.1"));

    underTest.execute(new TestComputationStepContext());

    verify(eventRepository, times(1)).add(eventArgumentCaptor.capture());
    verifyNoMoreInteractions(eventRepository);

    assertThat(eventArgumentCaptor.getAllValues())
      .extracting(Event::getCategory, Event::getName)
      .containsExactly(tuple(Event.Category.SQ_UPGRADE, "10.3"));
  }

  @Test
  public void whenGetDescriptionIsCalled_shouldReturnExpectedValue() {
    assertThat(underTest.getDescription()).isEqualTo("Generate SQ Upgrade analysis events");
  }

}
