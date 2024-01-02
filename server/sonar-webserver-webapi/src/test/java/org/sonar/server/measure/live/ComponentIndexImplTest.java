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
package org.sonar.server.measure.live;

import java.util.List;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ComponentTesting;

import static org.assertj.core.api.Assertions.assertThat;

public class ComponentIndexImplTest {
  @Rule
  public DbTester db = DbTester.create();
  private final ComponentIndexImpl componentIndex = new ComponentIndexImpl(db.getDbClient());

  private ComponentDto project;
  private ComponentDto dir1;
  private ComponentDto dir2;
  private ComponentDto file11;
  private ComponentDto file12;
  private ComponentDto file21;

  private ComponentDto branch;
  private ComponentDto branchDir1;
  private ComponentDto branchDir2;
  private ComponentDto branchFile11;
  private ComponentDto branchFile12;
  private ComponentDto branchFile21;

  @Before
  public void setUp() {
    project = db.components().insertPrivateProject();
    dir1 = db.components().insertComponent(ComponentTesting.newDirectory(project, "src/main/java"));
    dir2 = db.components().insertComponent(ComponentTesting.newDirectory(project, "src/main/java2"));
    file11 = db.components().insertComponent(ComponentTesting.newFileDto(project, dir1));
    file12 = db.components().insertComponent(ComponentTesting.newFileDto(project, dir1));
    file21 = db.components().insertComponent(ComponentTesting.newFileDto(project, dir2));

    branch = db.components().insertProjectBranch(project);
    branchDir1 = db.components().insertComponent(ComponentTesting.newDirectory(branch, "src/main/java"));
    branchDir2 = db.components().insertComponent(ComponentTesting.newDirectory(branch, "src/main/java2"));
    branchFile11 = db.components().insertComponent(ComponentTesting.newFileDto(branch, branchDir1));
    branchFile12 = db.components().insertComponent(ComponentTesting.newFileDto(branch, branchDir1));
    branchFile21 = db.components().insertComponent(ComponentTesting.newFileDto(branch, branchDir2));
  }

  @Test
  public void loads_all_necessary_components() {
    componentIndex.load(db.getSession(), List.of(file11));
    assertThat(componentIndex.getSortedTree()).containsExactly(file11, dir1, project);
    assertThat(componentIndex.getBranch()).isEqualTo(project);
    assertThat(componentIndex.getAllUuids()).containsOnly(project.uuid(), dir1.uuid(), dir2.uuid(), file11.uuid(), file12.uuid());
    assertThat(componentIndex.getChildren(dir1)).containsOnly(file11, file12);
  }


  @Test
  public void loads_all_necessary_components_for_root() {
    componentIndex.load(db.getSession(), List.of(project));
    assertThat(componentIndex.getSortedTree()).containsExactly(project);
    assertThat(componentIndex.getBranch()).isEqualTo(project);
    assertThat(componentIndex.getAllUuids()).containsOnly(project.uuid(), dir1.uuid(), dir2.uuid());
    assertThat(componentIndex.getChildren(dir1)).isEmpty();
    assertThat(componentIndex.getChildren(project)).containsOnly(dir1, dir2);
  }

  @Test
  public void loads_all_necessary_components_from_branch() {
    componentIndex.load(db.getSession(), List.of(branchDir1));
    assertThat(componentIndex.getSortedTree()).containsExactly(branchDir1, branch);
    assertThat(componentIndex.getBranch()).isEqualTo(branch);
    assertThat(componentIndex.getAllUuids()).containsOnly(branch.uuid(), branchDir1.uuid(), branchDir2.uuid(), branchFile11.uuid(), branchFile12.uuid());
    assertThat(componentIndex.getChildren(branchDir1)).containsOnly(branchFile11, branchFile12);
  }
}
