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
package org.sonar.server.project;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static java.util.Collections.emptyList;
import static org.apache.commons.lang.RandomStringUtils.randomAlphanumeric;
import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.db.component.ComponentTesting.newPrivateProjectDto;
import static org.sonar.db.organization.OrganizationTesting.newOrganizationDto;

public class RekeyedProjectTest {
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Test
  public void constructor_throws_NPE_if_project_is_null() {
    expectedException.expect(NullPointerException.class);
    expectedException.expectMessage("project can't be null");

    new RekeyedProject(null, randomAlphanumeric(3));
  }

  @Test
  public void constructor_throws_NPE_if_previousKey_is_null() {
    expectedException.expect(NullPointerException.class);
    expectedException.expectMessage("previousKey can't be null");

    new RekeyedProject(newRandomProject(), null);
  }

  @Test
  public void verify_getters() {
    Project project = newRandomProject();
    String previousKey = randomAlphanumeric(6);
    RekeyedProject underTest = new RekeyedProject(project, previousKey);

    assertThat(underTest.getProject()).isSameAs(project);
    assertThat(underTest.getPreviousKey()).isEqualTo(previousKey);
  }

  @Test
  public void equals_is_based_on_project_and_previousKey() {
    Project project = newRandomProject();
    String previousKey = randomAlphanumeric(6);
    RekeyedProject underTest = new RekeyedProject(project, previousKey);

    assertThat(underTest).isEqualTo(underTest);
    assertThat(underTest).isEqualTo(new RekeyedProject(project, previousKey));
    assertThat(underTest).isNotEqualTo(new RekeyedProject(project, randomAlphanumeric(11)));
    assertThat(underTest).isNotEqualTo(new RekeyedProject(newRandomProject(), previousKey));
    assertThat(underTest).isNotEqualTo(new Object());
    assertThat(underTest).isNotEqualTo(null);
  }

  @Test
  public void hashCode_is_based_on_project_and_previousKey() {
    Project project = newRandomProject();
    String previousKey = randomAlphanumeric(6);
    RekeyedProject underTest = new RekeyedProject(project, previousKey);

    assertThat(underTest.hashCode()).isEqualTo(underTest.hashCode());
    assertThat(underTest.hashCode()).isEqualTo(new RekeyedProject(project, previousKey).hashCode());
    assertThat(underTest.hashCode()).isNotEqualTo(new RekeyedProject(project, randomAlphanumeric(11)).hashCode());
    assertThat(underTest.hashCode()).isNotEqualTo(new RekeyedProject(newRandomProject(), previousKey).hashCode());
    assertThat(underTest.hashCode()).isNotEqualTo(new Object().hashCode());
    assertThat(underTest.hashCode()).isNotEqualTo(null);
  }

  @Test
  public void verify_toString() {
    Project project = new Project("A", "B", "C", "D", emptyList());
    String previousKey = "E";
    RekeyedProject underTest = new RekeyedProject(project, previousKey);

    assertThat(underTest.toString()).isEqualTo("RekeyedProject{project=Project{uuid='A', key='B', name='C', description='D'}, previousKey='E'}");
  }

  private static Project newRandomProject() {
    return Project.from(newPrivateProjectDto(newOrganizationDto()));
  }
}
