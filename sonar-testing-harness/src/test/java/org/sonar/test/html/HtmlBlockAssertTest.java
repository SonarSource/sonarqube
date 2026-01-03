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
package org.sonar.test.html;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.sonar.test.html.HtmlFragmentAssert.assertThat;

public class HtmlBlockAssertTest {

  @Test
  public void withLinkOn_singleLink_passes() {
    assertThat("<p>Click <a href='http://example.com'>here</a> for more</p>")
      .hasParagraph()
      .withLinkOn("here");
  }

  @Test
  public void withLinkOn_multipleOccurrences_passes() {
    assertThat("<p><a href='#'>link</a> and <a href='#'>link</a></p>")
      .hasParagraph()
      .withLinkOn("link", 2);
  }

  @Test
  public void withLinkOn_noLink_throwsAssertionError() {
    assertThatThrownBy(() -> 
      assertThat("<p>No links here</p>")
        .hasParagraph()
        .withLinkOn("link"))
      .isInstanceOf(AssertionError.class)
      .hasMessageContaining("no link in bloc");
  }

  @Test
  public void withLinkOn_wrongText_throwsAssertionError() {
    assertThatThrownBy(() -> 
      assertThat("<p><a href='#'>actual</a></p>")
        .hasParagraph()
        .withLinkOn("expected"))
      .isInstanceOf(AssertionError.class);
  }

  @Test
  public void withLink_matchingTextAndHref_passes() {
    assertThat("<p><a href='http://example.com'>Example</a></p>")
      .hasParagraph()
      .withLink("Example", "http://example.com");
  }

  @Test
  public void withLink_wrongHref_throwsAssertionError() {
    assertThatThrownBy(() -> 
      assertThat("<p><a href='http://wrong.com'>Example</a></p>")
        .hasParagraph()
        .withLink("Example", "http://example.com"))
      .isInstanceOf(AssertionError.class);
  }

  @Test
  public void withoutLink_noLinks_passes() {
    assertThat("<p>No links here</p>")
      .hasParagraph()
      .withoutLink();
  }

  @Test
  public void withoutLink_hasLink_throwsAssertionError() {
    assertThatThrownBy(() -> 
      assertThat("<p><a href='#'>link</a></p>")
        .hasParagraph()
        .withoutLink())
      .isInstanceOf(AssertionError.class);
  }

  @Test
  public void withEmphasisOn_matchingEmphasis_passes() {
    assertThat("<p>This is <em>important</em> text</p>")
      .hasParagraph()
      .withEmphasisOn("important");
  }

  @Test
  public void withEmphasisOn_noEmphasis_throwsAssertionError() {
    assertThatThrownBy(() -> 
      assertThat("<p>No emphasis here</p>")
        .hasParagraph()
        .withEmphasisOn("text"))
      .isInstanceOf(AssertionError.class)
      .hasMessageContaining("no <em> in block");
  }

  @Test
  public void withSmallOn_matchingSmall_passes() {
    assertThat("<p>Normal <small>small text</small></p>")
      .hasParagraph()
      .withSmallOn("small text");
  }

  @Test
  public void withSmallOn_noSmall_throwsAssertionError() {
    assertThatThrownBy(() -> 
      assertThat("<p>No small text here</p>")
        .hasParagraph()
        .withSmallOn("text"))
      .isInstanceOf(AssertionError.class)
      .hasMessageContaining("no <small> in block");
  }
}
