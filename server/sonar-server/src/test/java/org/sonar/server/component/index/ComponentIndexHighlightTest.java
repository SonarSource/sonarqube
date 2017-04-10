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

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.Test;
import org.sonar.api.resources.Qualifiers;

import static org.assertj.core.api.Assertions.assertThat;

public class ComponentIndexHighlightTest extends ComponentIndexTest {

  @Test
  public void should_highlight_prefix() {
    assertHighlighting("quick brown fox", "brown", "quick <mark>brown</mark> fox");
  }

  @Test
  public void should_escape_html() {
    assertHighlighting("quick< brown fox", "brown", "quick&lt; <mark>brown</mark> fox");
  }

  private void assertHighlighting(String fileName, String search, String expectedHighlighting) {
    indexFile(fileName);

    ComponentIndexQuery query = ComponentIndexQuery.builder()
      .setQuery(search)
      .setQualifiers(Collections.singletonList(Qualifiers.FILE))
      .build();
    List<ComponentHitsPerQualifier> results = index.search(query, features.get());

    assertThat(results).flatExtracting(ComponentHitsPerQualifier::getHits)
      .extracting(ComponentHit::getHighlightedText)
      .extracting(Optional::get)
      .containsExactly(expectedHighlighting);
  }
}
