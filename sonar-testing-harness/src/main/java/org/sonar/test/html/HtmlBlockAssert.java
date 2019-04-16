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
package org.sonar.test.html;

import java.util.stream.Collectors;
import org.assertj.core.api.AbstractAssert;
import org.assertj.core.api.Assertions;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import static org.assertj.core.util.Preconditions.checkArgument;

public abstract class HtmlBlockAssert<T extends HtmlBlockAssert<T>> extends AbstractAssert<T, Element> {
  static final String PRINT_FRAGMENT_TEMPLATE = "\n---fragment---\n%s\n---fragment---";
  private static final String NO_LINK_IN_BLOC = "no link in bloc";

  public HtmlBlockAssert(Element v, Class<T> selfType) {
    super(v, selfType);
  }

  /**
   * Verifies the current block contains a single link with the specified piece of text.
   */
  public T withLinkOn(String linkText) {
    return withLinkOn(linkText, 1);
  }

  /**
   * Verifies the current block contains {@code times} links with the specified piece of text.
   */
  public T withLinkOn(String linkText, int times) {
    checkArgument(times >= 1, "times must be >= 1");

    isNotNull();

    Elements as = actual.select("a");
    Assertions.assertThat(as)
      .describedAs(NO_LINK_IN_BLOC + PRINT_FRAGMENT_TEMPLATE, actual)
      .isNotEmpty();

    long count = as.stream().filter(t -> linkText.equals(t.text())).count();
    if (count != times) {
      failWithMessage("link on text \"%s\" found %s times in bloc (expected %s). \n Got: %s", linkText, count, times, asyncLinksToString(as));
    }

    return myself;
  }

  /**
   * Verifies the current block contains a link with the specified text and href.
   */
  public T withLink(String linkText, String href) {
    isNotNull();

    Elements as = actual.select("a");
    Assertions.assertThat(as)
      .describedAs(NO_LINK_IN_BLOC + PRINT_FRAGMENT_TEMPLATE, actual)
      .isNotEmpty();

    if (as.stream().noneMatch(t -> linkText.equals(t.text()) && href.equals(t.attr("href")))) {
      failWithMessage(
        "link with text \"%s\" and href \"%s\" not found in block. \n Got: %s" + PRINT_FRAGMENT_TEMPLATE,
        linkText, href, asyncLinksToString(as), actual);
    }

    return myself;
  }

  public T withoutLink() {
    isNotNull();

    Assertions.assertThat(actual.select("a")).isEmpty();

    return myself;
  }

  private static Object asyncLinksToString(Elements linkElements) {
    return new Object() {
      @Override
      public String toString() {
        return linkElements.stream()
          .map(a -> "<a href=\"" + a.attr("href") + "\">" + a.text() + "<a>")
          .collect(Collectors.joining("\n"));
      }
    };
  }

  public T withEmphasisOn(String emphasisText) {
    isNotNull();

    Elements emphases = actual.select("em");
    Assertions.assertThat(emphases)
      .describedAs("no <em> in block")
      .isNotEmpty();
    Assertions.assertThat(emphases.stream().map(Element::text))
      .contains(emphasisText);

    return myself;
  }

  public T withSmallOn(String emphasisText) {
    isNotNull();

    Elements smalls = actual.select("small");
    Assertions.assertThat(smalls)
      .describedAs("no <small> in block")
      .isNotEmpty();
    Assertions.assertThat(smalls.stream().map(Element::text))
      .contains(emphasisText);

    return myself;
  }
}
