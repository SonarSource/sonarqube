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
import org.sonar.core.component.ComponentDto;

import java.util.List;

import static com.google.common.collect.Lists.newArrayList;
import static org.fest.assertions.Assertions.assertThat;

public class ComponentsFinderSortTest {

  @Test
  public void should_sort_by_name() {
    List<? extends Component> dtoList = newArrayList(
      new ComponentDto().setKey("org.codehaus.sonar").setName("Sonar"),
      new ComponentDto().setKey("org.apache.tika:tika").setName("Apache Tika"),
      new ComponentDto().setKey("org.picocontainer:picocontainer-parent").setName("PicoContainer Parent"),
      new ComponentDto().setKey("org.codehaus.sample")
    );

    ComponentQuery query = ComponentQuery.builder().sort(ComponentQuery.SORT_BY_NAME).asc(true).build();
    ComponentsFinderSort issuesFinderSort = new ComponentsFinderSort(dtoList, query);

    List<Component> result = newArrayList(issuesFinderSort.sort());

    assertThat(result).hasSize(4);
    assertThat(result.get(0).name()).isEqualTo("Apache Tika");
    assertThat(result.get(1).name()).isEqualTo("PicoContainer Parent");
    assertThat(result.get(2).name()).isEqualTo("Sonar");
    assertThat(result.get(3).name()).isNull();
  }

}
