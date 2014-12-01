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

import org.sonar.channel.CodeReader;
import org.sonar.channel.EndMatcher;

import java.util.Arrays;

/**
 * @deprecated since 4.5.2 replace by highlighting mechanism
 */
@Deprecated
public abstract class InlineDocTokenizer extends Tokenizer {

  private final String tagBefore;
  private final String tagAfter;

  private final char[] startToken;

  public InlineDocTokenizer(String startToken, String tagBefore, String tagAfter) {
    this.tagBefore = tagBefore;
    this.tagAfter = tagAfter;
    this.startToken = startToken.toCharArray();
  }

  @Override
  public boolean consume(CodeReader code, HtmlCodeBuilder codeBuilder) {
    if (code.peek() == startToken[0] && Arrays.equals(code.peek(startToken.length), startToken)) {
      codeBuilder.appendWithoutTransforming(tagBefore);
      code.popTo(LINE_END_MATCHER, codeBuilder);
      codeBuilder.appendWithoutTransforming(tagAfter);
      return true;
    } else {
      return false;
    }
  }

  private static final EndMatcher LINE_END_MATCHER = new EndMatcher() {
    @Override
    public boolean match(int endFlag) {
      return endFlag == '\r' || endFlag == '\n';
    }
  };

}
