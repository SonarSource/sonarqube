/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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
package org.sonar.ce.task.projectanalysis.issue;

import java.util.Collections;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sonar.ce.task.projectanalysis.component.BranchComponentUuidsDelegate;
import org.sonar.ce.task.projectanalysis.component.Component;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.core.issue.tracking.Input;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ComponentTesting;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TrackerReferenceBranchInputFactoryIT {
  private final static String COMPONENT_KEY = "file1";
  private final static String COMPONENT_UUID = "uuid1";

  @RegisterExtension
  private final DbTester db = DbTester.create();

  private final ComponentIssuesLoader componentIssuesLoader = mock(ComponentIssuesLoader.class);
  private final BranchComponentUuidsDelegate referenceBranchComponentUuids = mock(BranchComponentUuidsDelegate.class);
  private TrackerReferenceBranchInputFactory underTest;

  @BeforeEach
  void setUp() {
    underTest = new TrackerReferenceBranchInputFactory(componentIssuesLoader, referenceBranchComponentUuids, db.getDbClient());
  }

  @Test
  void gets_issues_and_hashes_in_matching_component() {
    DefaultIssue issue1 = new DefaultIssue();
    when(referenceBranchComponentUuids.getComponentUuid(COMPONENT_KEY)).thenReturn(COMPONENT_UUID);
    when(componentIssuesLoader.loadOpenIssuesWithChanges(COMPONENT_UUID)).thenReturn(Collections.singletonList(issue1));
    ComponentDto fileDto = ComponentTesting.newFileDto(ComponentTesting.newPublicProjectDto()).setUuid(COMPONENT_UUID);
    db.fileSources().insertFileSource(fileDto, 3);

    Component component = mock(Component.class);
    when(component.getKey()).thenReturn(COMPONENT_KEY);
    when(component.getType()).thenReturn(Component.Type.FILE);
    Input<DefaultIssue> input = underTest.create(component);

    assertThat(input.getIssues()).containsOnly(issue1);
    assertThat(input.getLineHashSequence().length()).isEqualTo(3);
  }

  @Test
  void gets_nothing_when_there_is_no_matching_component() {
    Component component = mock(Component.class);
    when(component.getKey()).thenReturn(COMPONENT_KEY);
    when(component.getType()).thenReturn(Component.Type.FILE);
    Input<DefaultIssue> input = underTest.create(component);

    assertThat(input.getIssues()).isEmpty();
    assertThat(input.getLineHashSequence().length()).isZero();
  }
}
