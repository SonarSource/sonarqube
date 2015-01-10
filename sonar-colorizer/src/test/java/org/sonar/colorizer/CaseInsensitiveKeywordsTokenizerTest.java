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

public class CaseInsensitiveKeywordsTokenizerTest {

  @Test
  public void hasNextToken() {
    CaseInsensitiveKeywordsTokenizer tokenizer = new CaseInsensitiveKeywordsTokenizer("<k>", "</k>", "PROCEDURE");
    assertThat(highlight("procedure name", tokenizer)).isEqualTo("<k>procedure</k> name");
    assertThat(highlight("Procedure name", tokenizer)).isEqualTo("<k>Procedure</k> name");
    assertThat(highlight("PROCEDURE name", tokenizer)).isEqualTo("<k>PROCEDURE</k> name");
  }

  @Test
  public void testClone() {
    CaseInsensitiveKeywordsTokenizer tokenizer = new CaseInsensitiveKeywordsTokenizer("<k>", "</k>", "PROCEDURE");
    Tokenizer cloneTokenizer = tokenizer.clone();
    assertThat(tokenizer).isNotEqualTo(cloneTokenizer);
    assertThat(highlight("procedure name", cloneTokenizer)).isEqualTo("<k>procedure</k> name");
  }
}
