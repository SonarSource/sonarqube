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
package org.sonar.db.component;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.db.component.ComponentTesting.newPrivateProjectDto;
import static org.sonar.db.component.ComponentTreeQuery.Strategy.CHILDREN;
import static org.sonar.db.component.ComponentTreeQuery.Strategy.LEAVES;
import static org.sonar.db.organization.OrganizationTesting.newOrganizationDto;

public class ComponentTreeQueryTest {

  private static final String BASE_UUID = "ABCD";

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Test
  public void create_query() {
    ComponentTreeQuery query = ComponentTreeQuery.builder()
      .setBaseUuid(BASE_UUID)
      .setStrategy(CHILDREN)
      .setQualifiers(asList("FIL", "DIR"))
      .setNameOrKeyQuery("teSt")
      .build();

    assertThat(query.getBaseUuid()).isEqualTo(BASE_UUID);
    assertThat(query.getStrategy()).isEqualTo(CHILDREN);
    assertThat(query.getQualifiers()).containsOnly("FIL", "DIR");
    assertThat(query.getNameOrKeyQuery()).isEqualTo("teSt");
    assertThat(query.getNameOrKeyUpperLikeQuery()).isEqualTo("%TEST%");
  }

  @Test
  public void create_minimal_query() {
    ComponentTreeQuery query = ComponentTreeQuery.builder()
      .setBaseUuid(BASE_UUID)
      .setStrategy(CHILDREN)
      .build();

    assertThat(query.getBaseUuid()).isEqualTo(BASE_UUID);
    assertThat(query.getStrategy()).isEqualTo(CHILDREN);
    assertThat(query.getQualifiers()).isNull();
    assertThat(query.getNameOrKeyQuery()).isNull();
    assertThat(query.getNameOrKeyUpperLikeQuery()).isNull();
  }

  @Test
  public void test_getUuidPath() {
    assertThat(ComponentTreeQuery.builder().setBaseUuid(BASE_UUID).setStrategy(CHILDREN)
      .build().getUuidPath(newPrivateProjectDto(newOrganizationDto(), "PROJECT_UUID"))).isEqualTo(".PROJECT_UUID.");

    assertThat(ComponentTreeQuery.builder().setBaseUuid(BASE_UUID).setStrategy(LEAVES)
      .build().getUuidPath(newPrivateProjectDto(newOrganizationDto(), "PROJECT_UUID"))).isEqualTo(".PROJECT/_UUID.%");
  }

  @Test
  public void fail_when_no_base_uuid() {
    expectedException.expect(NullPointerException.class);
    ComponentTreeQuery.builder()
      .setStrategy(CHILDREN)
      .build();
  }

  @Test
  public void fail_when_no_strategy() {
    expectedException.expect(NullPointerException.class);
    ComponentTreeQuery.builder()
      .setBaseUuid(BASE_UUID)
      .build();
  }
}
