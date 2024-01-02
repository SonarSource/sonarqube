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
package org.sonar.ce.task.projectexport.steps;

import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.utils.MessageException;
import org.sonar.api.utils.System2;
import org.sonar.ce.task.projectexport.taskprocessor.ProjectDescriptor;
import org.sonar.ce.task.step.TestComputationStepContext;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class LoadProjectStepTest {

  private static final String PROJECT_KEY = "project_key";

  @Rule
  public DbTester dbTester = DbTester.create(System2.INSTANCE);

  private final ProjectDescriptor descriptor = new ProjectDescriptor("project_uuid", PROJECT_KEY, "Project Name");
  private final MutableProjectHolder definitionHolder = new MutableProjectHolderImpl();
  private final LoadProjectStep underTest = new LoadProjectStep(descriptor, definitionHolder, dbTester.getDbClient());

  @Test
  public void fails_if_project_does_not_exist() {
    assertThatThrownBy(() -> underTest.execute(new TestComputationStepContext()))
      .isInstanceOf(MessageException.class)
      .hasMessage("Project with key [project_key] does not exist");
  }

  @Test
  public void fails_if_component_is_not_a_project() {
    // insert a module, but not a project
    dbTester.executeInsert("projects",
      "kee", PROJECT_KEY,
      "qualifier", Qualifiers.APP,
      "uuid", "not_used",
      "private", false,
      "created_at", 1L,
      "updated_at", 1L);

    assertThatThrownBy(() -> underTest.execute(new TestComputationStepContext()))
      .isInstanceOf(MessageException.class)
      .hasMessage("Project with key [project_key] does not exist");
  }

  @Test
  public void registers_project_if_valid() {
    ComponentDto project = dbTester.components().insertPublicProject(c -> c.setKey(PROJECT_KEY));
    underTest.execute(new TestComputationStepContext());
    assertThat(definitionHolder.projectDto().getKey()).isEqualTo(project.getKey());
  }

  @Test
  public void getDescription_is_defined() {
    assertThat(underTest.getDescription()).isNotEmpty();
  }
}
