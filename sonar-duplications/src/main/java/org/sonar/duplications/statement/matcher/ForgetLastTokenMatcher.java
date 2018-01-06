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
package org.sonar.duplications.statement.matcher;

import java.util.Collections;
import java.util.List;

import org.sonar.duplications.token.Token;
import org.sonar.duplications.token.TokenQueue;

/**
 * Last token would be returned to the queue.
 */
public class ForgetLastTokenMatcher extends TokenMatcher {

  /**
   * @return always true
   */
  @Override
  public boolean matchToken(TokenQueue tokenQueue, List<Token> matchedTokenList) {
    int last = matchedTokenList.size() - 1;
    tokenQueue.pushForward(Collections.singletonList(matchedTokenList.get(last)));
    matchedTokenList.remove(last);
    return true;
  }

}
