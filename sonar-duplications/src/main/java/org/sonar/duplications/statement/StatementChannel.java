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
package org.sonar.duplications.statement;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import org.sonar.duplications.statement.matcher.TokenMatcher;
import org.sonar.duplications.token.Token;
import org.sonar.duplications.token.TokenQueue;

public final class StatementChannel {

  private final TokenMatcher[] tokenMatchers;
  private final boolean blackHole;

  private StatementChannel(boolean blackHole, @Nullable TokenMatcher... tokenMatchers) {
    if (tokenMatchers == null || tokenMatchers.length == 0) {
      throw new IllegalArgumentException();
    }
    this.blackHole = blackHole;
    this.tokenMatchers = tokenMatchers;
  }

  public static StatementChannel create(TokenMatcher... tokenMatchers) {
    return new StatementChannel(false, tokenMatchers);
  }

  public static StatementChannel createBlackHole(TokenMatcher... tokenMatchers) {
    return new StatementChannel(true, tokenMatchers);
  }

  public boolean consume(TokenQueue tokenQueue, List<Statement> output) {
    List<Token> matchedTokenList = new ArrayList<>();
    for (TokenMatcher tokenMatcher : tokenMatchers) {
      if (!tokenMatcher.matchToken(tokenQueue, matchedTokenList)) {
        tokenQueue.pushForward(matchedTokenList);
        return false;
      }
    }

    // all matchers were successful, so now build the statement
    // matchedTokenList.size() check is for case with ForgiveLastTokenMatcher
    if (!blackHole && !matchedTokenList.isEmpty()) {
      output.add(new Statement(matchedTokenList));
    }
    return true;
  }

}
