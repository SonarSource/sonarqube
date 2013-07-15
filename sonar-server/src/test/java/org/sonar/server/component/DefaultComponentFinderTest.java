/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
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

package org.sonar.server.component;

import org.junit.Test;
import org.sonar.api.component.Component;
import org.sonar.core.resource.ResourceDao;
import org.sonar.core.resource.ResourceDto;
import org.sonar.core.resource.ResourceQuery;

import java.util.Iterator;
import java.util.List;

import static com.google.common.collect.Lists.newArrayList;
import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DefaultComponentFinderTest {

  ResourceDao dao = mock(ResourceDao.class);
  DefaultComponentFinder finder = new DefaultComponentFinder(dao);

  @Test
  public void should_return_all_components_when_no_parameter() {
    List<ResourceDto> dtoList = newArrayList(
      new ResourceDto().setKey("org.codehaus.sonar").setName("Sonar").setQualifier("TRK"),
      new ResourceDto().setKey("org.apache.tika:tika").setName("Apache Tika").setQualifier("TRK"),
      new ResourceDto().setKey("org.picocontainer:picocontainer-parent").setName("PicoContainer Parent").setQualifier("TRK")
    );
    when(dao.getResources(any(ResourceQuery.class))).thenReturn(dtoList);

    ComponentQuery query = ComponentQuery.builder().build();
    DefaultComponentQueryResult results = finder.find(query);

    assertThat(results.components()).hasSize(3);
    Component component = results.components().iterator().next();
    assertThat(component.name()).isEqualTo("Apache Tika");
    assertThat(component.key()).isEqualTo("org.apache.tika:tika");
    assertThat(component.qualifier()).isEqualTo("TRK");
  }

  @Test
  public void should_find_components_by_key_pattern() {
    List<ResourceDto> dtoList = newArrayList(
      new ResourceDto().setKey("org.codehaus.sonar").setName("Sonar").setQualifier("TRK"),
      new ResourceDto().setKey("org.apache.tika:tika").setName("Apache Tika").setQualifier("TRK"),
      new ResourceDto().setKey("org.apache.jackrabbit:jackrabbit").setName("Apache Jackrabbit").setQualifier("TRK")
    );
    when(dao.getResources(any(ResourceQuery.class))).thenReturn(dtoList);

    ComponentQuery query = ComponentQuery.builder().keys(newArrayList("org.apache")).build();
    assertThat(finder.find(query).components()).hasSize(2);
  }

  @Test
  public void should_find_components_by_name_pattern() {
    List<ResourceDto> dtoList = newArrayList(
      new ResourceDto().setKey("org.codehaus.sonar").setName("Sonar").setQualifier("TRK"),
      new ResourceDto().setKey("org.apache.tika:tika").setName("Apache Tika").setQualifier("TRK"),
      new ResourceDto().setKey("org.apache.jackrabbit:jackrabbit").setName("Apache Jackrabbit").setQualifier("TRK")
    );
    when(dao.getResources(any(ResourceQuery.class))).thenReturn(dtoList);

    ComponentQuery query = ComponentQuery.builder().names(newArrayList("Apache")).build();
    assertThat(finder.find(query).components()).hasSize(2);
  }

  @Test
  public void should_sort_result_by_name() {
    List<ResourceDto> dtoList = newArrayList(
      new ResourceDto().setKey("org.codehaus.sonar").setName("Sonar").setQualifier("TRK"),
      new ResourceDto().setKey("org.apache.tika:tika").setName("Apache Tika").setQualifier("TRK"),
      new ResourceDto().setKey("org.picocontainer:picocontainer-parent").setName("PicoContainer Parent").setQualifier("TRK")
    );
    when(dao.getResources(any(ResourceQuery.class))).thenReturn(dtoList);

    ComponentQuery query = ComponentQuery.builder().build();
    DefaultComponentQueryResult results = finder.find(query);

    assertThat(results.components()).hasSize(3);
    Iterator<? extends Component> iterator = results.components().iterator();
    assertThat(iterator.next().name()).isEqualTo("Apache Tika");
    assertThat(iterator.next().name()).isEqualTo("PicoContainer Parent");
    assertThat(iterator.next().name()).isEqualTo("Sonar");
  }

  @Test
  public void should_find_paginate_result() {
    ComponentQuery query = ComponentQuery.builder().pageSize(1).pageIndex(1).build();

    List<ResourceDto> dtoList = newArrayList(
      new ResourceDto().setKey("org.codehaus.sonar").setName("Sonar").setQualifier("TRK"),
      new ResourceDto().setKey("org.apache.tika:tika").setName("Apache Tika").setQualifier("TRK"),
      new ResourceDto().setKey("org.picocontainer:picocontainer-parent").setName("PicoContainer Parent").setQualifier("TRK")
    );
    when(dao.getResources(any(ResourceQuery.class))).thenReturn(dtoList);

    DefaultComponentQueryResult results = finder.find(query);
    assertThat(results.paging().offset()).isEqualTo(0);
    assertThat(results.paging().pages()).isEqualTo(3);
    assertThat(results.paging().total()).isEqualTo(3);
  }

}
