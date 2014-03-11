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

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;
import static org.sonar.colorizer.SyntaxHighlighterTestingHarness.highlight;

import org.junit.Test;

public class KeywordsTokenizerTest {

  @Test
  public void testColorizeKeywords() {
    KeywordsTokenizer tokenizer = new KeywordsTokenizer("<s>", "</s>", "public", "new");
    assertThat(highlight("new()", tokenizer), is("<s>new</s>()"));
    assertThat(highlight("public new get()", tokenizer), is("<s>public</s> <s>new</s> get()"));
    assertThat(highlight("publication", tokenizer), is("publication"));
  }

  @Test
  public void testUnderscoreAndDigit() {
    KeywordsTokenizer tokenizer = new KeywordsTokenizer("<s>", "</s>", "_01public");
    assertThat(highlight("_01public", tokenizer), is("<s>_01public</s>"));
  }

  @Test
  public void testCaseSensitive() {
    KeywordsTokenizer tokenizer = new KeywordsTokenizer("<s>", "</s>", "public");
    assertThat(highlight("PUBLIC Public public", tokenizer), is("PUBLIC Public <s>public</s>"));
  }
  
  @Test
  public void testClone() {
    KeywordsTokenizer tokenizer = new KeywordsTokenizer("<s>", "</s>", "public", "[a-z]+");
    KeywordsTokenizer cloneTokenizer = tokenizer.clone();
    assertThat(tokenizer, is(not(cloneTokenizer)));
    assertThat(highlight("public 1234", cloneTokenizer), is("<s>public</s> 1234"));
  }
}
