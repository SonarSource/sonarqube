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

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.colorizer.SyntaxHighlighterTestingHarness.highlight;

import org.junit.Test;

public class KeywordsTokenizerTest {

  @Test
  public void testColorizeKeywords() {
    KeywordsTokenizer tokenizer = new KeywordsTokenizer("<s>", "</s>", "public", "new");
    assertThat(highlight("new()", tokenizer)).isEqualTo("<s>new</s>()");
    assertThat(highlight("public new get()", tokenizer)).isEqualTo("<s>public</s> <s>new</s> get()");
    assertThat(highlight("publication", tokenizer)).isEqualTo("publication");
  }

  @Test
  public void testUnderscoreAndDigit() {
    KeywordsTokenizer tokenizer = new KeywordsTokenizer("<s>", "</s>", "_01public");
    assertThat(highlight("_01public", tokenizer)).isEqualTo("<s>_01public</s>");
  }

  @Test
  public void testCaseSensitive() {
    KeywordsTokenizer tokenizer = new KeywordsTokenizer("<s>", "</s>", "public");
    assertThat(highlight("PUBLIC Public public", tokenizer)).isEqualTo("PUBLIC Public <s>public</s>");
  }
  
  @Test
  public void testClone() {
    KeywordsTokenizer tokenizer = new KeywordsTokenizer("<s>", "</s>", "public", "[a-z]+");
    KeywordsTokenizer cloneTokenizer = tokenizer.clone();
    assertThat(tokenizer).isNotEqualTo(cloneTokenizer);
    assertThat(highlight("public 1234", cloneTokenizer)).isEqualTo("<s>public</s> 1234");
  }
}
