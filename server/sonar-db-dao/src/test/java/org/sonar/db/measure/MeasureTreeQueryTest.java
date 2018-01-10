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
package org.sonar.db.measure;

import java.util.Collections;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.db.component.ComponentTesting;
import org.sonar.db.organization.OrganizationTesting;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.db.measure.MeasureTreeQuery.Strategy.CHILDREN;
import static org.sonar.db.measure.MeasureTreeQuery.Strategy.LEAVES;

public class MeasureTreeQueryTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Test
  public void create_query() {
    MeasureTreeQuery query = MeasureTreeQuery.builder()
      .setStrategy(CHILDREN)
      .setQualifiers(asList("FIL", "DIR"))
      .setNameOrKeyQuery("teSt")
      .setMetricIds(asList(10, 11))
      .build();

    assertThat(query.getStrategy()).isEqualTo(CHILDREN);
    assertThat(query.getQualifiers()).containsOnly("FIL", "DIR");
    assertThat(query.getNameOrKeyQuery()).isEqualTo("teSt");
    assertThat(query.getMetricIds()).containsOnly(10, 11);
  }

  @Test
  public void create_minimal_query() {
    MeasureTreeQuery query = MeasureTreeQuery.builder()
      .setStrategy(CHILDREN)
      .build();

    assertThat(query.getStrategy()).isEqualTo(CHILDREN);
    assertThat(query.getQualifiers()).isNull();
    assertThat(query.getNameOrKeyQuery()).isNull();
    assertThat(query.getMetricIds()).isNull();
  }

  @Test
  public void test_getNameOrKeyUpperLikeQuery() {
    assertThat(MeasureTreeQuery.builder()
      .setNameOrKeyQuery("like-\\_%/-value")
      .setStrategy(CHILDREN)
      .build().getNameOrKeyUpperLikeQuery()).isEqualTo("%LIKE-\\/_/%//-VALUE%");

    assertThat(MeasureTreeQuery.builder()
      .setStrategy(CHILDREN)
      .build().getNameOrKeyUpperLikeQuery()).isNull();
  }

  @Test
  public void test_getUuidPath() {
    assertThat(MeasureTreeQuery.builder().setStrategy(CHILDREN)
      .build().getUuidPath(ComponentTesting.newPrivateProjectDto(OrganizationTesting.newOrganizationDto(), "PROJECT_UUID"))).isEqualTo(".PROJECT_UUID.");

    assertThat(MeasureTreeQuery.builder().setStrategy(LEAVES)
      .build().getUuidPath(ComponentTesting.newPrivateProjectDto(OrganizationTesting.newOrganizationDto(), "PROJECT_UUID"))).isEqualTo(".PROJECT/_UUID.%");
  }

  @Test
  public void return_empty_when_metrics_is_empty() {
    assertThat(MeasureTreeQuery.builder()
      .setStrategy(CHILDREN)
      .setMetricIds(Collections.emptyList())
      .build().returnsEmpty()).isTrue();

    assertThat(MeasureTreeQuery.builder()
      .setStrategy(CHILDREN)
      .setMetricIds(null)
      .build().returnsEmpty()).isFalse();
  }

  @Test
  public void return_empty_when_qualifiers_is_empty() {
    assertThat(MeasureTreeQuery.builder()
      .setStrategy(CHILDREN)
      .setQualifiers(Collections.emptyList())
      .build().returnsEmpty()).isTrue();

    assertThat(MeasureTreeQuery.builder()
      .setStrategy(CHILDREN)
      .setQualifiers(asList("FIL", "DIR"))
      .build().returnsEmpty()).isFalse();
  }

  @Test
  public void fail_when_no_strategy() {
    expectedException.expect(NullPointerException.class);
    MeasureTreeQuery.builder()
      .build();
  }

}
