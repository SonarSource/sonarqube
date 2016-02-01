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

package org.sonar.server.component.ws;

import java.util.Collections;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.SnapshotDto;

import static com.google.common.collect.Lists.newArrayList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.db.component.ComponentTesting.newProjectDto;
import static org.sonar.db.component.SnapshotTesting.newSnapshotForProject;

public class ShowDataTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  ComponentDto project = newProjectDto().setId(42L);

  ShowData underTest;

  @Test
  public void no_ancestors() {
    underTest = ShowData.builder(
      newSnapshotForProject(project).setPath(null))
      .withAncestorsSnapshots(Collections.<SnapshotDto>emptyList())
      .andAncestorComponents(Collections.<ComponentDto>emptyList());

    assertThat(underTest.getComponents()).isEmpty();
  }

  @Test
  public void fail_when_inconsistent_snapshot_ancestors_data() {
    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("Missing ancestor");

    underTest = ShowData.builder(
      newSnapshotForProject(project).setPath("1.2.3."))
      .withAncestorsSnapshots(newArrayList(
        newSnapshotForProject(project).setId(1L),
        newSnapshotForProject(project).setId(2L)))
      // missing snapshot with id = 3
      .andAncestorComponents(Collections.<ComponentDto>emptyList());
  }

  @Test
  public void fail_when_inconsistent_component_ancestors_data() {
    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("Missing ancestor");

    underTest = ShowData.builder(
      newSnapshotForProject(project).setPath("1.2.3."))
      .withAncestorsSnapshots(newArrayList(
        newSnapshotForProject(project).setId(1L),
        newSnapshotForProject(project).setId(2L),
        newSnapshotForProject(project).setId(3L)))
      .andAncestorComponents(Collections.<ComponentDto>emptyList());
  }
}
