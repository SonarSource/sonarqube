/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.Test;
import org.sonar.api.resources.Qualifiers;

import static org.assertj.core.api.Assertions.assertThat;

public class ComponentIndexHighlightTest extends ComponentIndexTest {

  @Test
  public void should_escape_html() {
    assertHighlighting("quick< brown fox", "brown", "quick&lt; <mark>brown</mark> fox");
  }

  @Test
  public void should_highlight_partial_name() {
    assertHighlighting("quickbrownfox", "brown", "quick<mark>brown</mark>fox");
  }

  @Test
  public void should_highlight_prefix() {
    assertHighlighting("quickbrownfox", "quick", "<mark>quick</mark>brownfox");
  }

  @Test
  public void should_highlight_suffix() {
    assertHighlighting("quickbrownfox", "fox", "quickbrown<mark>fox</mark>");
  }

  @Test
  public void should_highlight_multiple_words() {
    assertHighlighting("quickbrownfox", "fox bro", "quick<mark>bro</mark>wn<mark>fox</mark>");
  }

  @Test
  public void should_highlight_multiple_connected_words() {
    assertHighlighting("quickbrownfox", "fox brown", "quick<mark>brownfox</mark>");
  }

  private void assertHighlighting(String fileName, String search, String expectedHighlighting) {
    indexFile(fileName);

    SuggestionQuery query = SuggestionQuery.builder()
      .setQuery(search)
      .setQualifiers(Collections.singletonList(Qualifiers.FILE))
      .build();
    Stream<ComponentHitsPerQualifier> results = index.searchSuggestions(query, features.get()).getQualifiers();

    assertThat(results).flatExtracting(ComponentHitsPerQualifier::getHits)
      .extracting(ComponentHit::getHighlightedText)
      .extracting(Optional::get)
      .containsExactly(expectedHighlighting);
  }
}
