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

import java.util.Collections;
import org.junit.Test;
import org.sonar.db.component.ComponentDto;

import static com.google.common.collect.Sets.newHashSet;
import static java.util.Collections.singletonList;

public class ComponentIndexFilterByComponentUuidsTest extends ComponentIndexTest {

  private ComponentIndexQuery query = new ComponentIndexQuery().setQualifiers(singletonList("TRK"));

  @Test
  public void filter_by_component_uuids() throws Exception {
    ComponentDto project1 = indexProject("quality", "Quality Product");
    ComponentDto project2 = indexProject("sonarqube", "SonarQube");
    ComponentDto project3 = indexProject("apache", "Apache");

    assertSearch(query.setComponentUuids(newHashSet(project1.uuid()))).containsOnly(project1.uuid());
    assertSearch(query.setComponentUuids(newHashSet(project1.uuid(), project2.uuid()))).containsOnly(project1.uuid(), project2.uuid());
  }

  @Test
  public void return_all_components_when_component_uuids_is_null() throws Exception {
    ComponentDto project1 = indexProject("quality", "Quality Product");
    ComponentDto project2 = indexProject("sonarqube", "SonarQube");
    ComponentDto project3 = indexProject("apache", "Apache");

    assertSearch(query.setComponentUuids(null)).containsOnly(project1.uuid(), project2.uuid(), project3.uuid());
  }

  @Test
  public void return_nothing_when_component_uuids_is_empty() throws Exception {
    indexProject("quality", "Quality Product");
    indexProject("sonarqube", "SonarQube");
    indexProject("apache", "Apache");

    assertSearch(query.setComponentUuids(Collections.emptySet())).isEmpty();
  }

}
