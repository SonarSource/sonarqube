/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.colorizer;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.colorizer.SyntaxHighlighterTestingHarness.highlight;

public class RegexpTokenizerTest {

  RegexpTokenizer tokenHighlighter;
  ;

  @Test
  public void testHighlight() {
    tokenHighlighter = new RegexpTokenizer("<r>", "</r>", "[0-9]+");
    assertThat(highlight("123, word = 435;", tokenHighlighter)).isEqualTo("<r>123</r>, word = <r>435</r>;");
  }

  @Test
  public void testClone() {
    RegexpTokenizer tokenizer = new RegexpTokenizer("<r>", "</r>", "[a-z]+");
    RegexpTokenizer cloneTokenizer = tokenizer.clone();
    assertThat(tokenizer).isNotEqualTo(cloneTokenizer);
    assertThat(highlight("public 1234", cloneTokenizer)).isEqualTo("<r>public</r> 1234");
  }

}
