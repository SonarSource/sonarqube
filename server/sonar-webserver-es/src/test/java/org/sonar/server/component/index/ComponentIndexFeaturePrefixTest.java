/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sonar.server.es.textsearch.ComponentTextSearchFeatureRepertoire;

class ComponentIndexFeaturePrefixTest extends ComponentIndexTest {

  @BeforeEach
  void before() {
    features.set(ComponentTextSearchFeatureRepertoire.PREFIX, ComponentTextSearchFeatureRepertoire.PREFIX_IGNORE_CASE);
  }

  @Test
  void should_find_prefix() {
    assertResultOrder("comp", "component");
  }

  @Test
  void should_find_exact_match() {
    assertResultOrder("component.js", "component.js");
  }

  @Test
  void should_not_find_partially() {
    indexProject("my_component");

    assertNoSearchResults("component.js");
  }

  @Test
  void should_be_able_to_ignore_case() {
    features.set(ComponentTextSearchFeatureRepertoire.PREFIX_IGNORE_CASE);
    assertResultOrder("cOmPoNeNt.Js", "CoMpOnEnT.jS");
  }

  @Test
  void should_prefer_matching_case() {
    assertResultOrder("cOmPoNeNt.Js", "cOmPoNeNt.Js", "CoMpOnEnT.jS");
  }
}
