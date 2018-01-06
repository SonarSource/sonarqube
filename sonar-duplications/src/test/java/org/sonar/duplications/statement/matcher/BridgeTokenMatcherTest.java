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
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;
import org.sonar.duplications.token.Token;
import org.sonar.duplications.token.TokenQueue;

public class BridgeTokenMatcherTest {

  @Test(expected = IllegalArgumentException.class)
  public void shouldNotAcceptNullAsLeft() {
    new BridgeTokenMatcher(null, ")");
  }

  @Test(expected = IllegalArgumentException.class)
  public void shouldNotAcceptNullAsRight() {
    new BridgeTokenMatcher("(", null);
  }

  @Test
  public void shouldMatch() {
    Token t1 = new Token("(", 1, 1);
    Token t2 = new Token("a", 2, 1);
    Token t3 = new Token("(", 3, 1);
    Token t4 = new Token("b", 4, 1);
    Token t5 = new Token(")", 5, 1);
    Token t6 = new Token("c", 6, 1);
    Token t7 = new Token(")", 7, 1);
    TokenQueue tokenQueue = spy(new TokenQueue(Arrays.asList(t1, t2, t3, t4, t5, t6, t7)));
    List<Token> output = mock(List.class);
    BridgeTokenMatcher matcher = new BridgeTokenMatcher("(", ")");

    assertThat(matcher.matchToken(tokenQueue, output), is(true));
    verify(tokenQueue, times(1)).isNextTokenValue("(");
    verify(tokenQueue, times(7)).poll();
    verify(tokenQueue, times(7)).peek();
    verifyNoMoreInteractions(tokenQueue);
    verify(output).add(t1);
    verify(output).add(t2);
    verify(output).add(t3);
    verify(output).add(t4);
    verify(output).add(t5);
    verify(output).add(t6);
    verify(output).add(t7);
    verifyNoMoreInteractions(output);
  }

  @Test
  public void shouldNotMatchWhenNoLeft() {
    Token t1 = new Token("a", 1, 1);
    TokenQueue tokenQueue = spy(new TokenQueue(Arrays.asList(t1)));
    List<Token> output = mock(List.class);
    BridgeTokenMatcher matcher = new BridgeTokenMatcher("(", ")");

    assertThat(matcher.matchToken(tokenQueue, output), is(false));
    verify(tokenQueue).isNextTokenValue("(");
    verifyNoMoreInteractions(tokenQueue);
    verifyNoMoreInteractions(output);
  }

  @Test
  public void shouldNotMatchWhenNoRight() {
    Token t1 = new Token("(", 1, 1);
    TokenQueue tokenQueue = spy(new TokenQueue(Arrays.asList(t1)));
    List<Token> output = mock(List.class);
    BridgeTokenMatcher matcher = new BridgeTokenMatcher("(", ")");

    assertThat(matcher.matchToken(tokenQueue, output), is(false));
    verify(tokenQueue, times(1)).isNextTokenValue("(");
    verify(tokenQueue, times(1)).poll();
    verify(tokenQueue, times(2)).peek();
    verifyNoMoreInteractions(tokenQueue);
    verify(output).add(t1);
    verifyNoMoreInteractions(output);
  }

}
