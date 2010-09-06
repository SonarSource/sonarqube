/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2009 SonarSource SA
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.colorizer;

import java.util.Arrays;

import org.sonar.channel.CodeReader;
import org.sonar.channel.EndMatcher;

public abstract class InlineDocTokenizer extends Tokenizer {

  private final String tagBefore;
  private final String tagAfter;

  private final char[] startToken;

  public InlineDocTokenizer(String startToken, String tagBefore, String tagAfter) {
    this.tagBefore = tagBefore;
    this.tagAfter = tagAfter;
    this.startToken = startToken.toCharArray();
  }

  public boolean consume(CodeReader code, HtmlCodeBuilder codeBuilder) {
    if (code.peek() == startToken[0] && Arrays.equals(code.peek(startToken.length), startToken)) {
      codeBuilder.appendWithoutTransforming(tagBefore);
      code.popTo(new EndMatcher() {

        public boolean match(int endFlag) {
          return endFlag == '\r' || endFlag == '\n';
        }
      }, codeBuilder);
      codeBuilder.appendWithoutTransforming(tagAfter);
      return true;
    } else {
      return false;
    }
  }

}
