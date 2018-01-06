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
package org.sonar.server.computation.task.projectanalysis.issue;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.sonar.server.computation.task.projectanalysis.component.Component;
import org.sonar.server.computation.task.projectanalysis.filemove.MovedFilesRepository;
import org.sonar.server.computation.task.projectanalysis.filemove.MovedFilesRepository.OriginalFile;

import com.google.common.base.Optional;

public class RemoveProcessedComponentsVisitorTest {
  private static final String UUID = "uuid";
  private ComponentsWithUnprocessedIssues componentsWithUnprocessedIssues = mock(ComponentsWithUnprocessedIssues.class);
  private MovedFilesRepository movedFilesRepository = mock(MovedFilesRepository.class);
  private Component component = mock(Component.class);
  private RemoveProcessedComponentsVisitor underTest = new RemoveProcessedComponentsVisitor(componentsWithUnprocessedIssues, movedFilesRepository);

  @Before
  public void setUp() {
    when(component.getUuid()).thenReturn(UUID);
  }

  @Test
  public void remove_processed_files() {
    when(movedFilesRepository.getOriginalFile(any(Component.class))).thenReturn(Optional.absent());
    underTest.afterComponent(component);

    verify(movedFilesRepository).getOriginalFile(component);
    verify(componentsWithUnprocessedIssues).remove(UUID);
    verifyNoMoreInteractions(componentsWithUnprocessedIssues);
  }

  @Test
  public void also_remove_moved_files() {
    String uuid2 = "uuid2";
    OriginalFile movedFile = new OriginalFile(0, uuid2, "key");
    when(movedFilesRepository.getOriginalFile(any(Component.class))).thenReturn(Optional.of(movedFile));

    underTest.afterComponent(component);

    verify(movedFilesRepository).getOriginalFile(component);
    verify(componentsWithUnprocessedIssues).remove(UUID);
    verify(componentsWithUnprocessedIssues).remove(uuid2);

    verifyNoMoreInteractions(componentsWithUnprocessedIssues);
  }
}
