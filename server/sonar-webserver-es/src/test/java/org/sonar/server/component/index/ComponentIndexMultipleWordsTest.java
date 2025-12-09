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

import org.junit.jupiter.api.Test;
import org.sonar.server.es.textsearch.ComponentTextSearchFeatureRepertoire;

class ComponentIndexMultipleWordsTest extends ComponentIndexTest {

  @Test
  void should_find_perfect_match() {
    assertResultOrder("struts java",
      "Struts.java");
  }

  @Test
  void should_find_partial_match() {
    features.set(ComponentTextSearchFeatureRepertoire.PARTIAL);
    assertResultOrder("struts java",
      "Xstrutsx.Xjavax");
  }

  @Test
  void should_find_partial_match_prefix_word1() {
    assertResultOrder("struts java",
      "MyStruts.java");
  }

  @Test
  void should_find_partial_match_suffix_word1() {
    assertResultOrder("struts java",
      "StrutsObject.java");
  }

  @Test
  void should_find_partial_match_prefix_word2() {
    assertResultOrder("struts java",
      "MyStruts.xjava");
  }

  @Test
  void should_find_partial_match_suffix_word2() {
    assertResultOrder("struts java",
      "MyStruts.javax");
  }

  @Test
  void should_find_partial_match_prefix_and_suffix_everywhere() {
    assertResultOrder("struts java",
      "MyStrutsObject.xjavax");
  }

  @Test
  void should_find_subset_of_document_terms() {
    assertResultOrder("struts java",
      "Some.Struts.Class.java.old");
  }


}
