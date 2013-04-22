/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
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

package org.sonar.channel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ChannelDispatcher<O> extends Channel<O> {

  private static final Logger LOG = LoggerFactory.getLogger(ChannelDispatcher.class);
  private final boolean failIfNoChannelToConsumeOneCharacter;

  private final Channel<O>[] channels;

  /**
   * @deprecated in version 2.9. Please use the builder() method
   */
  @SuppressWarnings("rawtypes")
  @Deprecated
  public ChannelDispatcher(List<Channel> channels) {
    this(channels, false);
  }

  /**
   * @deprecated in version 2.9. Please use the builder() method
   */
  @SuppressWarnings("rawtypes")
  @Deprecated
  public ChannelDispatcher(Channel... channels) {
    this(Arrays.asList(channels), false);
  }

  /**
   * @deprecated in version 2.9. Please use the builder() method
   */
  @SuppressWarnings("rawtypes")
  @Deprecated
  public ChannelDispatcher(List<Channel> channels, boolean failIfNoChannelToConsumeOneCharacter) {
    this.channels = channels.toArray(new Channel[channels.size()]);
    this.failIfNoChannelToConsumeOneCharacter = failIfNoChannelToConsumeOneCharacter;
  }

  private ChannelDispatcher(Builder builder) {
    this.channels = builder.channels.toArray(new Channel[builder.channels.size()]);
    this.failIfNoChannelToConsumeOneCharacter = builder.failIfNoChannelToConsumeOneCharacter;
  }

  @Override
  public boolean consume(CodeReader code, O output) {
    int nextChar = code.peek();
    while (nextChar != -1) {
      boolean characterConsumed = false;
      for (Channel<O> channel : channels) {
        if (channel.consume(code, output)) {
          characterConsumed = true;
          break;
        }
      }
      if ( !characterConsumed) {
        if (LOG.isDebugEnabled() || failIfNoChannelToConsumeOneCharacter) {
          String message = "None of the channel has been able to handle character '" + (char) code.peek() + "' (decimal value "
              + code.peek() + ") at line " + code.getLinePosition() + ", column " + code.getColumnPosition();
          if (failIfNoChannelToConsumeOneCharacter) {
            throw new IllegalStateException(message);
          }
          LOG.debug(message);
        }
        code.pop();
      }
      nextChar = code.peek();
    }
    return true;
  }

  Channel[] getChannels() {
    return channels;
  }

  /**
   * Get a Builder instance to build a new ChannelDispatcher
   */
  public static Builder builder() {
    return new Builder();
  }

  public static final class Builder {

    private List<Channel> channels = new ArrayList<Channel>();
    private boolean failIfNoChannelToConsumeOneCharacter = false;

    private Builder() {
    }

    public Builder addChannel(Channel channel) {
      channels.add(channel);
      return this;
    }

    public Builder addChannels(Channel... c) {
      for (Channel channel : c) {
        addChannel(channel);
      }
      return this;
    }

    /**
     * If this option is activated, an IllegalStateException will be thrown as soon as a character won't be consumed by any channel.
     */
    public Builder failIfNoChannelToConsumeOneCharacter() {
      failIfNoChannelToConsumeOneCharacter = true;
      return this;
    }

    public <O> ChannelDispatcher<O> build() {
      return new ChannelDispatcher<O>(this);
    }

  }
}