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
package org.sonar.server.search;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class FacetValueTest {

  @Test
  public void should_check_for_equality_on_key() {
    FacetValue facetValue = new FacetValue("polop", 42);
    assertThat(facetValue.equals(facetValue)).isTrue();
    assertThat(facetValue.equals(null)).isFalse();
    assertThat(facetValue.equals(new Object())).isFalse();

    assertThat(facetValue.equals(new FacetValue("polop", 666))).isTrue();
    assertThat(facetValue.equals(new FacetValue("palap", 42))).isFalse();

    FacetValue withNullKey = new FacetValue(null, 42);
    assertThat(facetValue.equals(withNullKey)).isFalse();
    assertThat(withNullKey.equals(withNullKey)).isTrue();
    assertThat(withNullKey.equals(facetValue)).isFalse();
    assertThat(withNullKey.equals(new FacetValue(null, 666))).isTrue();
  }

  @Test
  public void should_use_key_hashcode() {
    assertThat(new FacetValue(null, 42).hashCode()).isZero();
    String key = "polop";
    assertThat(new FacetValue(key, 666).hashCode()).isEqualTo(key.hashCode());
  }

  @Test
  public void test_toString() {
    assertThat(new FacetValue("polop", 42).toString()).isEqualTo("{polop=42}");
  }
}
