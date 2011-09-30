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
package org.sonar.duplications.token;

import java.io.Reader;
import java.io.StringReader;
import org.sonar.channel.ChannelDispatcher;
import org.sonar.channel.CodeReader;
import org.sonar.channel.CodeReaderConfiguration;
import org.sonar.duplications.DuplicationsException;

public final class TokenChunker {

  /**
   * Some files can have large tokens (e.g. javadocs), so large buffer required.
   * 
   * @see CodeReaderConfiguration#DEFAULT_BUFFER_CAPACITY
   */
  private final static int BUFFER_CAPACITY = 80000;

  private final ChannelDispatcher<TokenQueue> channelDispatcher;

  public static Builder builder() {
    return new Builder();
  }

  private TokenChunker(Builder builder) {
    this.channelDispatcher = builder.getChannelDispatcher();
  }

  public TokenQueue chunk(String sourceCode) {
    return chunk(new StringReader(sourceCode));
  }

  public TokenQueue chunk(Reader reader) {
    CodeReaderConfiguration codeReaderConfiguration = new CodeReaderConfiguration();
    codeReaderConfiguration.setBufferCapacity(BUFFER_CAPACITY);
    CodeReader code = new CodeReader(reader, codeReaderConfiguration);
    TokenQueue queue = new TokenQueue();
    try {
      channelDispatcher.consume(code, queue);
      return queue;
    } catch (Exception e) {
      throw new DuplicationsException("Unable to lex source code at line : " + code.getLinePosition() + " and column : " + code.getColumnPosition(), e);
    }
  }

  /**
   * Note that order is important, e.g.
   * <code>token("A").ignore("A")</code> for the input string "A" will produce token, whereas
   * <code>ignore("A").token("A")</code> will not.
   */
  public static final class Builder {

    private ChannelDispatcher.Builder channelDispatcherBuilder = ChannelDispatcher.builder();

    private Builder() {
    }

    public TokenChunker build() {
      return new TokenChunker(this);
    }

    /**
     * Defines that sequence of characters must be ignored, if it matches specified regular expression.
     */
    public Builder ignore(String regularExpression) {
      channelDispatcherBuilder.addChannel(new BlackHoleTokenChannel(regularExpression));
      return this;
    }

    /**
     * Defines that sequence of characters, which is matched specified regular expression, is a token.
     */
    public Builder token(String regularExpression) {
      channelDispatcherBuilder.addChannel(new TokenChannel(regularExpression));
      return this;
    }

    /**
     * Defines that sequence of characters, which is matched specified regular expression, is a token with specified value.
     */
    public Builder token(String regularExpression, String normalizationValue) {
      channelDispatcherBuilder.addChannel(new TokenChannel(regularExpression, normalizationValue));
      return this;
    }

    private ChannelDispatcher<TokenQueue> getChannelDispatcher() {
      return channelDispatcherBuilder.build();
    }

  }

}
