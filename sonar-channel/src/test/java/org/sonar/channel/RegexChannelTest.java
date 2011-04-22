/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2011 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.channel;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import org.junit.Test;

public class RegexChannelTest {

  @Test
  public void shouldMatch() {
    ChannelDispatcher<StringBuilder> dispatcher = new ChannelDispatcher<StringBuilder>(new MyWordChannel(), new BlackholeChannel());
    StringBuilder output = new StringBuilder();
    dispatcher.consume(new CodeReader("my word"), output);
    assertThat(output.toString(), is("<w>my</w> <w>word</w>"));
  }

  private class MyWordChannel extends RegexChannel<StringBuilder> {

    public MyWordChannel() {
      super("\\w++");
    }

    @Override
    protected void consume(CharSequence token, StringBuilder output) {
      output.append("<w>" + token + "</w>");
    }
  }

  class BlackholeChannel extends Channel<StringBuilder> {

    @Override
    public boolean consume(CodeReader code, StringBuilder output) {
      output.append((char) code.pop());
      return true;
    }
  }

}
