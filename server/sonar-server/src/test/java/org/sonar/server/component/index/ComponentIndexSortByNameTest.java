/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
package org.sonar.server.component.index;

import org.junit.Test;
import org.sonar.db.component.ComponentDto;

import static java.util.Collections.singletonList;
import static org.sonar.server.component.index.ComponentIndexQuery.Sort.BY_ASCENDING_NAME;

public class ComponentIndexSortByNameTest extends ComponentIndexTest {

  private ComponentIndexQuery query = new ComponentIndexQuery().setQualifiers(singletonList("TRK"));

  @Test
  public void sort_by_name() throws Exception {
    ComponentDto project1 = indexProject("quality", "Quality Product");
    ComponentDto project2 = indexProject("sonarqube", "SonarQube");
    ComponentDto project3 = indexProject("apache", "Apache");

    assertSearch(query.setSort(BY_ASCENDING_NAME)).containsExactly(uuids(project3, project1, project2));
  }

  @Test
  public void sort_by_name_then_by_key() throws Exception {
    ComponentDto project1 = indexProject("project1", "Quality Product");
    ComponentDto project2 = indexProject("project2", "Apache");
    ComponentDto project3 = indexProject("project3", "Apache");

    assertSearch(query.setSort(BY_ASCENDING_NAME)).containsExactly(uuids(project2, project3, project1));
  }

}
