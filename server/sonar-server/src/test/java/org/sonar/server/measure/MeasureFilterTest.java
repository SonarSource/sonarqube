/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.server.measure;

import com.google.common.collect.Lists;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

public class MeasureFilterTest {
  @Test
  public void should_sanitize_list() {
    assertThat(MeasureFilter.sanitize(null)).isEmpty();
    assertThat(MeasureFilter.sanitize(Lists.<String>newArrayList())).isEmpty();
    assertThat(MeasureFilter.sanitize(Arrays.asList(""))).isEmpty();
    assertThat(MeasureFilter.sanitize(Lists.newArrayList("TRK"))).containsExactly("TRK");
    assertThat(MeasureFilter.sanitize(Lists.newArrayList("TRK", "BRC"))).containsExactly("TRK", "BRC");
  }

  @Test
  public void filter_is_not_empty_if_at_least_condition_on_favourites() {
    assertThat(new MeasureFilter().isEmpty()).isTrue();
    assertThat(new MeasureFilter().setUserFavourites(true).isEmpty()).isFalse();
  }

  @Test
  public void filter_is_not_empty_if_at_least_condition_on_qualifiers() {
    assertThat(new MeasureFilter().isEmpty()).isTrue();
    assertThat(new MeasureFilter().setResourceQualifiers(Collections.<String>emptyList()).isEmpty()).isTrue();
    assertThat(new MeasureFilter().setResourceQualifiers(Arrays.asList("TRK")).isEmpty()).isFalse();
  }

  @Test
  public void filter_is_not_empty_if_at_least_condition_on_scopes() {
    assertThat(new MeasureFilter().isEmpty()).isTrue();
    assertThat(new MeasureFilter().setResourceScopes(Collections.<String>emptyList()).isEmpty()).isTrue();
    assertThat(new MeasureFilter().setResourceScopes(Arrays.asList("PRJ")).isEmpty()).isFalse();
  }

  @Test
  public void filter_is_not_empty_if_at_least_condition_on_root_resource() {
    assertThat(new MeasureFilter().isEmpty()).isTrue();
    assertThat(new MeasureFilter().setBaseResourceKey("foo").isEmpty()).isFalse();
  }
}
