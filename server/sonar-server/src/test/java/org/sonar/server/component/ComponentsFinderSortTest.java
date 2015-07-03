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

package org.sonar.server.component;

import org.junit.Test;
import org.sonar.api.component.Component;
import org.sonar.db.component.ComponentDto;

import java.util.List;

import static com.google.common.collect.Lists.newArrayList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ComponentsFinderSortTest {

  @Test
  public void should_sort_by_asc_name() {
    List<? extends Component> dtoList = newArrayList(
      new ComponentDto().setKey("org.codehaus.sonar").setName("Sonar"),
      new ComponentDto().setKey("org.apache.tika:tika").setName("Apache Tika"),
      new ComponentDto().setKey("org.picocontainer:picocontainer-parent").setName("PicoContainer Parent"),
      new ComponentDto().setKey("org.codehaus.sample")
    );

    ComponentQuery query = ComponentQuery.builder().sort(ComponentQuery.SORT_BY_NAME).asc(true).build();
    ComponentsFinderSort finderSort = new ComponentsFinderSort(dtoList, query);

    List<Component> result = newArrayList(finderSort.sort());

    assertThat(result).hasSize(4);
    assertThat(result.get(0).name()).isEqualTo("Apache Tika");
    assertThat(result.get(1).name()).isEqualTo("PicoContainer Parent");
    assertThat(result.get(2).name()).isEqualTo("Sonar");
    assertThat(result.get(3).name()).isNull();
  }

  @Test
  public void should_sort_by_desc_name() {
    List<? extends Component> dtoList = newArrayList(
      new ComponentDto().setKey("org.codehaus.sonar").setName("Sonar"),
      new ComponentDto().setKey("org.apache.tika:tika").setName("Apache Tika"),
      new ComponentDto().setKey("org.picocontainer:picocontainer-parent").setName("PicoContainer Parent"),
      new ComponentDto().setKey("org.codehaus.sample")
    );

    ComponentQuery query = ComponentQuery.builder().sort(ComponentQuery.SORT_BY_NAME).asc(false).build();
    ComponentsFinderSort finderSort = new ComponentsFinderSort(dtoList, query);

    List<Component> result = newArrayList(finderSort.sort());

    assertThat(result).hasSize(4);
    assertThat(result.get(0).name()).isNull();
    assertThat(result.get(1).name()).isEqualTo("Sonar");
    assertThat(result.get(2).name()).isEqualTo("PicoContainer Parent");
    assertThat(result.get(3).name()).isEqualTo("Apache Tika");
  }

  @Test
  public void should_not_sort_with_null_sort() {
    List<? extends Component> dtoList = newArrayList(
      new ComponentDto().setKey("org.codehaus.sonar").setName("Sonar"),
      new ComponentDto().setKey("org.apache.tika:tika").setName("Apache Tika"),
      new ComponentDto().setKey("org.picocontainer:picocontainer-parent").setName("PicoContainer Parent"),
      new ComponentDto().setKey("org.codehaus.sample")
    );

    ComponentQuery query = ComponentQuery.builder().sort(null).build();
    ComponentsFinderSort finderSort = new ComponentsFinderSort(dtoList, query);

    List<Component> result = newArrayList(finderSort.sort());

    assertThat(result).hasSize(4);
    assertThat(result.get(0).name()).isEqualTo("Sonar");
    assertThat(result.get(1).name()).isEqualTo("Apache Tika");
    assertThat(result.get(2).name()).isEqualTo("PicoContainer Parent");
    assertThat(result.get(3).name()).isNull();
  }

  @Test
  public void should_fail_to_sort_with_unknown_sort() {
    ComponentQuery query = mock(ComponentQuery.class);
    when(query.sort()).thenReturn("unknown");
    ComponentsFinderSort finderSort = new ComponentsFinderSort(null, query);
    try {
      finderSort.sort();
    } catch (Exception e) {
       assertThat(e).isInstanceOf(IllegalArgumentException.class).hasMessage("Cannot sort on field : unknown");
    }
  }

}
