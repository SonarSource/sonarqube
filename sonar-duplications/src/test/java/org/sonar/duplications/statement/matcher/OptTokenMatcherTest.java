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

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import java.util.List;

import org.junit.Test;
import org.sonar.duplications.token.Token;
import org.sonar.duplications.token.TokenQueue;

public class OptTokenMatcherTest {

  @Test(expected = IllegalArgumentException.class)
  public void shouldNotAcceptNull() {
    new OptTokenMatcher(null);
  }

  @Test
  public void shouldMatch() {
    TokenQueue tokenQueue = spy(new TokenQueue());
    TokenMatcher delegate = mock(TokenMatcher.class);
    OptTokenMatcher matcher = new OptTokenMatcher(delegate);
    List<Token> output = mock(List.class);

    assertThat(matcher.matchToken(tokenQueue, output), is(true));
    verify(delegate).matchToken(tokenQueue, output);
    verifyNoMoreInteractions(delegate);
    verifyNoMoreInteractions(tokenQueue);
    verifyNoMoreInteractions(output);
  }

}
