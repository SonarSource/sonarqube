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

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;

import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.sonar.channel.CodeReader;

public class TokenChannelTest {

  @Test
  public void shouldConsume() {
    TokenChannel channel = new TokenChannel("ABC");
    TokenQueue output = mock(TokenQueue.class);
    CodeReader codeReader = new CodeReader("ABCD");

    assertThat(channel.consume(codeReader, output), is(true));
    ArgumentCaptor<Token> token = ArgumentCaptor.forClass(Token.class);
    verify(output).add(token.capture());
    assertThat(token.getValue(), is(new Token("ABC", 1, 0)));
    verifyNoMoreInteractions(output);
    assertThat(codeReader.getLinePosition(), is(1));
    assertThat(codeReader.getColumnPosition(), is(3));
  }

  @Test
  public void shouldNormalize() {
    TokenChannel channel = new TokenChannel("ABC", "normalized");
    TokenQueue output = mock(TokenQueue.class);
    CodeReader codeReader = new CodeReader("ABCD");

    assertThat(channel.consume(codeReader, output), is(true));
    ArgumentCaptor<Token> token = ArgumentCaptor.forClass(Token.class);
    verify(output).add(token.capture());
    assertThat(token.getValue(), is(new Token("normalized", 1, 0)));
    verifyNoMoreInteractions(output);
    assertThat(codeReader.getLinePosition(), is(1));
    assertThat(codeReader.getColumnPosition(), is(3));
  }

  @Test
  public void shouldNotConsume() {
    TokenChannel channel = new TokenChannel("ABC");
    TokenQueue output = mock(TokenQueue.class);
    CodeReader codeReader = new CodeReader("123");

    assertThat(channel.consume(new CodeReader("123"), output), is(false));
    verifyZeroInteractions(output);
    assertThat(codeReader.getLinePosition(), is(1));
    assertThat(codeReader.getColumnPosition(), is(0));
  }

  @Test
  public void shouldCorrectlyDeterminePositionWhenTokenSpansMultipleLines() {
    TokenChannel channel = new TokenChannel("AB\nC");
    TokenQueue output = mock(TokenQueue.class);
    CodeReader codeReader = new CodeReader("AB\nCD");

    assertThat(channel.consume(codeReader, output), is(true));
    ArgumentCaptor<Token> token = ArgumentCaptor.forClass(Token.class);
    verify(output).add(token.capture());
    assertThat(token.getValue(), is(new Token("AB\nC", 1, 0)));
    verifyNoMoreInteractions(output);
    assertThat(codeReader.getLinePosition(), is(2));
    assertThat(codeReader.getColumnPosition(), is(1));
  }

}
