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
package org.sonar.server.component.index;

import org.junit.Before;
import org.junit.Test;
import org.sonar.db.component.ComponentDto;

public class ComponentIndexFeatureFuzzyTest extends ComponentIndexTest {

  @Before
  public void before() {
    features.set(ComponentIndexSearchFeature.FUZZY, ComponentIndexSearchFeature.FUZZY_PREFIX);
  }

  @Test
  public void should_find_item_despite_missing_character() {
    ComponentDto project = indexProject("key-1", "SonarQube");

    assertSearchResults("SonrQube", project);
  }

  @Test
  public void should_find_item_despite_missing_character_and_lowercase() {
    ComponentDto project = indexProject("key-1", "SonarQube");

    assertSearchResults("sonrqube", project);
  }

  @Test
  public void should_find_item_despite_two_missing_characters_and_lowercase() {
    ComponentDto project = indexProject("key-1", "SonarQube");

    assertSearchResults("sonqube", project);
  }

  @Test
  public void missing_characters_should_reduce_score() {
    assertResultOrder("SonarQube.java",
      "sonarqube.java",
      "sonaqube.java",
      "sonqube.java");
  }
}
