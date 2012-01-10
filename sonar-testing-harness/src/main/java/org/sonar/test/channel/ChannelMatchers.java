/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
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
package org.sonar.test.channel;

import org.sonar.channel.CodeReader;

public final class ChannelMatchers {

  private ChannelMatchers() {
  }

  public static <OUTPUT> ChannelMatcher<OUTPUT> consume(String sourceCode, OUTPUT output) {
    return new ChannelMatcher<OUTPUT>(sourceCode, output);
  }

  public static <OUTPUT> ChannelMatcher<OUTPUT> consume(CodeReader codeReader, OUTPUT output) {
    return new ChannelMatcher<OUTPUT>(codeReader, output);
  }

  public static ReaderHasNextCharMatcher hasNextChar(char nextChar) {
    return new ReaderHasNextCharMatcher(nextChar);
  }
}
