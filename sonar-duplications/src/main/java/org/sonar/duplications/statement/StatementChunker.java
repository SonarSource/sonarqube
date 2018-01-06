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

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import org.sonar.duplications.DuplicationsException;
import org.sonar.duplications.statement.matcher.TokenMatcher;
import org.sonar.duplications.token.TokenQueue;

public final class StatementChunker {

  private final StatementChannelDisptacher channelDispatcher;

  private StatementChunker(Builder builder) {
    this.channelDispatcher = builder.getChannelDispatcher();
  }

  public static Builder builder() {
    return new Builder();
  }

  public List<Statement> chunk(@Nullable TokenQueue tokenQueue) {
    if (tokenQueue == null) {
      throw new IllegalArgumentException();
    }
    List<Statement> statements = new ArrayList<>();
    try {
      channelDispatcher.consume(tokenQueue, statements);
      return statements;
    } catch (Exception e) {
      throw new DuplicationsException("Unable to build statement from token : " + tokenQueue.peek(), e);
    }
  }

  /**
   * Note that order is important, e.g.
   * <code>statement(token(A)).ignore(token(A))</code> for the input sequence "A" will produce statement, whereas
   * <code>ignore(token(A)).statement(token(A))</code> will not.
   */
  public static final class Builder {

    private List<StatementChannel> channels = new ArrayList<>();

    private Builder() {
    }

    public StatementChunker build() {
      return new StatementChunker(this);
    }

    /**
     * Defines that sequence of tokens must be ignored, if it matches specified list of matchers.
     * 
     * @see TokenMatcherFactory
     */
    public Builder ignore(TokenMatcher... matchers) {
      channels.add(StatementChannel.createBlackHole(matchers));
      return this;
    }

    /**
     * Defines that sequence of tokens, which is matched specified list of matchers, is a statement.
     * 
     * @see TokenMatcherFactory
     */
    public Builder statement(TokenMatcher... matchers) {
      channels.add(StatementChannel.create(matchers));
      return this;
    }

    private StatementChannelDisptacher getChannelDispatcher() {
      return new StatementChannelDisptacher(channels);
    }
  }

}
