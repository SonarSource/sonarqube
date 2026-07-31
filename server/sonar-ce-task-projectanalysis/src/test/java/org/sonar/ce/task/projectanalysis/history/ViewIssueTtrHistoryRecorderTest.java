/*
 * SonarQube
 * Copyright (C) SonarSource Sàrl
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
package org.sonar.ce.task.projectanalysis.history;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.sonar.ce.task.projectanalysis.component.Component;
import org.sonar.ce.task.projectanalysis.component.TreeRootHolder;
import org.sonar.ce.task.projectanalysis.component.ViewAttributes;
import org.sonarsource.history.model.EntityType;
import org.sonarsource.history.server.service.IssueTtrHistoryRecordingService;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ViewIssueTtrHistoryRecorderTest {

  private final TreeRootHolder treeRootHolder = mock();
  private final IssueTtrHistoryRecordingService issueTtrHistoryRecordingService = mock();

  private final ViewIssueTtrHistoryRecorder underTest = new ViewIssueTtrHistoryRecorder(treeRootHolder, issueTtrHistoryRecordingService);

  @ParameterizedTest
  @CsvSource(textBlock = """
    APPLICATION,APPLICATION
    PORTFOLIO,PORTFOLIO
    """)
  void recordTtrHistory_shouldCallHistoryRecordingServiceWithCorrectEntityType(ViewAttributes.Type viewType, EntityType expectedEntityType) {
    String entityUuid = "entity-uuid";
    Component component = mock();
    when(treeRootHolder.getRoot()).thenReturn(component);
    when(component.getViewAttributes()).thenReturn(new ViewAttributes(viewType));

    underTest.recordTtrHistory(entityUuid);

    verify(issueTtrHistoryRecordingService).recordIssueTtrHistoryForAggregation(entityUuid, expectedEntityType);
  }
}