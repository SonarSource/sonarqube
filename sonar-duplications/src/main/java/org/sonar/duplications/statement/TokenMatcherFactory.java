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
package org.sonar.duplications.statement;

import org.sonar.duplications.statement.matcher.AnyTokenMatcher;
import org.sonar.duplications.statement.matcher.BridgeTokenMatcher;
import org.sonar.duplications.statement.matcher.ExactTokenMatcher;
import org.sonar.duplications.statement.matcher.ForgetLastTokenMatcher;
import org.sonar.duplications.statement.matcher.OptTokenMatcher;
import org.sonar.duplications.statement.matcher.TokenMatcher;
import org.sonar.duplications.statement.matcher.UptoTokenMatcher;

public final class TokenMatcherFactory {

  private TokenMatcherFactory() {
  }

  public static TokenMatcher from(String token) {
    return new ExactTokenMatcher(token);
  }

  public static TokenMatcher to(String... tokens) {
    return new UptoTokenMatcher(tokens);
  }

  public static TokenMatcher bridge(String lToken, String rToken) {
    return new BridgeTokenMatcher(lToken, rToken);
  }

  public static TokenMatcher anyToken() {
    // TODO Godin: we can return singleton instance
    return new AnyTokenMatcher();
  }

  public static TokenMatcher opt(TokenMatcher optMatcher) {
    return new OptTokenMatcher(optMatcher);
  }

  public static TokenMatcher forgetLastToken() {
    // TODO Godin: we can return singleton instance
    return new ForgetLastTokenMatcher();
  }

  public static TokenMatcher token(String token) {
    return new ExactTokenMatcher(token);
  }

}
