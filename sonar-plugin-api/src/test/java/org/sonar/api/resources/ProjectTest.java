/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
package org.sonar.api.resources;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;
import org.sonar.api.CoreProperties;
import org.sonar.api.batch.bootstrap.ImmutableProjectDefinition;
import org.sonar.api.batch.bootstrap.ProjectDefinition;

public class ProjectTest {
  @Test
  public void effectiveKeyShouldEqualKeyWithBranch() {

    ImmutableProjectDefinition definition = ProjectDefinition.create()
      .setKey("mykey")
      .setProperty(CoreProperties.PROJECT_BRANCH_PROPERTY, "branch")
      .build();
    assertThat(new Project(definition).getEffectiveKey()).isEqualTo("mykey:branch");
    assertThat(new Project(definition).getKey()).isEqualTo("mykey");
  }

  @Test
  public void setNameWithBranch() {
    ImmutableProjectDefinition definition = ProjectDefinition.create()
      .setProperty(CoreProperties.PROJECT_BRANCH_PROPERTY, "branch")
      .setKey("key")
      .setName("name")
      .build();
    Project project = new Project(definition);
    assertThat(project.getName()).isEqualTo("name branch");
    assertThat(project.getOriginalName()).isEqualTo("name branch");
  }

  @Test
  public void setNameWithoutBranch() {
    ImmutableProjectDefinition definition = ProjectDefinition.create()
      .setKey("key")
      .setName("name")
      .build();
    Project project = new Project(definition);
    assertThat(project.getName()).isEqualTo("name");
    assertThat(project.getOriginalName()).isEqualTo("name");
  }
}
