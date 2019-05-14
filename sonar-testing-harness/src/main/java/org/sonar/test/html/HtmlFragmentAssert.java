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

import java.util.Iterator;
import org.assertj.core.api.AbstractAssert;
import org.assertj.core.api.Assertions;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import static java.util.stream.Collectors.toList;
import static org.sonar.test.html.HtmlParagraphAssert.verifyIsParagraph;

public class HtmlFragmentAssert extends AbstractAssert<HtmlFragmentAssert, String> {

  public HtmlFragmentAssert(String s) {
    super(s, HtmlFragmentAssert.class);
  }

  public static HtmlFragmentAssert assertThat(String s) {
    return new HtmlFragmentAssert(s);
  }

  public HtmlParagraphAssert hasParagraph() {
    isNotNull();

    Document document = Jsoup.parseBodyFragment(actual);
    Iterator<Element> blockIt = document.body().children().stream()
      .filter(Element::isBlock)
      .collect(toList())
      .iterator();
    Assertions.assertThat(blockIt.hasNext())
      .describedAs("no bloc in fragment")
      .isTrue();

    Element firstBlock = blockIt.next();
    verifyIsParagraph(firstBlock);

    return new HtmlParagraphAssert(firstBlock, blockIt);
  }

  /**
   * Convenience method.
   * Sames as {@code hasParagraph().withText(text)}.
   */
  public HtmlParagraphAssert hasParagraph(String text) {
    return hasParagraph()
      .withText(text);
  }

}
