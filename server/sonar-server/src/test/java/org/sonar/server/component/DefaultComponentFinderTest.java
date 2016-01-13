/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package org.sonar.server.component;

import org.junit.Test;
import org.sonar.api.component.Component;
import org.sonar.api.resources.Project;

import java.util.Iterator;
import java.util.List;

import static com.google.common.collect.Lists.newArrayList;
import static org.assertj.core.api.Assertions.assertThat;

public class DefaultComponentFinderTest {

  DefaultComponentFinder finder = new DefaultComponentFinder();

  @Test
  public void should_return_all_components_when_no_parameter() {
    List<Component> components = newArrayList(
      createProject("org.codehaus.sonar", "Sonar"),
      createProject("org.apache.tika:tika", "Apache Tika"),
      createProject("org.picocontainer:picocontainer-parent", "PicoContainer Parent")
    );

    ComponentQuery query = ComponentQuery.builder().build();
    DefaultComponentQueryResult results = finder.find(query, components);

    assertThat(results.components()).hasSize(3);
    Component component = results.components().iterator().next();
    assertThat(component.name()).isEqualTo("Apache Tika");
    assertThat(component.key()).isEqualTo("org.apache.tika:tika");
    assertThat(component.qualifier()).isEqualTo("TRK");
  }

  @Test
  public void should_find_components_by_key_pattern() {
    List<Component> components = newArrayList(
      createProject("org.codehaus.sonar", "Sonar"),
      createProject("org.apache.tika:tika", "Apache Tika"),
      createProject("org.apache.jackrabbit:jackrabbit", "Apache Jackrabbit")
    );

    ComponentQuery query = ComponentQuery.builder().keys(newArrayList("org.apache")).build();
    assertThat(finder.find(query, components).components()).hasSize(2);
  }

  @Test
  public void should_find_components_by_name_pattern() {
    List<Component> components = newArrayList(
      createProject("org.codehaus.sonar", "Sonar"),
      createProject("org.apache.tika:tika", "Apache Tika"),
      createProject("org.apache.jackrabbit:jackrabbit", "Apache Jackrabbit")
    );

    ComponentQuery query = ComponentQuery.builder().names(newArrayList("Apache")).build();
    assertThat(finder.find(query, components).components()).hasSize(2);
  }

  @Test
  public void should_sort_result_by_name() {
    List<Component> components = newArrayList(
      createProject("org.codehaus.sonar", "Sonar"),
      createProject("org.apache.tika:tika", "Apache Tika"),
      createProject("org.picocontainer:picocontainer-parent", "PicoContainer Parent")
    );

    ComponentQuery query = ComponentQuery.builder().build();
    DefaultComponentQueryResult results = finder.find(query, components);

    assertThat(results.components()).hasSize(3);
    Iterator<? extends Component> iterator = results.components().iterator();
    assertThat(iterator.next().name()).isEqualTo("Apache Tika");
    assertThat(iterator.next().name()).isEqualTo("PicoContainer Parent");
    assertThat(iterator.next().name()).isEqualTo("Sonar");
  }

  @Test
  public void should_find_paginate_result() {
    ComponentQuery query = ComponentQuery.builder().pageSize(1).pageIndex(1).build();

    List<Component> components = newArrayList(
      createProject("org.codehaus.sonar", "Sonar"),
      createProject("org.apache.tika:tika", "Apache Tika"),
      createProject("org.picocontainer:picocontainer-parent", "PicoContainer Parent")
    );

    DefaultComponentQueryResult results = finder.find(query, components);
    assertThat(results.paging().offset()).isEqualTo(0);
    assertThat(results.paging().pages()).isEqualTo(3);
    assertThat(results.paging().total()).isEqualTo(3);
  }

  @Test
  public void should_skip_pagination() {
    ComponentQuery query = ComponentQuery.builder().pageSize(ComponentQuery.NO_PAGINATION)
      .pageIndex(ComponentQuery.DEFAULT_PAGE_INDEX).build();

    List<Component> components = newArrayList(
      createProject("org.codehaus.sonar", "Sonar"),
      createProject("org.apache.tika:tika", "Apache Tika"),
      createProject("org.picocontainer:picocontainer-parent", "PicoContainer Parent")
    );

    DefaultComponentQueryResult results = finder.find(query, components);
    assertThat(results.paging()).isNull();
    assertThat(results.components().size()).isEqualTo(3);
  }

  private Component createProject(String key, String name) {
    return new Project(key, null, name);
  }

}
