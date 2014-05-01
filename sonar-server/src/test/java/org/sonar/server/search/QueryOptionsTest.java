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

package org.sonar.server.search;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import org.junit.Test;

import java.util.Arrays;

import static org.fest.assertions.Assertions.assertThat;

public class QueryOptionsTest {

  @Test
  public void fields_to_return() throws Exception {
    QueryOptions options = new QueryOptions();
    assertThat(options.getFieldsToReturn()).isEmpty();

    options.setFieldsToReturn(Arrays.asList("one", "two"));
    assertThat(options.getFieldsToReturn()).containsOnly("one", "two");

    options.addFieldsToReturn(Arrays.asList("three"));
    assertThat(options.getFieldsToReturn()).containsOnly("one", "two", "three");

    options.addFieldsToReturn("four");
    assertThat(options.getFieldsToReturn()).containsOnly("one", "two", "three", "four");

    options.filterFieldsToReturn(Sets.newHashSet("one", "four", "five"));
    assertThat(options.getFieldsToReturn()).containsOnly("one", "four");
  }

  @Test
  public void support_immutable_fields() throws Exception {
    QueryOptions options = new QueryOptions();

    options.setFieldsToReturn(ImmutableList.of("one", "two"));
    assertThat(options.getFieldsToReturn()).containsOnly("one", "two");

    options.addFieldsToReturn(ImmutableList.of("three"));
    assertThat(options.getFieldsToReturn()).containsOnly("one", "two", "three");

    options.addFieldsToReturn("four");
    assertThat(options.getFieldsToReturn()).containsOnly("one", "two", "three", "four");

    options.filterFieldsToReturn(ImmutableSet.of("one", "four", "five"));
    assertThat(options.getFieldsToReturn()).containsOnly("one", "four");
  }
}
