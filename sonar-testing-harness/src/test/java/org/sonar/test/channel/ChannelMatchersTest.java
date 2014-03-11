/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.test.channel;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.sonar.test.channel.ChannelMatchers.consume;
import static org.sonar.test.channel.ChannelMatchers.hasNextChar;

import org.junit.Test;
import org.sonar.channel.Channel;
import org.sonar.channel.CodeReader;

public class ChannelMatchersTest {

  @Test
  public void testConsumeMatcher() {
    Channel<StringBuilder> numberChannel = new Channel<StringBuilder>() {

      @Override
      public boolean consume(CodeReader code, StringBuilder output) {
        if (Character.isDigit(code.peek())) {
          output.append((char) code.pop());
          return true;
        }
        return false;
      }
    };
    StringBuilder output = new StringBuilder();
    assertThat(numberChannel, consume("3", output));
    assertThat(output.toString(), is("3"));
    assertThat(numberChannel, consume(new CodeReader("333333"), output));

    output = new StringBuilder();
    assertThat(numberChannel, not(consume("n", output)));
    assertThat(output.toString(), is(""));
    assertThat(numberChannel, not(consume(new CodeReader("n"), output)));
  }

  @Test
  public void testHasNextChar() {
    assertThat(new CodeReader("123"), hasNextChar('1'));
    assertThat(new CodeReader("123"), not(hasNextChar('n')));
  }
}
