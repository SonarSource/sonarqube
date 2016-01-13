/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package org.sonar.colorizer;

import org.sonar.channel.Channel;
import org.sonar.channel.CodeReader;

import java.util.List;

public class TokenizerDispatcher {

  private Channel<HtmlCodeBuilder>[] tokenizers;

  public TokenizerDispatcher(Channel<HtmlCodeBuilder>... tokenizers) {
    this.tokenizers = tokenizers;
  }

  public TokenizerDispatcher(List<Channel<HtmlCodeBuilder>> tokenizersArray) {
    this.tokenizers = tokenizersArray.toArray(new Channel[tokenizersArray.size()]);
  }

  public final String colorize(String code) {
    HtmlCodeBuilder colorizedCode = new HtmlCodeBuilder();
    colorize(new CodeReader(code), colorizedCode);
    return colorizedCode.toString();
  }

  public final void colorize(CodeReader code, HtmlCodeBuilder colorizedCode) {
    cloneNotThreadSafeTokenizers();
    nextChar:
    while (code.peek() != -1) {
      for (Channel<HtmlCodeBuilder> codeTokenizer : tokenizers) {
        if (codeTokenizer.consume(code, colorizedCode)) {
          continue nextChar;
        }
      }
      colorizedCode.append((char) code.pop());
    }
  }

  private void cloneNotThreadSafeTokenizers() {
    for (int i = 0; i < tokenizers.length; i++) {
      if (tokenizers[i] instanceof NotThreadSafeTokenizer) {
        tokenizers[i] = ((NotThreadSafeTokenizer) tokenizers[i]).clone();
      }
    }
  }
}
