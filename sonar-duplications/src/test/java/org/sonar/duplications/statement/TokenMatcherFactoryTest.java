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

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;

import org.junit.Test;
import org.sonar.duplications.statement.matcher.AnyTokenMatcher;
import org.sonar.duplications.statement.matcher.BridgeTokenMatcher;
import org.sonar.duplications.statement.matcher.ExactTokenMatcher;
import org.sonar.duplications.statement.matcher.ForgetLastTokenMatcher;
import org.sonar.duplications.statement.matcher.OptTokenMatcher;
import org.sonar.duplications.statement.matcher.TokenMatcher;
import org.sonar.duplications.statement.matcher.UptoTokenMatcher;

public class TokenMatcherFactoryTest {

  @Test
  public void shouldCreateMatchers() {
    assertThat(TokenMatcherFactory.anyToken(), instanceOf(AnyTokenMatcher.class));
    assertThat(TokenMatcherFactory.bridge("(", ")"), instanceOf(BridgeTokenMatcher.class));
    assertThat(TokenMatcherFactory.forgetLastToken(), instanceOf(ForgetLastTokenMatcher.class));
    assertThat(TokenMatcherFactory.from("if"), instanceOf(ExactTokenMatcher.class));
    assertThat(TokenMatcherFactory.opt(mock(TokenMatcher.class)), instanceOf(OptTokenMatcher.class));
    assertThat(TokenMatcherFactory.to(";"), instanceOf(UptoTokenMatcher.class));
    assertThat(TokenMatcherFactory.token(";"), instanceOf(ExactTokenMatcher.class));
  }

}
