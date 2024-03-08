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
package org.sonar.server.project;

import org.junit.Test;

import static java.util.Collections.emptyList;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.sonar.db.component.ComponentTesting.newPrivateProjectDto;

public class RekeyedProjectTest {

  @Test
  public void constructor_throws_NPE_if_project_is_null() {
    assertThatThrownBy(() -> new RekeyedProject(null, randomAlphanumeric(3)))
      .isInstanceOf(NullPointerException.class)
      .hasMessage("project can't be null");
  }

  @Test
  public void constructor_throws_NPE_if_previousKey_is_null() {
    assertThatThrownBy(() -> new RekeyedProject(newRandomProject(), null))
      .isInstanceOf(NullPointerException.class)
      .hasMessage("previousKey can't be null");
  }

  @Test
  public void verify_getters() {
    Project project = newRandomProject();
    String previousKey = randomAlphanumeric(6);
    RekeyedProject underTest = new RekeyedProject(project, previousKey);

    assertThat(underTest.project()).isSameAs(project);
    assertThat(underTest.previousKey()).isEqualTo(previousKey);
  }

  @Test
  public void equals_is_based_on_project_and_previousKey() {
    Project project = newRandomProject();
    String previousKey = randomAlphanumeric(6);
    RekeyedProject underTest = new RekeyedProject(project, previousKey);

    assertThat(underTest)
      .isEqualTo(underTest)
      .isEqualTo(new RekeyedProject(project, previousKey))
      .isNotEqualTo(new RekeyedProject(project, randomAlphanumeric(11)))
      .isNotEqualTo(new RekeyedProject(newRandomProject(), previousKey))
      .isNotEqualTo(new Object())
      .isNotNull();
  }

  @Test
  public void hashCode_is_based_on_project_and_previousKey() {
    Project project = newRandomProject();
    String previousKey = randomAlphanumeric(6);
    RekeyedProject underTest = new RekeyedProject(project, previousKey);

    assertThat(underTest)
      .hasSameHashCodeAs(underTest)
      .hasSameHashCodeAs(new RekeyedProject(project, previousKey));
    assertThat(underTest.hashCode())
      .isNotEqualTo(new RekeyedProject(project, randomAlphanumeric(11)).hashCode())
      .isNotEqualTo(new RekeyedProject(newRandomProject(), previousKey).hashCode())
      .isNotEqualTo(new Object().hashCode());
  }

  @Test
  public void verify_toString() {
    Project project = new Project("A", "B", "C", "D", emptyList());
    String previousKey = "E";
    RekeyedProject underTest = new RekeyedProject(project, previousKey);

    assertThat(underTest).hasToString("RekeyedProject{project=Project{uuid='A', key='B', name='C', description='D'}, previousKey='E'}");
  }

  private static Project newRandomProject() {
    return Project.from(newPrivateProjectDto());
  }
}
