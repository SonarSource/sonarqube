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

package org.sonar.server.view.index;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.sonar.api.config.Settings;
import org.sonar.server.es.EsTester;

import java.util.List;

import static com.google.common.collect.Lists.newArrayList;
import static org.assertj.core.api.Assertions.assertThat;

public class ViewIndexTest {

  @ClassRule
  public static EsTester esTester = new EsTester().addDefinitions(new ViewIndexDefinition(new Settings()));

  private ViewIndex index;

  @Before
  public void setUp() {
    esTester.truncateIndices();
    index = new ViewIndex(esTester.client());
  }

  @Test
  public void find_all_view_uuids() throws Exception {
    esTester.putDocuments(ViewIndexDefinition.INDEX, ViewIndexDefinition.TYPE_VIEW, this.getClass(), "view1.json", "view2.json");

    List<String> result = newArrayList(index.findAllViewUuids());

    assertThat(result).containsOnly("fed0a543-9d9c-4af5-a4ec-450a8fe78ce7", "8d0bc2a5-bfba-464b-92de-bb170e9d978e");
  }

  @Test
  public void not_find_all_view_uuids() {
    List<String> result = newArrayList(index.findAllViewUuids());

    assertThat(result).isEmpty();
  }

  @Test
  public void delete_views() throws Exception {
    esTester.putDocuments(ViewIndexDefinition.INDEX, ViewIndexDefinition.TYPE_VIEW, this.getClass(), "view1.json", "view2.json");

    index.delete(newArrayList("fed0a543-9d9c-4af5-a4ec-450a8fe78ce7", "8d0bc2a5-bfba-464b-92de-bb170e9d978e"));

    assertThat(esTester.countDocuments(ViewIndexDefinition.INDEX, ViewIndexDefinition.TYPE_VIEW)).isEqualTo(0L);
  }

}
