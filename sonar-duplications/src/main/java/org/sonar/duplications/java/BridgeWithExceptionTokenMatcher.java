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
package org.sonar.duplications.java;

import java.util.List;

import org.sonar.duplications.statement.matcher.TokenMatcher;
import org.sonar.duplications.token.Token;
import org.sonar.duplications.token.TokenQueue;

public class BridgeWithExceptionTokenMatcher extends TokenMatcher {

  private final String lToken;
  private final String rToken;
  private final String except;

  public BridgeWithExceptionTokenMatcher(String lToken, String rToken, String except) {
    if (lToken == null || rToken == null || except == null) {
      throw new IllegalArgumentException();
    }
    this.lToken = lToken;
    this.rToken = rToken;
    this.except = except;
  }

  @Override
  public boolean matchToken(TokenQueue tokenQueue, List<Token> matchedTokenList) {
    if (!tokenQueue.isNextTokenValue(lToken)) {
      return false;
    }
    int stack = 0;
    while (tokenQueue.peek() != null) {
      Token token = tokenQueue.poll();
      matchedTokenList.add(token);
      if (lToken.equals(token.getValue())) {
        stack++;
      } else if (rToken.equals(token.getValue())) {
        stack--;
      } else if (except.equals(token.getValue())) {
        return false;
      }
      if (stack == 0) {
        return true;
      }
    }
    return false;
  }

}
