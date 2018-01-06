/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
package org.sonar.duplications.token;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.sonar.channel.Channel;
import org.sonar.channel.CodeBuffer.Cursor;
import org.sonar.channel.CodeReader;

class TokenChannel extends Channel<TokenQueue> {

  private final StringBuilder tmpBuilder = new StringBuilder();
  private final Matcher matcher;
  private String normalizationValue;

  public TokenChannel(String regex) {
    matcher = Pattern.compile(regex).matcher("");
  }

  public TokenChannel(String regex, String normalizationValue) {
    this(regex);
    this.normalizationValue = normalizationValue;
  }

  @Override
  public boolean consume(CodeReader code, TokenQueue output) {
    if (code.popTo(matcher, tmpBuilder) > 0) {
      // see SONAR-2499
      Cursor previousCursor = code.getPreviousCursor();
      if (normalizationValue != null) {
        output.add(new Token(normalizationValue, previousCursor.getLine(), previousCursor.getColumn()));
      } else {
        output.add(new Token(tmpBuilder.toString(), previousCursor.getLine(), previousCursor.getColumn()));
      }
      // Godin: note that other channels use method delete in order to do the same thing
      tmpBuilder.setLength(0);
      return true;
    }
    return false;
  }

}
