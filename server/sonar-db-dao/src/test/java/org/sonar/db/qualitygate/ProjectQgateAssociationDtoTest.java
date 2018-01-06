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
package org.sonar.db.qualitygate;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ProjectQgateAssociationDtoTest {

  @Test
  public void to_assoc_with_project_having_assoc() {
    ProjectQgateAssociation project = new ProjectQgateAssociationDto()
      .setId(1L)
      .setName("polop")
      .setGateId("10")
      .toQgateAssociation();

    assertThat(project.id()).isEqualTo(1);
    assertThat(project.name()).isEqualTo("polop");
    assertThat(project.isMember()).isTrue();
  }

  @Test
  public void to_assoc_with_project_not_having_assoc() {
    ProjectQgateAssociation project = new ProjectQgateAssociationDto()
      .setId(1L)
      .setName("polop")
      .setGateId(null)
      .toQgateAssociation();

    assertThat(project.id()).isEqualTo(1);
    assertThat(project.name()).isEqualTo("polop");
    assertThat(project.isMember()).isFalse();
  }

}
