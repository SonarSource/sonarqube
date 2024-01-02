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
package org.sonar.server.es.searchrequest;

import org.junit.Test;
import org.sonar.server.es.searchrequest.TopAggregationDefinition.NestedFieldFilterScope;
import org.sonar.server.es.searchrequest.TopAggregationDefinition.SimpleFieldFilterScope;

import static org.apache.commons.lang.RandomStringUtils.randomAlphabetic;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class SimpleFieldFilterScopeTest {
  @Test
  public void constructor_fails_with_NPE_if_fieldName_is_null() {
    assertThatThrownBy(() -> new SimpleFieldFilterScope(null))
      .isInstanceOf(NullPointerException.class)
      .hasMessage("fieldName can't be null");
  }

  @Test
  public void getFieldName() {
    String fieldName = randomAlphabetic(12);
    SimpleFieldFilterScope underTest = new SimpleFieldFilterScope(fieldName);

    assertThat(underTest.getFieldName()).isEqualTo(fieldName);
  }

  @Test
  public void verify_equals() {
    String fieldName1 = randomAlphabetic(11);
    String fieldName2 = randomAlphabetic(12);
    SimpleFieldFilterScope underTest = new SimpleFieldFilterScope(fieldName1);

    assertThat(underTest)
      .isEqualTo(underTest)
      .isEqualTo(new SimpleFieldFilterScope(fieldName1))
      .isNotNull()
      .isNotEqualTo(new Object())
      .isNotEqualTo(new SimpleFieldFilterScope(fieldName2))
      .isNotEqualTo(new NestedFieldFilterScope<>(fieldName1, "foo", "bar"))
      .isNotEqualTo(new NestedFieldFilterScope<>(fieldName2, "foo", "bar"));
  }

  @Test
  public void verify_hashcode() {
    String fieldName1 = randomAlphabetic(11);
    String fieldName2 = randomAlphabetic(12);
    SimpleFieldFilterScope underTest = new SimpleFieldFilterScope(fieldName1);

    assertThat(underTest.hashCode())
      .isEqualTo(underTest.hashCode())
      .isEqualTo(new SimpleFieldFilterScope(fieldName1).hashCode());
    assertThat(underTest.hashCode())
      .isNotEqualTo(new Object().hashCode())
      .isNotEqualTo(new SimpleFieldFilterScope(fieldName2).hashCode())
      .isNotEqualTo(new NestedFieldFilterScope<>(fieldName1, "foo", "bar").hashCode())
      .isNotEqualTo(new NestedFieldFilterScope<>(fieldName1, "foo", "bar").hashCode());
  }

  @Test
  public void verify_intersect() {
    String fieldName1 = randomAlphabetic(11);
    String fieldName2 = randomAlphabetic(12);
    SimpleFieldFilterScope underTest = new SimpleFieldFilterScope(fieldName1);

    assertThat(underTest.intersect(underTest)).isTrue();
    assertThat(underTest.intersect(new SimpleFieldFilterScope(fieldName1))).isTrue();
    assertThat(underTest.intersect(new SimpleFieldFilterScope(fieldName2))).isFalse();
    assertThat(underTest.intersect(new NestedFieldFilterScope<>(fieldName1, "foo", "bar"))).isTrue();
    assertThat(underTest.intersect(new NestedFieldFilterScope<>(fieldName2, "foo", "bar"))).isFalse();
  }
}
