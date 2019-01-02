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

import java.util.List;
import org.junit.Test;
import org.sonar.duplications.statement.matcher.TokenMatcher;
import org.sonar.duplications.token.Token;
import org.sonar.duplications.token.TokenQueue;

import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class StatementChannelDisptacherTest {

  @Test(expected = IllegalStateException.class)
  public void shouldThrowAnException() {
    TokenMatcher tokenMatcher = mock(TokenMatcher.class);
    StatementChannel channel = StatementChannel.create(tokenMatcher);
    StatementChannelDisptacher dispatcher = new StatementChannelDisptacher(asList(channel));
    TokenQueue tokenQueue = mock(TokenQueue.class);
    when(tokenQueue.peek()).thenReturn(new Token("a", 1, 0)).thenReturn(null);
    List<Statement> statements = mock(List.class);

    dispatcher.consume(tokenQueue, statements);
  }

  @Test
  public void shouldConsume() {
    TokenMatcher tokenMatcher = mock(TokenMatcher.class);
    when(tokenMatcher.matchToken(any(TokenQueue.class), anyList())).thenReturn(true);
    StatementChannel channel = StatementChannel.create(tokenMatcher);
    StatementChannelDisptacher dispatcher = new StatementChannelDisptacher(asList(channel));
    TokenQueue tokenQueue = mock(TokenQueue.class);
    when(tokenQueue.peek()).thenReturn(new Token("a", 1, 0)).thenReturn(null);
    List<Statement> statements = mock(List.class);

    assertThat(dispatcher.consume(tokenQueue, statements), is(true));
    verify(tokenQueue, times(2)).peek();
    verifyNoMoreInteractions(tokenQueue);
    verifyNoMoreInteractions(statements);
  }

}
