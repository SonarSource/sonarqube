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
package org.sonar.server.computation.task.projectanalysis.analysis;

import org.junit.Test;
import org.sonar.server.computation.task.projectanalysis.component.Component;
import org.sonar.server.computation.task.projectanalysis.component.ReportComponent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.server.computation.task.projectanalysis.component.Component.Type.PROJECT;

public class ProjectTest {
  @Test
  public void test_bean() {
    Project project = new Project("U1", "K1", "N1");

    assertThat(project.getUuid()).isEqualTo("U1");
    assertThat(project.getKey()).isEqualTo("K1");
    assertThat(project.getName()).isEqualTo("N1");

    assertThat(project.toString()).isEqualTo("Project{uuid='U1', key='K1', name='N1'}");
  }

  @Test
  public void test_equals_and_hashCode() {
    Project project1 = new Project("U1", "K1", "N1");
    Project project1bis = new Project("U1", "K1", "N1");
    Project project2 = new Project("U2", "K2", project1.getName() /* same name */);

    assertThat(project1.equals(project1)).isTrue();
    assertThat(project1.equals(project1bis)).isTrue();
    assertThat(project1.equals(project2)).isFalse();
    assertThat(project1.equals("U1")).isFalse();

    assertThat(project1.hashCode()).isEqualTo(project1.hashCode());
    assertThat(project1.hashCode()).isEqualTo(project1bis.hashCode());
  }

  @Test
  public void test_copyOf() {
    Component root = ReportComponent.builder(PROJECT, 1).setKey("ROOT").build();

    Project project = Project.copyOf(root);
    assertThat(project.getUuid()).isEqualTo(root.getUuid()).isNotNull();
    assertThat(project.getKey()).isEqualTo(root.getKey()).isNotNull();
    assertThat(project.getName()).isEqualTo(root.getName()).isNotNull();
  }
}
