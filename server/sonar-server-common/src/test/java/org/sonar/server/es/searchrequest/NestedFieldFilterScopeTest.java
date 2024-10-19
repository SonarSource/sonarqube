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

import static org.apache.commons.lang3.RandomStringUtils.randomAlphabetic;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class NestedFieldFilterScopeTest {
  @Test
  public void constructor_fails_with_NPE_if_fieldName_is_null() {
    String nestedFieldName = randomAlphabetic(11);
    String value = randomAlphabetic(12);

    assertThatThrownBy(() -> new NestedFieldFilterScope<>(null, nestedFieldName, value))
      .isInstanceOf(NullPointerException.class)
      .hasMessage("fieldName can't be null");
  }

  @Test
  public void constructor_fails_with_NPE_if_nestedFieldName_is_null() {
    String fieldName = randomAlphabetic(10);
    String value = randomAlphabetic(12);

    assertThatThrownBy(() -> new NestedFieldFilterScope<>(fieldName, null, value))
      .isInstanceOf(NullPointerException.class)
      .hasMessage("nestedFieldName can't be null");
  }

  @Test
  public void constructor_fails_with_NPE_if_value_is_null() {
    String fieldName = randomAlphabetic(10);
    String nestedFieldName = randomAlphabetic(11);

    assertThatThrownBy(() -> new NestedFieldFilterScope<>(fieldName, nestedFieldName, null))
      .isInstanceOf(NullPointerException.class)
      .hasMessage("value can't be null");
  }

  @Test
  public void verify_getters() {
    String fieldName = randomAlphabetic(10);
    String nestedFieldName = randomAlphabetic(11);
    Object value = new Object();

    NestedFieldFilterScope<Object> underTest = new NestedFieldFilterScope<>(fieldName, nestedFieldName, value);

    assertThat(underTest.getFieldName()).isEqualTo(fieldName);
    assertThat(underTest.getNestedFieldName()).isEqualTo(nestedFieldName);
    assertThat(underTest.getNestedFieldValue()).isSameAs(value);
  }

  @Test
  public void verify_equals() {
    String fieldName = randomAlphabetic(10);
    String nestedFieldName = randomAlphabetic(11);
    Object value = new Object();
    String fieldName2 = randomAlphabetic(12);
    String nestedFieldName2 = randomAlphabetic(13);
    Object value2 = new Object();
    NestedFieldFilterScope<Object> underTest = new NestedFieldFilterScope<>(fieldName, nestedFieldName, value);

    assertThat(underTest)
      .isEqualTo(underTest)
      .isEqualTo(new NestedFieldFilterScope<>(fieldName, nestedFieldName, value))
      .isNotNull()
      .isNotEqualTo(new Object())
      .isNotEqualTo(new NestedFieldFilterScope<>(fieldName2, nestedFieldName, value))
      .isNotEqualTo(new NestedFieldFilterScope<>(fieldName, nestedFieldName2, value))
      .isNotEqualTo(new NestedFieldFilterScope<>(fieldName, nestedFieldName, value2))
      .isNotEqualTo(new NestedFieldFilterScope<>(fieldName2, nestedFieldName2, value))
      .isNotEqualTo(new NestedFieldFilterScope<>(fieldName, nestedFieldName2, value2))
      .isNotEqualTo(new NestedFieldFilterScope<>(fieldName2, nestedFieldName, value2))
      .isNotEqualTo(new NestedFieldFilterScope<>(fieldName2, nestedFieldName2, value2))
      .isNotEqualTo(new SimpleFieldFilterScope(fieldName))
      .isNotEqualTo(new SimpleFieldFilterScope(fieldName2));
  }

  @Test
  public void verify_hashcode() {
    String fieldName = randomAlphabetic(10);
    String nestedFieldName = randomAlphabetic(11);
    Object value = new Object();
    String fieldName2 = randomAlphabetic(12);
    String nestedFieldName2 = randomAlphabetic(13);
    Object value2 = new Object();
    NestedFieldFilterScope<Object> underTest = new NestedFieldFilterScope<>(fieldName, nestedFieldName, value);

    assertThat(underTest.hashCode())
      .isEqualTo(underTest.hashCode())
      .isEqualTo(new NestedFieldFilterScope<>(fieldName, nestedFieldName, value).hashCode());
    assertThat(underTest.hashCode())
      .isNotEqualTo(new Object().hashCode())
      .isNotEqualTo(new NestedFieldFilterScope<>(fieldName2, nestedFieldName, value).hashCode())
      .isNotEqualTo(new NestedFieldFilterScope<>(fieldName, nestedFieldName2, value).hashCode())
      .isNotEqualTo(new NestedFieldFilterScope<>(fieldName, nestedFieldName, value2).hashCode())
      .isNotEqualTo(new NestedFieldFilterScope<>(fieldName2, nestedFieldName2, value).hashCode())
      .isNotEqualTo(new NestedFieldFilterScope<>(fieldName, nestedFieldName2, value2).hashCode())
      .isNotEqualTo(new NestedFieldFilterScope<>(fieldName2, nestedFieldName, value2).hashCode())
      .isNotEqualTo(new NestedFieldFilterScope<>(fieldName2, nestedFieldName2, value2).hashCode())
      .isNotEqualTo(new SimpleFieldFilterScope(fieldName).hashCode())
      .isNotEqualTo(new SimpleFieldFilterScope(fieldName2)).hashCode();
  }

  @Test
  public void verify_intersect() {
    String fieldName = randomAlphabetic(10);
    String nestedFieldName = randomAlphabetic(11);
    Object value = new Object();
    String fieldName2 = randomAlphabetic(12);
    String nestedFieldName2 = randomAlphabetic(13);
    Object value2 = new Object();
    NestedFieldFilterScope<Object> underTest = new NestedFieldFilterScope<>(fieldName, nestedFieldName, value);

    assertThat(underTest.intersect(underTest)).isTrue();
    assertThat(underTest.intersect(new NestedFieldFilterScope<>(fieldName, nestedFieldName, value))).isTrue();
    assertThat(underTest.intersect(new NestedFieldFilterScope<>(fieldName2, nestedFieldName, value))).isFalse();
    assertThat(underTest.intersect(new NestedFieldFilterScope<>(fieldName, nestedFieldName2, value))).isFalse();
    assertThat(underTest.intersect(new NestedFieldFilterScope<>(fieldName, nestedFieldName, value2))).isFalse();
    assertThat(underTest.intersect(new NestedFieldFilterScope<>(fieldName2, nestedFieldName2, value))).isFalse();
    assertThat(underTest.intersect(new NestedFieldFilterScope<>(fieldName, nestedFieldName2, value2))).isFalse();
    assertThat(underTest.intersect(new NestedFieldFilterScope<>(fieldName2, nestedFieldName, value2))).isFalse();
    assertThat(underTest.intersect(new NestedFieldFilterScope<>(fieldName2, nestedFieldName2, value2))).isFalse();
    assertThat(underTest.intersect(new SimpleFieldFilterScope(fieldName))).isFalse();
    assertThat(underTest.intersect(new SimpleFieldFilterScope(fieldName2))).isFalse();
  }
}
