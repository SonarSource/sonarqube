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
package org.sonar.duplications.statement.matcher;

import java.util.List;

import org.sonar.duplications.token.Token;
import org.sonar.duplications.token.TokenQueue;

/**
 * Consumes everything between pair of tokens.
 */
public class BridgeTokenMatcher extends TokenMatcher {

  private final String lToken;
  private final String rToken;

  public BridgeTokenMatcher(String lToken, String rToken) {
    if (lToken == null || rToken == null) {
      throw new IllegalArgumentException();
    }
    this.lToken = lToken;
    this.rToken = rToken;
  }

  @Override
  public boolean matchToken(TokenQueue tokenQueue, List<Token> matchedTokenList) {
    if (!tokenQueue.isNextTokenValue(lToken)) {
      return false;
    }
    int stack = 0;
    while (tokenQueue.peek() != null) {
      Token token = tokenQueue.poll();
      if (lToken.equals(token.getValue())) {
        stack++;
      } else if (rToken.equals(token.getValue())) {
        stack--;
      }
      matchedTokenList.add(token);
      if (stack == 0) {
        return true;
      }
    }
    return false;
  }

}
