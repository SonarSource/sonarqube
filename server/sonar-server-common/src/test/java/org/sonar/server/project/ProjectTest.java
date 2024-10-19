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
import org.sonar.db.entity.EntityDto;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ProjectTest {
  @Test
  public void test_bean_without_description() {
    Project project1 = new Project("U1", "K1", "N1", null, emptyList());
    Project project2 = new Project("U1", "K1", "N1", null, emptyList());

    assertThat(project1.getUuid()).isEqualTo(project2.getUuid()).isEqualTo("U1");
    assertThat(project1.getKey()).isEqualTo(project2.getKey()).isEqualTo("K1");
    assertThat(project1.getName()).isEqualTo(project2.getName()).isEqualTo("N1");
    assertThat(project1.getDescription()).isEqualTo(project2.getDescription()).isNull();

    assertThat(project1.toString())
      .isEqualTo(project2.toString())
      .isEqualTo("Project{uuid='U1', key='K1', name='N1', description=null}");
  }

  @Test
  public void test_bean_with_description() {
    Project project1 = new Project("U1", "K1", "N1", "D1", emptyList());

    assertThat(project1.getUuid()).isEqualTo("U1");
    assertThat(project1.getKey()).isEqualTo("K1");
    assertThat(project1.getName()).isEqualTo("N1");
    assertThat(project1.getDescription()).isEqualTo("D1");

    assertThat(project1.toString())
      .isEqualTo(project1.toString())
      .isEqualTo("Project{uuid='U1', key='K1', name='N1', description='D1'}");
  }

  @Test
  public void from_whenPortfolioPassed_shouldNotReturnNullFields() {
    EntityDto entity = mock(EntityDto.class);

    when(entity.getUuid()).thenReturn("U1");
    when(entity.getKey()).thenReturn("K1");
    when(entity.getName()).thenReturn("N1");
    when(entity.getDescription()).thenReturn("D1");

    Project underTest = Project.from(entity);

    assertThat(underTest.getUuid()).isEqualTo("U1");
    assertThat(underTest.getKey()).isEqualTo("K1");
    assertThat(underTest.getName()).isEqualTo("N1");
    assertThat(underTest.getDescription()).isEqualTo("D1");

    assertThat(underTest.toString())
      .isEqualTo(underTest.toString())
      .isEqualTo("Project{uuid='U1', key='K1', name='N1', description='D1'}");
  }

  @Test
  public void test_equals_and_hashCode() {
    Project project1 = new Project("U1", "K1", "N1", null, emptyList());
    Project project2 = new Project("U1", "K1", "N1", "D1", emptyList());

    assertThat(project1).isEqualTo(project1)
      .isNotNull()
      .isNotEqualTo(new Object())
      .isEqualTo(new Project("U1", "K1", "N1", null, emptyList()))
      .isNotEqualTo(new Project("U1", "K2", "N1", null, emptyList()))
      .isNotEqualTo(new Project("U1", "K1", "N2", null, emptyList()))
      .isEqualTo(project2);

    assertThat(project1).hasSameHashCodeAs(project1);
    assertThat(project1.hashCode()).isNotEqualTo(new Object().hashCode());
    assertThat(project1).hasSameHashCodeAs(new Project("U1", "K1", "N1", null, emptyList()));
    assertThat(project1.hashCode()).isNotEqualTo(new Project("U1", "K2", "N1", null, emptyList()).hashCode());
    assertThat(project1.hashCode()).isNotEqualTo(new Project("U1", "K1", "N2", null, emptyList()).hashCode());
    assertThat(project1).hasSameHashCodeAs(project2);
  }

}
