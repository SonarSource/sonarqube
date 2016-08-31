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
package org.sonar.api.resources;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ProjectTest {

  @Test
  public void effectiveKeyShouldEqualKey() {
    assertThat(new Project("my:project").getEffectiveKey()).isEqualTo("my:project");
  }

  @Test
  public void createFromMavenIds() {
    Project project = Project.createFromMavenIds("my", "artifact");

    assertThat(project.getKey()).isEqualTo("my:artifact");
  }
  
  @Test
  public void setNameWithBranch() {
    Project project = new Project("key", "branch", "name");
    assertThat(project.getName()).isEqualTo("name branch");
    assertThat(project.getOriginalName()).isEqualTo("name branch");

    project.setOriginalName("Project1");
    assertThat(project.getOriginalName()).isEqualTo("Project1 branch");
  }
  
  @Test
  public void setNameWithoutBranch() {
    Project project = new Project("key", null, "name");
    assertThat(project.getName()).isEqualTo("name");
    assertThat(project.getOriginalName()).isEqualTo("name");

    project.setOriginalName("Project1");
    assertThat(project.getOriginalName()).isEqualTo("Project1");
  }

}
